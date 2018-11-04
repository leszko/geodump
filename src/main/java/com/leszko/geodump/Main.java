package com.leszko.geodump;

import java.io.IOException;

public class Main {
    public static void main(String[] args)
            throws IOException {
        if (args.length != 1) {
            System.out.println("No argument found");
            System.out.println("./geodump <geoblog-username>");
            System.exit(1);
        }

        String username = args[0];
        String outputFile = "./GEOBLOG.md";
        new GeoDumper(username, outputFile).dump();
    }
}
