package com.leszko.geodump;

import com.leszko.geodump.model.Overview;
import com.leszko.geodump.model.Overview.OverviewBuilder;
import com.leszko.geodump.model.Post;
import com.leszko.geodump.model.Post.Comment;
import com.leszko.geodump.model.Post.PostBuilder;
import com.leszko.geodump.model.Trip;
import com.leszko.geodump.model.Trip.Stats.StatsBuilder;
import com.leszko.geodump.model.Trip.TripBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

class GeoCrawler {
    private static final String TEMPLATE_URL = "http://%s.geoblog.pl/podroze";
    private final String username;

    GeoCrawler(String username) {
        this.username = username;
    }

    Overview readOverview()
            throws IOException {
        OverviewBuilder result = Overview.builder();
        String url = String.format(TEMPLATE_URL, username);

        List<Overview.Trip> trips =
                Jsoup.connect(url).get()
                     .select(".userJournal a").stream()
                     .filter(trip -> trip.attr("href").startsWith("http://"))
                     .map(trip -> Overview.Trip.builder()
                                               .url(trip.attr("href"))
                                               .name(trip.selectFirst("div div").html())
                                               .build())
                     .collect(toList());
        result.trips(trips);

        return result.build();
    }

    Trip readTrip(String url)
            throws IOException {
        TripBuilder result = Trip.builder();
        Document doc = Jsoup.connect(url).get();

        result.url(url);
        result.stats(parseTripStats(doc));
        result.name(doc.selectFirst(".title").html());
        result.posts(parsePosts(doc));

        return result.build();
    }

    private static Trip.Stats parseTripStats(Document doc) {
        StatsBuilder result = Trip.Stats.builder();

        Elements journalStats = doc.select(".journalStats span");
        if (journalStats.size() >= 3) {
            result.daysInTrip(journalStats.get(0).html());
            result.countries(journalStats.get(1).html());
            result.kilometers(journalStats.get(2).html());
        }

        Elements note = doc.select(".note");
        if (note.size() >= 2) {
            result.geoblogNote(note.get(0).html());
            result.geoblogVisitsCount(note.get(1).html());
        }

        return result.build();
    }

    private List<Trip.Post> parsePosts(Document doc) {
        return doc.select("#journalEntriesList a").stream()
                  .map(entry -> Trip.Post.builder()
                                         .url(entry.attr("href"))
                                         .name(entry.selectFirst(".eTitle").html())
                                         .place(parseTripPlace(entry))
                                         .date(entry.selectFirst(".eDate").html())
                                         .build()).collect(toList());
    }

    private static String parseTripPlace(Element element) {
        String country = element.selectFirst(".entryData div div div").childNode(0).toString().trim();
        String city = element.selectFirst(".entryData div div div strong").html().trim();
        return String.format("%s %s", country, city);
    }

    Post readPost(String url)
            throws IOException {
        PostBuilder result = Post.builder();
        Document doc = Jsoup.connect(url).get();

        result.url(url);
        result.date(parsePostDate(doc));
        result.distance(doc.selectFirst(".bottom div span span").html());
        result.name(doc.selectFirst("h1").html());
        result.place(parsePlace(doc));
        result.text(parseText(doc));
        result.mediaLinks(parseMediaLinks(doc));
        result.comments(parseComments(doc));

        return result.build();
    }

    private String parsePostDate(Document doc) {
        Elements year = doc.select(".year");
        Elements month = doc.select(".msc");
        Elements day = doc.select(".day");
        if (year.size() == 1 && day.size() == 1 && month.size() == 1) {
            String yearString = year.html();
            String monthString = toMonthNumber(month.html());
            String dayString = day.html();
            return String.format("%s.%s.%s", dayString, monthString, yearString);
        }
        return null;
    }

    private String parseText(Document doc) {
        return doc.selectFirst("#entryBody").childNodes().stream()
                  .map(child -> child.toString())
                  .filter(childString -> !childString.startsWith("<div"))
                  .collect(Collectors.joining()).trim();
    }

    private List<String> parseMediaLinks(Document doc) {
        return doc.select("#newGalleries li a").stream()
                  .map(image -> image.attr("href"))
                  .map(link -> photoNumberOf(link))
                  .map(photoNumber -> String.format("http://%s.geoblog.pl/zdjecie/duze/%s", username, photoNumber))
                  .map(link -> extractMediaLink(link))
                  .collect(toList());
    }

    private static String photoNumberOf(String link) {
        String[] parts = link.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            try {
                Integer.parseInt(parts[i]);
                return parts[i];
            } catch (Exception e) {
                // do nothing, continue
            }
        }
        throw new RuntimeException(String.format("Invalid photo link: '%s'", link));
    }

    private String extractMediaLink(String url) {
        try {
            List<String> image = Jsoup.connect(url).get().select(".mapa img").stream()
                                      .map(img -> img.attr("src"))
                                      .filter(link -> link.startsWith("http://"))
                                      .filter(link -> link.contains("gallery"))
                                      .collect(toList());
            if (image.size() == 1) {
                return image.get(0);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException(String.format("Unknown link '%s'", url));
    }

    private List<Comment> parseComments(Document doc) {
        return Stream.concat(
                doc.select(".commentLine div.active").stream(),
                doc.select(".commentLine div.inactive").stream())
                     .map(entry -> Comment.builder()
                                          .username(parseCommentUsername(entry))
                                          .date(entry.selectFirst("span[style=color: #898989; font-size: 8pt;]").html()
                                                     .substring(8))
                                          .text(entry.selectFirst("div[style=padding-left: 7px;]").html())
                                          .build())
                     .collect(toList());
    }

    private String parseCommentUsername(Element entry) {
        Elements username = entry.select("a");
        if (username.size() >= 1) {
            return username.html();
        }
        username = entry.select("span[style=color: #898989; font-weight: bold; font-size: 10pt;]");
        if (username.size() >= 1) {
            return username.html();
        }
        return null;
    }

    private static String parsePlace(Element element) {
        Elements country = element.select(".entryPlace");
        Elements city = element.select(".entryPlace strong");
        if (country.size() == 1 && city.size() == 1) {
            String countryString = country.get(0).childNode(0).toString().trim();
            String cityString = city.get(0).childNode(0).toString().trim();
            return String.format("%s%s", countryString, cityString);
        }
        return null;
    }

    private static String toMonthNumber(String month) {
        switch (month) {
            case "sty":
                return "01";
            case "lut":
                return "02";
            case "mar":
                return "03";
            case "kwi":
                return "04";
            case "maj":
                return "05";
            case "cze":
                return "06";
            case "lip":
                return "07";
            case "sie":
                return "08";
            case "wrz":
                return "09";
            case "paź":
                return "10";
            case "lis":
                return "11";
            case "gru":
                return "12";
            default:
                System.out.println(String.format("WARNING: Unknown month '%s'", month));
                return "00";
        }
    }
}
