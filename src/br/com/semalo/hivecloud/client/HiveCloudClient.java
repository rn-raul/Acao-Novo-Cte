package br.com.semalo.hivecloud.client;

import br.com.semalo.hivecloud.model.ApiResponse;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;


public class HiveCloudClient {

    public static final String BASE_URL = "https://cte-api.hivecloud.com.br/api/v1/integracoes";
    public static final String AUTHORIZATION = "--";
    private static final String TENANT_ID = "--";

    private static HttpURLConnection getHttpURLConnection(String endpoint) throws IOException {
        HttpURLConnection conn = (HttpURLConnection)
                new URL(BASE_URL + endpoint).openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        conn.setRequestProperty("Authorization", "Bearer " + AUTHORIZATION);
        conn.setRequestProperty("TenantId", TENANT_ID);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        return conn;
    }

    public ApiResponse enviarParaHive(String jsonBody, String endpoint) throws Exception {

        HttpURLConnection conn = getHttpURLConnection(endpoint);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();

        InputStream is = (responseCode >= 200 && responseCode < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8));

        StringBuilder response = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
            response.append(line);
        }

        br.close();
        conn.disconnect();

        return new ApiResponse(responseCode, response.toString());
    }
}
