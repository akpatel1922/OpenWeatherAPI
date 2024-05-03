package org.example;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;

public class Main {
    public static void main(String[] args) throws IOException {

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/weather", new WeatherHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port 8080");

    }

    static class WeatherHandler implements HttpHandler {
        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
            String response = "";
            if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                // Get query parameters (latitude and longitude)
                String query = exchange.getRequestURI().getQuery();
                String[] params = query.split("&");
                String lat = null, lon = null;
                for (String param : params) {
                    String[] pair = param.split("=");
                    if (pair[0].equalsIgnoreCase("lat")) {
                        lat = pair[1];
                    } else if (pair[0].equalsIgnoreCase("lon")) {
                        lon = pair[1];
                    }
                }
                if (lat != null && lon != null) {
                    // Fetch weather data from OpenWeather API
                    String apiKey = "Your_API_Key";
                    String apiUrl = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon + "&appid=" + apiKey;
                    System.out.println(apiUrl);
                    URL url = new URL(apiUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();
                    int responseCode = connection.getResponseCode();
                    if (responseCode == 200) {
                        // Read response
                        Scanner scanner = new Scanner(url.openStream());
                        String responseBody = scanner.useDelimiter("\\A").next();
                        scanner.close();
                        JSONObject json = null;
                        try {
                            json = new JSONObject(responseBody);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        // Extract weather condition and temperature
                        String weatherCondition = null;
                        try {
                            weatherCondition = json.getJSONArray("weather").getJSONObject(0).getString("main");
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        double temperature = 0; // Convert from Kelvin to Celsius
                        try {
                            temperature = json.getJSONObject("main").getDouble("temp") - 273.15;
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        // Determine weather type
                        String weatherType;
                        if (temperature >= 30) {
                            weatherType = "hot";
                        } else if (temperature <= 10) {
                            weatherType = "cold";
                        } else {
                            weatherType = "moderate";
                        }
                        // Prepare response
                        response = "Weather Condition: " + weatherCondition + "\n";
                        response += "Temperature: " + String.format("%.2f", temperature) + "°C\n";
                        response += "Weather Type: " + weatherType;
                    } else {
                        response = "Error fetching weather data. Response code: " + responseCode;
                    }
                } else {
                    response = "Latitude and longitude parameters are required.";
                }
            } else {
                response = "Unsupported HTTP method. Only GET requests are supported.";
            }
            // Send response
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            System.out.println(os);
            os.write(response.getBytes());
            os.close();
        }
    }
}
