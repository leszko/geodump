package com.leszko.geodump;

import com.leszko.geodump.model.Overview;
import com.leszko.geodump.model.Post;
import com.leszko.geodump.model.Trip;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

class GeoDumper {
    private final GeoCrawler geoCrawler;
    private final String username;
    private final String outputPath;

    GeoDumper(String username, String outputPath) {
        this.geoCrawler = new GeoCrawler(username);
        this.username = username;
        this.outputPath = outputPath;
    }

    void dump()
            throws IOException {
        System.out.println(String.format("Starting to process Geoblog: '%s'", username));
        System.out.println("===============================================");

        Path mainPath = Paths.get(outputPath);
        createDirectoryIfNotExists(mainPath);
        Path usernamePath = mainPath.resolve(username);
        createDirectoryIfNotExists(usernamePath);

        int tripsToSkip = toSkip(usernamePath);
        if (tripsToSkip > 0) {
            System.out.println(String.format("Skipping %s trips", tripsToSkip));
        }

        System.out.println("Reading all trips...");
        List<Overview.Trip> trips = geoCrawler.readOverview().getTrips();
        for (int i = tripsToSkip; i < trips.size(); i++) {
            Overview.Trip trip = trips.get(i);

            System.out.println("\n\n**********************************\n");
            System.out.println(String.format("Processing trip %02d: %s", i + 1, trip.getName()));
            processTrip(createDirectory(i, trip.getName(), usernamePath), trip.getUrl());
        }
    }

    private void processTrip(Path path, String tripUrl)
            throws IOException {
        Trip trip = geoCrawler.readTrip(tripUrl);

        writeToFile(path.resolve("geoblog-url.txt"), trip.getUrl());
        writeToFile(path.resolve("name.txt"), trip.getName());
        writeTripStatsToFile(path.resolve("stats.txt"), trip.getStats());

        List<Trip.Post> posts = trip.getPosts();
        for (int i = toSkip(path); i < posts.size(); i++) {
            Trip.Post post = posts.get(i);

            System.out.println(String.format("Processing post %02d: %s", i + 1, post.getName()));
            processPost(createDirectory(i, postName(post), path), post.getUrl());
        }
    }

    private String postName(Trip.Post post) {
        return String.format("%s (%s, %s)", post.getName(), post.getPlace(), post.getDate());
    }

    private void processPost(Path path, String url)
            throws IOException {
        Post post = geoCrawler.readPost(url);

        writeToFile(path.resolve("geoblog-url.txt"), post.getUrl());
        writeToFile(path.resolve("name.txt"), post.getName());
        writeToFile(path.resolve("stats.txt"), String.format("Dystans: %s", post.getDistance()));
        writeToFile(path.resolve("date.txt"), post.getDate());
        writeToFile(path.resolve("place.txt"), post.getPlace());
        writeToFile(path.resolve("text.txt"), post.getText());
        writeCommentsToFile(path.resolve("comments.txt"), post.getComments());
        downloadMedia(path.resolve("media"), post.getMediaLinks());
    }

    private void downloadMedia(Path path, List<String> mediaLinks)
            throws IOException {
        createDirectoryIfNotExists(path);

        for (String mediaLink : mediaLinks) {
            InputStream in = new URL(mediaLink).openStream();
            Files.copy(in, path.resolve(lastPartOf(mediaLink)), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String lastPartOf(String url) {
        String[] parts = url.split("/");
        return parts[parts.length - 1];
    }

    private void writeCommentsToFile(Path file, List<Post.Comment> comments) {
        StringBuilder text = new StringBuilder();
        for (Post.Comment comment : comments) {
            text.append(String.format("%s\n", comment.getUsername()));
            text.append(String.format("%s\n", comment.getDate()));
            text.append(String.format("%s\n\n================================\n", comment.getText()));
        }

        writeToFile(file, text.toString());
    }

    private void writeTripStatsToFile(Path file, Trip.Stats stats) {
        StringBuilder text = new StringBuilder();
        text.append(String.format("Liczba przebytych krajów: %s\n", stats.getCountries()));
        text.append(String.format("Liczba dni w podróży: %s\n", stats.getDaysInTrip()));
        text.append(String.format("Liczba dni w podróży: %s\n", stats.getKilometers()));
        text.append(String.format("Geoblog %s\n", stats.getGeoblogNote()));
        text.append(String.format("Geoblog %s\n", stats.getGeoblogVisitsCount()));

        writeToFile(file, text.toString());
    }

    private static void writeToFile(Path file, String text) {
        try (PrintWriter out = new PrintWriter(file.toAbsolutePath().toString())) {
            out.println(text);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createDirectoryIfNotExists(Path path)
            throws IOException {
        if (Files.notExists(path)) {
            Files.createDirectory(path);
        }
    }

    private static int toSkip(Path path) {
        int directoriesCount = countDirectories(path);
        return directoriesCount == 0 ? 0 : directoriesCount - 1;
    }

    private static int countDirectories(Path path) {
        return new File(path.toAbsolutePath().toString()).listFiles(f -> f.isDirectory()).length;
    }

    private static Path createDirectory(int n, String name, Path currentPath)
            throws IOException {
        String directoryName = directoryName(n, name);
        Path newPath = currentPath.resolve(directoryName);
        createDirectoryIfNotExists(newPath);
        return newPath;
    }

    private static String directoryName(int n, String name) {
        return sanitizeFilename(String.format("%02d. %s", n + 1, name));
    }

    public static String sanitizeFilename(String name) {
        return name.replaceAll("[:\\\\/*?|<>]", "");
    }

}
