package org.omenki.dev.tech.app.test;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @program: ai-rag-knowledge
 * @description: rag的test类
 * @author: Patrick_Zhu(朱兆麒)
 * @create: 2025-07-06 15:42
 **/
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
//这是一个RAG流程的完整实现：用户提问 → 检索知识库相关文档 → 拼接进提示词 → 大模型生成中文答案。
//这样做的好处是：即使大模型本身不知道答案，也能借助知识库里的内容给出准确回复，并且答案更可控、更可追溯。
public class RAGTest {

    @Resource
    private OllamaChatClient ollamaChatClient;
    @Resource
    private TokenTextSplitter tokenTextSplitter;
    @Resource
    private SimpleVectorStore simpleVectorStore;
    @Resource
    private PgVectorStore pgVectorStore;

    @Test
    public void upload() {
        TikaDocumentReader reader = new TikaDocumentReader("./data/file.txt");

        //文件切割
        List<Document> documents = reader.get();
        List<Document> documentsSplitterList = tokenTextSplitter.apply(documents);

        //文件分块打标签
        documents.forEach(document -> document.getMetadata().put("knowledge", "知识库源"));
        documentsSplitterList.forEach(document -> document.getMetadata().put("knowledge", "切割知识库源"));

        //加入到向量知识库中
        pgVectorStore.accept(documentsSplitterList);

        log.info("上传成功");
    }



    @Test
    public void chat() {
        String message = "帮我搜集一下朱兆麒的资料";

        String SYSTEM_PROMPT = """
                Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
                If unsure, simply state that you don't know.
                Another thing you need to note is that your reply must be in Chinese!
                DOCUMENTS:
                    {documents}
                """;

        //构造向量检索请求，基于用户问题找出最相关的5条文档（过滤知识库为“知识库名称”）。
        SearchRequest request = SearchRequest.query(message).withTopK(5).withFilterExpression("knowledge == '切割知识库源'");

        //检索文档，组装获取到的topk文档内容
        List<Document> documents = pgVectorStore.similaritySearch(request);
        String documentsCollectors = documents.stream().map(Document::getContent).collect(Collectors.joining());

        //组装提示词，将检索到的文档内容拼接到提示词中
        Message ragMessage = new SystemPromptTemplate(SYSTEM_PROMPT).createMessage(Map.of("documents", documentsCollectors));

        ArrayList<Message> messages = new ArrayList<>();
        messages.add(new UserMessage(message));
        messages.add(ragMessage);

        //调用一下本地的模型进行测试
        ChatResponse chatResponse = ollamaChatClient.call(new Prompt(messages, OllamaOptions.create().withModel("deepseek-r1:1.5b")));

        log.info("chatResponse:{}", chatResponse.getResult().getOutput().getContent());

    }
}
