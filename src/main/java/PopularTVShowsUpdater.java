import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class PopularTVShowsUpdater implements Runnable {
    // Holds the popular TV shows and the API genres codes
    private ConcurrentHashMap<String, List<String>> popularTVShowsByGenre;
    private HashMap<Integer, String> genresCodesMap;

    // To handle the thread loop exit condition and lists update interval
    private final long updateIntervalInMinutes;

    // Handles the API requests from "The Movies DB"
    private TheMovieDbApiHandler theMovieDbApiHandler;

    // Number of tv shows in each list
    private final int NumOfTvShowsInList = 10;

    public PopularTVShowsUpdater(long updateIntervalInMinuts) {
        popularTVShowsByGenre = new ConcurrentHashMap<>();
        this.updateIntervalInMinutes = updateIntervalInMinuts;
        theMovieDbApiHandler = new TheMovieDbApiHandler();
    }

    public ConcurrentHashMap<String, List<String>> getPopularTVShowsByGenre() {
        return popularTVShowsByGenre;
    }

    private boolean doneUpdatingPopularTVShowsMap(ConcurrentHashMap<String, List<String>> popularTVShowsList) {
        for (List<String> tvShowsNames : popularTVShowsList.values()) {
            if (tvShowsNames.size() < NumOfTvShowsInList) {
                return false;
            }
        }
        return true;
    }

    private String sensitizeGenreName(String genreName) {
        // convert name to lowercase,
        // and underscore instead of other characters, spaces are omitted.
        // Action & Adventure -> action_adventure
        // Sci-Fi & Fantasy -> sci_fi_fantasy
        return genreName.toLowerCase().replace(" ", "").replaceAll("[^a-z]+", "_");
    }

    private HashMap<Integer, String> parseGenresCodeJson(JSONObject genresCodesJson) {
        HashMap<Integer, String> tmpGenresCodesMap = new HashMap<>();

        JSONArray genresJsonArr = genresCodesJson.getJSONArray("genres");
        // Check genresJsonArr
        for (Object jsonObj : genresJsonArr) {
            tmpGenresCodesMap.put(((JSONObject) jsonObj).getInt("id"),
                    sensitizeGenreName(((JSONObject) jsonObj).getString("name")));
        }
        return tmpGenresCodesMap;
    }

    private boolean parsePopularTvShowsJson(JSONObject popularTvShowsJson,
                                            ConcurrentHashMap<String, List<String>> popularTVShowsMap) {
        JSONArray popularTvShowsJsonArr = popularTvShowsJson.getJSONArray("results");
        // Check popularTvShowsJsonArr
        for (Object movieInfoJson : popularTvShowsJsonArr) {
            JSONArray genresIds = ((JSONObject) movieInfoJson).getJSONArray("genre_ids");
            for (Object genreId : genresIds) {
                String genreName = genresCodesMap.get(genreId);
                if (null == genreName) {
                    continue;
                }
                List<String> tvShowsList = popularTVShowsMap.get(genreName);
                if (tvShowsList.size() < NumOfTvShowsInList) { // Make the magic number defined or passed from somewhere.
                    tvShowsList.add(((JSONObject) movieInfoJson).getString("name"));
                }
            }
        }
        return true;
    }

    private boolean updateGenresCodes() throws IOException, URISyntaxException {
        JSONObject genresCodesJson = theMovieDbApiHandler.getGenresCodes();
        if (genresCodesJson == null) {
            return false;
        }

        HashMap<Integer, String> tmpGenresCodesMap = parseGenresCodeJson(genresCodesJson);
        if (tmpGenresCodesMap.isEmpty()) {
            return false;
        }
        genresCodesMap = tmpGenresCodesMap;

        return true;
    }

    private boolean updatePopularTVShows() throws IOException, URISyntaxException {
        try {
            if (!updateGenresCodes()) {
                // If genresCodesMap not empty, should we rely on it and continue with
                // TV shows lists update and not return false?
                return false;
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }

        ConcurrentHashMap<String, List<String>> tmpPopularTVShowsByGenre = new ConcurrentHashMap<>();
        for (String genreName : genresCodesMap.values()) {
            //System.out.println("genreName: " + genreName);
            tmpPopularTVShowsByGenre.put(genreName, new ArrayList<>());
        }
        theMovieDbApiHandler.resetPopularTVShowsPageIndex();
        while (true) {
            JSONObject popularTvShowsJson = theMovieDbApiHandler.getNextPopularTVShowsPage();
            if (popularTvShowsJson == null) {
                return false;
            }

            if (!parsePopularTvShowsJson(popularTvShowsJson, tmpPopularTVShowsByGenre)) {
                return false;
            }

            if (doneUpdatingPopularTVShowsMap(tmpPopularTVShowsByGenre)) {
                // Add a breaking condition were we parsed all the API pages but didn't
                // meet our "done" conditions.
                break;
            }
        }
        popularTVShowsByGenre = tmpPopularTVShowsByGenre;

        return true;
    }

    private void waitForNextUpdateInterval() {
        final long sleepForInMinutes = updateIntervalInMinutes * 60 * 1000; // convert to milliseconds.
        try {
            Thread.sleep(sleepForInMinutes);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (!updatePopularTVShows()) {
                    // This error should go to a log file with extended error explanation if possible
                    System.out.println("Updating popular TV shows lists failed");
                }
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
            waitForNextUpdateInterval();

        }
    }
}
