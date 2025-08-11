package com.aoe4Forum.service;

import com.aoe4Forum.entity.PostDocument;

import java.util.List;

public interface ElasticSearchService {
    void save(PostDocument postDocument);
    List<Long> search(String keyword);
}
