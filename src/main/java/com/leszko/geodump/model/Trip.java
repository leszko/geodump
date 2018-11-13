package com.leszko.geodump.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class Trip implements Serializable {
    private final String name;
    private final Stats stats;
    private final List<Post> posts;
    private final String url;

    @Data
    @Builder
    public static class Post implements Serializable {
        private final String name;
        private final String place;
        private final String date;
        private final String url;
    }

    @Data
    @Builder
    public static class Stats implements Serializable{
        private final String daysInTrip;
        private final String countries;
        private final String kilometers;
        private final String geoblogNote;
        private final String geoblogVisitsCount;
    }
}
