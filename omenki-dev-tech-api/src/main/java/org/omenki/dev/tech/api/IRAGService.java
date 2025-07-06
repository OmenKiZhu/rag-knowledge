package org.omenki.dev.tech.api;

import org.omenki.dev.tech.api.response.Response;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IRAGService {
    /**
     * 查询RAG标签列表
     *
     * @return Response<List<String>> 包含RAG标签的响应对象
     */
    Response<List<String>> queryRagTagList();

    /**
     * 上传文件到指定的RAG标签
     *
     * @param ragTag RAG标签
     * @param files  要上传的文件列表
     * @return Response<String> 包含上传结果的响应对象
     */
    Response<String> uploadFile(String ragTag, List<MultipartFile> files);
}
