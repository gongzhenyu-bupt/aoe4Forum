package com.aoe4Forum.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.aoe4Forum.entity.PostDocument;
import com.aoe4Forum.service.ElasticSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ElasticSearchServiceImpl implements ElasticSearchService {

    @Resource
    private ElasticsearchClient client;

    @Override
    public void save(PostDocument post) {
        try{
            IndexResponse response = client.index(i -> i
                    .index("post") // 索引名
                    .id(String.valueOf(post.getId()))
                    .document(post)
            );
        }
        catch (Exception e){
            log.error("es保存失败，postid:"+post.getId(),e);
        }
    }

    @Override
    public List<Long> search(String keyword) {
        try {
            SearchResponse<PostDocument> response = client.search(s -> s
                            .index("post")
                            .query(q -> q
                                    .multiMatch(m -> m
                                            .fields("title^2", "content")
                                            .query(keyword)
                                    )
                            ),
                    PostDocument.class
            );

            return response.hits().hits().stream()
                    .map(Hit::source)        // 取 PostDocument
                    .filter(Objects::nonNull) // 过滤 null
                    .map(PostDocument::getId) // 提取 id
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("es搜索失败，keyword: " + keyword, e);
            return Collections.emptyList();
        }
    }
}
