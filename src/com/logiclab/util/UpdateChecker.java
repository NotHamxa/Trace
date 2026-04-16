package com.logiclab.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateChecker {
    private static final String REPO = "NotHamxa/LogicLab";
    private static final String API_URL = "https://api.github.com/repos/" + REPO + "/releases/latest";

    private UpdateChecker() {}

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

    public static void checkAsync() {
        Thread t = new Thread(UpdateChecker::checkBlocking, "update-checker");
        t.setDaemon(true);
        t.start();
    }

    private static void checkBlocking() {
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
            String installerUrl = findInstallerAsset(body);
            if (tag == null || installerUrl == null) return;

            String latest = stripV(tag);
            String current = currentVersion();
            if (compareVersions(latest, current) <= 0) return;

            Platform.runLater(() -> promptAndUpdate(latest, installerUrl));
        } catch (Exception ignored) {
            // Network failure — silently skip; user can still use the app.
        }
    }

    private static void promptAndUpdate(String latest, String installerUrl) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "LogicLab " + latest + " is available (you have " + currentVersion() + ").\n\n" +
                        "Download and install now? The app will close while the installer runs.",
                ButtonType.YES, ButtonType.NO);
        a.setTitle("Update available");
        a.setHeaderText("New version available");
        Optional<ButtonType> choice = a.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.YES) return;

        Thread t = new Thread(() -> downloadAndLaunch(installerUrl), "update-downloader");
        t.setDaemon(false);
        t.start();
    }

    private static void downloadAndLaunch(String installerUrl) {
        try {
            Path tmp = Files.createTempFile("LogicLab-installer-", ".exe");
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(installerUrl)).GET().build();
            HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() != 200) return;
            try (InputStream in = resp.body()) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            new ProcessBuilder(tmp.toAbsolutePath().toString()).start();
            Platform.exit();
        } catch (Exception e) {
            Platform.runLater(() -> {
                Alert err = new Alert(Alert.AlertType.ERROR, "Update failed: " + e.getMessage(), ButtonType.OK);
                err.setHeaderText("Could not install update");
                err.showAndWait();
            });
        }
    }

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
