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
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int code = conn.getResponseCode();
                if (code == 200) {
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

    /** Perform a POST request with a JSON body on a background thread. */
    public static void post(String path, String jsonBody, Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                OutputStream os = conn.getOutputStream();
                os.write(jsonBody.getBytes("UTF-8"));
                os.close();

                int code = conn.getResponseCode();
                if (code == 200 || code == 201) {
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
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }
}
