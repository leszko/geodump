package com.leszko.geodump;

import com.leszko.geodump.GeoDump.Post;
import com.leszko.geodump.GeoDump.TripStats;

import java.io.IOException;

class GeoDumper {
    private static final String TEMPLATE_URL = "http://%s.geoblog.pl/podroze";
    private final GeoCrawler geoCrawler;
    private final String username;
    private final String outputFile;

    GeoDumper(String username, String outputFile) {
        this.geoCrawler = new GeoCrawler(username);
        this.username = username;
        this.outputFile = outputFile;
    }

    void dump()
            throws IOException {
//        System.out.println(geoCrawler.readOverview());
//        System.out.println(geoCrawler.readTrip("http://marianka.geoblog.pl/podroz/13157/na-kolach-za-kolo-rowerem-na-nordkapp"));
        System.out.println(geoCrawler.readPost("http://marianka.geoblog.pl/wpis/110784/tuz-pod-kolem-polarnym"));

//        String url = String.format(TEMPLATE_URL, username);
//        List<String> trips = geoCrawler.fetchTrips(url);
//        String tripUrl = "http://marianka.geoblog.pl/podroz/13157/na-kolach-za-kolo-rowerem-na-nordkapp";
        //        List<String> posts = fetchPosts(tripUrl);
//        String postUrl = "http://marianka.geoblog.pl/wpis/109743/nocleg-wreszcie-na-plazy";
        //        posts.forEach(System.out::println);
//        Stats stats = geoCrawler.parseStats(tripUrl);
//        System.out.println(stats);

//        Post post = geoCrawler.parsePost(postUrl);
//        System.out.println(post);
        //        fetchMedia(postUrl).stream()
        //                           .map(link -> extractMediaLink(link))
        //                           .forEach(System.out::println);

    }



}
