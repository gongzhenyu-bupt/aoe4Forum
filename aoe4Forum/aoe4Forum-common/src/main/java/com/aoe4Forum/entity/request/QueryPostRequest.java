package com.aoe4Forum.entity.request;

import lombok.Data;

@Data
public class QueryPostRequest {

    private String forum;

    private int offset = 0;

    private int limit = 1;

}