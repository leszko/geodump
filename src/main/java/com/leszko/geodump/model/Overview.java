package com.leszko.geodump.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class Overview implements Serializable {
    private final List<Trip> trips;

    @Data
    @Builder
    public static class Trip implements Serializable {
        private final String name;
        private final String url;
    }
}
