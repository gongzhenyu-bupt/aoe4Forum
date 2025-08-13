package com.aoe4Forum.entity;

import lombok.Data;

@Data
public class PostDocument {
    private Long id;
    private String title;
    private String content;

    public PostDocument(){}
    
    public PostDocument(Long id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
    }

}
