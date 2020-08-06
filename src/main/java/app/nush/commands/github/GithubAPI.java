package app.nush.commands.github;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.Objects;

public class GithubAPI {

    private final Gson gson;
    private final OkHttpClient client;

    public GithubAPI() {
        gson = new Gson();
        client = new OkHttpClient();
    }

    public Release[] releases(String owner, String repo) throws IOException {
        Request request = new Request.Builder()
                .url("https://api.github.com/repos/" + URLEncoder.encode(owner) + "/" + URLEncoder.encode(repo) + "/releases")
                .build();
        Response response = client.newCall(request).execute();
        return gson.fromJson(new InputStreamReader(Objects.requireNonNull(response.body()).byteStream()), Release[].class);
    }

    public static class Release {
        public String url, assets_url, upload_url, html_url;
        public int id;
        public String node_id, tag_name, target_commitish, name;
        public boolean draft;
        public User author;
        public boolean prerelease;
        public String created_at, published_at;
        public Asset[] assets;
        public String tarball_url, zipball_url, body;
    }

    public static class User {
        public String login;
        public int id;
        public String node_id, avatar_url, gravatar_id, url, html_url, followers_url, following_url, gists_url,
                starred_url, subscriptions_url, organizations_url, repos_url, events_url, received_events_url, type;
        public boolean site_admin;
    }

    public static class Asset {
        public String url;
        public int id;
        public String node_id;
        public String name;
        public Object label;
        public User uploader;
        public String content_type;
        public String state;
        public int size;
        public int download_count;
        public String created_at;
        public String updated_at;
        public String browser_download_url;
    }
}
