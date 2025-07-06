package org.omenki.dev.tech.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * @program: ai-rag-knowledge
 * @description: 统一response类
 * @author: Patrick_Zhu(朱兆麒)
 * @create: 2025-07-06 17:00
 **/
public class Response<T> implements Serializable {

    private String code;
    private String info;
    private T data;

}

