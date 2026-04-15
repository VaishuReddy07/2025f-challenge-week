package com.epita.fitnesstracker.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Minimal HTTP helper that talks to the Flask backend.
 * Uses HttpURLConnection so there is no extra dependency.
 */
public class ApiClient {

    // Use 10.0.2.2 from the Android emulator to reach the host machine's localhost.
    private static final String BASE_URL = "http://10.0.2.2:5000";
    private static final String API_KEY = "dev-api-key-change-in-production";

    public interface Callback {
        void onSuccess(String responseBody);
        void onError(Exception e);
    }

    /** Perform a GET request on a background thread. */
    public static void get(String path, Callback callback) {
        request("GET", path, null, callback);
    }

    /** Perform a POST request with a JSON body on a background thread. */
    public static void post(String path, String jsonBody, Callback callback) {
        request("POST", path, jsonBody, callback);
    }

    /** Perform a DELETE request on a background thread. */
    public static void delete(String path, Callback callback) {
        request("DELETE", path, null, callback);
    }

    private static void request(String method, String path, String body, Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(method);
                conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (body != null) {
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    OutputStream os = conn.getOutputStream();
                    os.write(body.getBytes("UTF-8"));
                    os.close();
                }

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    callback.onSuccess(readStream(conn));
                } else {
                    callback.onError(new IOException("HTTP " + code));
                }
                conn.disconnect();
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    private static String readStream(HttpURLConnection conn) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            // Handle error streams as well
            if (conn.getErrorStream() != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    return sb.toString();
                }
            }
            throw e;
        }
    }
}
