package com.aoe4Forum.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;

@Service
public class ElasticsearchInitService {

    private final ElasticsearchClient client;

    // 构造函数注入
    public ElasticsearchInitService(ElasticsearchClient client) {
        this.client = client;
    }

    public void init() throws IOException {
        // 连接ES服务器

        // 创建索引请求
        CreateIndexRequest request = CreateIndexRequest.of(b -> b
                .index("post")
                .settings(s -> s
                        .analysis(a -> a
                                .analyzer("ik_max_word", analyzer -> analyzer
                                        .custom(c -> c.tokenizer("ik_max_word"))
                                )
                        )
                )
                .mappings(m -> m
                        .properties("content", p -> p
                                .text(t -> t
                                        .analyzer("ik_max_word")
                                        .searchAnalyzer("ik_max_word")
                                )
                        )
                        .properties("title", p -> p
                                .text(t -> t
                                        .analyzer("ik_max_word")
                                        .searchAnalyzer("ik_max_word")
                                )
                        )
                )
        );

        // 发送创建请求
        CreateIndexResponse response = client.indices().create(request);

        if (response.acknowledged()) {
            System.out.println("索引创建成功");
        } else {
            System.out.println("索引创建失败");
        }
    }
}
