package com.logiclab.util;

import javafx.application.Platform;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateChecker {
    private static final String REPO = "NotHamxa/LogicLab";
    private static final String API_URL = "https://api.github.com/repos/" + REPO + "/releases/latest";

    public interface Listener {
        void onAvailable(String version);
        void onProgress(double fraction);
        void onReady(Path installer);
        void onError(String message);
    }

    private final Listener listener;
    private volatile String installerUrl;
    private volatile String latestVersion;
    private volatile Path downloadedInstaller;

    public UpdateChecker(Listener listener) {
        this.listener = listener;
    }

    public static String currentVersion() {
        try (InputStream in = UpdateChecker.class.getResourceAsStream("/version.properties")) {
            if (in == null) return "0.0";
            Properties p = new Properties();
            p.load(in);
            return p.getProperty("version", "0.0");
        } catch (IOException e) {
            return "0.0";
        }
    }

    public void checkAsync() {
        Thread t = new Thread(this::checkBlocking, "update-checker");
        t.setDaemon(true);
        t.start();
    }

    private void checkBlocking() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(API_URL))
                    .header("Accept", "application/vnd.github+json")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return;

            String body = resp.body();
            String tag = jsonField(body, "tag_name");
            String exe = findInstallerAsset(body);
            if (tag == null || exe == null) return;

            String latest = stripV(tag);
            if (compareVersions(latest, currentVersion()) <= 0) return;

            this.latestVersion = latest;
            this.installerUrl = exe;
            Platform.runLater(() -> listener.onAvailable(latest));
        } catch (Exception e) {
            // Silent — no network or rate-limited; user can still use the app.
        }
    }

    /** Begin downloading the installer in a background thread. */
    public void startDownload() {
        if (installerUrl == null) return;
        Thread t = new Thread(this::downloadBlocking, "update-downloader");
        t.setDaemon(true);
        t.start();
    }

    private void downloadBlocking() {
        try {
            Path tmp = Files.createTempFile("LogicLab-installer-", ".exe");
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(installerUrl)).GET().build();
            HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() != 200) {
                Platform.runLater(() -> listener.onError("HTTP " + resp.statusCode()));
                return;
            }

            long total = resp.headers().firstValueAsLong("Content-Length").orElse(-1L);
            try (InputStream in = resp.body();
                 OutputStream out = Files.newOutputStream(tmp)) {
                byte[] buf = new byte[64 * 1024];
                long read = 0;
                int n;
                long lastTick = 0;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    read += n;
                    long now = System.currentTimeMillis();
                    if (total > 0 && now - lastTick > 100) {
                        double frac = (double) read / (double) total;
                        Platform.runLater(() -> listener.onProgress(frac));
                        lastTick = now;
                    }
                }
            }

            downloadedInstaller = tmp;
            Platform.runLater(() -> {
                listener.onProgress(1.0);
                listener.onReady(tmp);
            });
        } catch (Exception e) {
            Platform.runLater(() -> listener.onError(e.getMessage()));
        }
    }

    /** Launch the downloaded installer and exit the app. */
    public void installAndExit() {
        if (downloadedInstaller == null) return;
        try {
            new ProcessBuilder(downloadedInstaller.toAbsolutePath().toString()).start();
            Platform.exit();
        } catch (IOException e) {
            Platform.runLater(() -> listener.onError(e.getMessage()));
        }
    }

    public String getLatestVersion() { return latestVersion; }

    private static String findInstallerAsset(String json) {
        Matcher m = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.exe)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static String jsonField(String json, String name) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static String stripV(String tag) {
        return tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag;
    }

    private static int compareVersions(String a, String b) {
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        int n = Math.max(pa.length, pb.length);
        for (int i = 0; i < n; i++) {
            int ai = i < pa.length ? parseInt(pa[i]) : 0;
            int bi = i < pb.length ? parseInt(pb[i]) : 0;
            if (ai != bi) return Integer.compare(ai, bi);
        }
        return 0;
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s.replaceAll("\\D.*", "")); }
        catch (NumberFormatException e) { return 0; }
    }
}
