import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;

public class TheMovieDbApiHandler {

    private final String theMovieDbApiKey = "2bd8b1f8f29d23972a1499b54d8ef38d";
    private final String theMovieDbTvShowsUrl = "https://api.themoviedb.org/3/tv/%s?";
    private final String theMovieDbGenreUrl = "https://api.themoviedb.org/3/genre/tv/list?";

    // popular TV shows page index variables
    private int popularTvShowsPage = 0;
    private final int popularTvShowsMinPage = 1;
    private final int popularTvShowsMaxPage = 500;

    @Nullable
    private JSONObject getPopularTVShows() throws URISyntaxException, IOException {
        String popularTvShowsApi = String.format(theMovieDbTvShowsUrl, "popular");
        URIBuilder url = new URIBuilder(popularTvShowsApi);

        url.setParameter("api_key", theMovieDbApiKey).setParameter("language", "en-US")
                .setParameter("page", String.valueOf(popularTvShowsPage));
        URL paramUrl = url.build().toURL();

        return createHttpReq(paramUrl);
    }

    @Nullable
    public JSONObject getNextPopularTVShowsPage() throws IOException, URISyntaxException {
        popularTvShowsPage += 1;
        if (popularTvShowsPage > popularTvShowsMaxPage) {
            resetPopularTVShowsPageIndex();
        }
        return getPopularTVShows();
    }

    public void resetPopularTVShowsPageIndex() {
        popularTvShowsPage = popularTvShowsMinPage - 1;
    }

    @Nullable
    public JSONObject getGenresCodes() throws URISyntaxException, IOException {
        URIBuilder url = new URIBuilder(theMovieDbGenreUrl);

        url.setParameter("api_key", theMovieDbApiKey).setParameter("language", "en-US");
        URL paramUrl = url.build().toURL();

        return createHttpReq(paramUrl);
    }

    private JSONObject createHttpReq(URL paramUrl) throws IOException {
        HttpURLConnection con = (HttpURLConnection) paramUrl.openConnection();

        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);

        // Handling errors
        if (con.getResponseCode() != 200) {
            // This error should go to a log file with extended error explanation if possible
            System.out.println("Http request returned error code " + con.getResponseCode());
            return null;
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }

        in.close();
        con.disconnect();

        return new JSONObject(content.toString());
    }
}
