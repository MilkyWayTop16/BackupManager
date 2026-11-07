package ru.milkyway.backupmanager;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GofileClient {

    private static final String SERVERS_ENDPOINT = "https://api.gofile.io/servers";
    private static final Pattern SERVER_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DOWNLOAD_PAGE_PATTERN = Pattern.compile("\"downloadPage\"\\s*:\\s*\"([^\"]+)\"");

    public static String getBestServer() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVERS_ENDPOINT))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                Matcher matcher = SERVER_PATTERN.matcher(response.body());
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        } catch (Exception ignored) {
        }
        return "store1";
    }

    public static String upload(Path zipFile) throws Exception {
        String server = getBestServer();
        String uploadUrl = "https://" + server + ".gofile.io/contents/uploadfile";

        String boundary = "----BackupManager" + UUID.randomUUID().toString().replace("-", "");
        String lineEnd = "\r\n";

        String prefix = "--" + boundary + lineEnd
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + zipFile.getFileName().toString() + "\"" + lineEnd
                + "Content-Type: application/zip" + lineEnd
                + lineEnd;

        String suffix = lineEnd + "--" + boundary + "--" + lineEnd;

        byte[] prefixBytes = prefix.getBytes(StandardCharsets.UTF_8);
        byte[] suffixBytes = suffix.getBytes(StandardCharsets.UTF_8);

        long fileSize = Files.size(zipFile);
        long totalLength = prefixBytes.length + fileSize + suffixBytes.length;

        URL url = URI.create(uploadUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(120000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setFixedLengthStreamingMode(totalLength);

        try (OutputStream out = conn.getOutputStream()) {
            out.write(prefixBytes);

            try (InputStream in = new BufferedInputStream(Files.newInputStream(zipFile), 65536)) {
                byte[] buf = new byte[65536];
                int read;
                while ((read = in.read(buf)) != -1) {
                    if (Thread.currentThread().isInterrupted()) {
                        conn.disconnect();
                        throw new InterruptedException("Загрузка прервана");
                    }
                    out.write(buf, 0, read);
                }
            }

            out.write(suffixBytes);
            out.flush();
        }

        int code = conn.getResponseCode();
        InputStream respStream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder responseBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(respStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
        }

        String json = responseBuilder.toString();
        Matcher matcher = DOWNLOAD_PAGE_PATTERN.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new IllegalStateException("Сервер Gofile вернул неожиданный ответ: " + json);
    }
}
