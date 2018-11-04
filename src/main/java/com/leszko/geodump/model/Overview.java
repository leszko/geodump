package com.leszko.geodump.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Overview {
    private final List<Trip> trips;

    @Data
    @Builder
    public static class Trip {
        private final String name;
        private final String url;
    }
}
