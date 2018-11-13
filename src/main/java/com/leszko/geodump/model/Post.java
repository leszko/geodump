package com.leszko.geodump.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class Post implements Serializable {
    private final String name;
    private final String place;
    private final String date;
    private final String distance;
    private final String text;
    private final String url;
    private final List<String> mediaLinks;
    private final List<Comment> comments;

    @Data
    @Builder
    public static class Comment implements Serializable{
        private final String username;
        private final String date;
        private final String text;
    }
}
