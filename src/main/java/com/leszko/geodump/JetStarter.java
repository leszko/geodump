package com.leszko.geodump;

import com.hazelcast.core.IMap;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JetConfig;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.leszko.geodump.model.Overview;
import com.leszko.geodump.model.Post;
import com.leszko.geodump.model.Trip;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import static com.hazelcast.jet.Traversers.traverseStream;
import static java.util.stream.Collectors.toList;

public class JetStarter implements Serializable {
    private final GeoCrawler geoCrawler;

    private JetStarter(String username) {
        this.geoCrawler = new GeoCrawler(username);
    }

    public static void main(String[] args)
            throws IOException, ExecutionException, InterruptedException {
        new JetStarter("marianka").start();
    }

    private void start()
            throws IOException, ExecutionException, InterruptedException {
        GeoCrawler geoCrawler = new GeoCrawler("marianka");
        List<String> trips = geoCrawler.readOverview().getTrips().stream()
                                       .map(Overview.Trip::getUrl)
                                       .collect(toList());

        //        stream(trips);
        //        parallelStream(trips);
        //        parallelStreamCustomThreads(trips);
        hazelcastJet(trips);
    }

    private void stream(List<String> trips) {
        long start = System.currentTimeMillis();
        List<String> posts = trips.stream()
                                  .map(this::readTrip)
                                  .flatMap(trip -> trip.getPosts().stream())
                                  .map(Trip.Post::getUrl)
                                  .map(this::readPost)
                                  .map(this::formatPost)
                                  .collect(toList());
        long end = System.currentTimeMillis();
        System.out.println(String.format("Stream time: %s [s]", (end - start) / 1000L));
    }

    private void parallelStream(List<String> trips) {
        long start = System.currentTimeMillis();
        List<String> posts2 = trips.parallelStream()
                                   .map(this::readTrip)
                                   .flatMap(trip -> trip.getPosts().parallelStream())
                                   .map(Trip.Post::getUrl)
                                   .map(this::readPost)
                                   .map(this::formatPost)
                                   .collect(toList());
        long end = System.currentTimeMillis();
        System.out.println(String.format("Parallel Stream time: %s [s]", (end - start) / 1000L));
    }

    private void parallelStreamCustomThreads(List<String> trips)
            throws InterruptedException, ExecutionException {
        long start = System.currentTimeMillis();
        ForkJoinPool myPool = new ForkJoinPool(20);
        myPool.submit(() ->
                trips.parallelStream()
                     .map(this::readTrip)
                     .flatMap(trip -> trip.getPosts().parallelStream())
                     .map(Trip.Post::getUrl)
                     .map(this::readPost)
                     .map(this::formatPost)
                     .collect(toList())).get();
        long end = System.currentTimeMillis();
        System.out.println(String.format("Parallel Stream (20 threads) time: %s [s]", (end - start) / 1000L));
    }

    private void hazelcastJet(List<String> trips) {
        Pipeline p = Pipeline.create();
        p.drawFrom(Sources.<Long, String>map("tripUrls"))
         .map(e -> e.getValue())
         .map(this::readTrip)
         .flatMap(trip -> traverseStream(trip.getPosts().stream()))
         .map(Trip.Post::getUrl)
         .map(this::readPost)
         .map(this::formatPost)
         .drainTo(Sinks.list("result"));

        JetInstance jet = Jet.newJetClient();
        try {
            IMap<Long, String> input = jet.getMap("tripUrls");
            for (int i = 0; i < trips.size(); i++) {
                input.put(new Long(i), trips.get(i));
            }
            JobConfig jobConfig = new JobConfig();
            jobConfig.addClass(JetStarter.class);
            jobConfig.addClass(GeoCrawler.class);
            jobConfig.addClass(Overview.class);
            jobConfig.addClass(Overview.Trip.class);
            jobConfig.addClass(Overview.OverviewBuilder.class);
            jobConfig.addClass(Trip.class);
            jobConfig.addClass(Trip.Post.class);
            jobConfig.addClass(Trip.Post.PostBuilder.class);
            jobConfig.addClass(Trip.Stats.class);
            jobConfig.addClass(Trip.Stats.StatsBuilder.class);
            jobConfig.addClass(Trip.TripBuilder.class);
            jobConfig.addClass(Post.class);
            jobConfig.addClass(Post.Comment.class);
            jobConfig.addClass(Post.Comment.CommentBuilder.class);
            jobConfig.addClass(Post.PostBuilder.class);

            jobConfig.addJar(new File("C:\\Users\\rafal\\.gradle\\caches\\modules-2\\files-2.1\\org.jsoup\\jsoup\\1.11.3\\36da09a8f68484523fa2aaa100399d612b247d67\\jsoup-1.11.3.jar"));
            long start = System.currentTimeMillis();
            jet.newJob(p,jobConfig).join();
            List<String> result = jet.getList("result");
            long end = System.currentTimeMillis();
            System.out.println(String.format("Hazelcast Jet time: %s [s]", (end - start) / 1000L));
        } finally {
            Jet.shutdownAll();
        }
    }

    private String formatPost(Post post) {
        StringBuilder result = new StringBuilder();
        result.append(post.getName());
        result.append("\n=============================================\n\n");
        result.append(post.getText());
        return result.toString();
    }

    private Trip readTrip(String tripUrl) {
        try {
            System.out.println(String.format("Reading trip: %s", tripUrl));
            return geoCrawler.readTrip(tripUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Post readPost(String postUrl) {
        try {
            System.out.println(String.format("Reading post: %s", postUrl));
            return geoCrawler.readPost(postUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
