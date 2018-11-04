package com.leszko.geodump;

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
        //        System.out.println(geoCrawler.readPost("http://marianka.geoblog.pl/wpis/110784/tuz-pod-kolem-polarnym"));
    }

}
