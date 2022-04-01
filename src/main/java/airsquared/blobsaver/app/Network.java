package airsquared.blobsaver.app;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Network {

    // one instance, reuse
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

    // Performs a POST Request with the specified URL and Parameters
    public static HttpResponse<String> makePOSTRequest(String url, Map<Object, Object> parameters, Map<String, String> headers, boolean convertParamtersToJSON) throws IOException, InterruptedException {

        // convert Arguments to JSON (and use them if convertParametersToJSON is true)
        Gson gson = new Gson();
        String JSONParameters = gson.toJson(parameters);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .POST(convertParamtersToJSON ? BodyPublishers.ofString(JSONParameters) : buildFormDataFromMap(parameters))
                .uri(URI.create(url));

        for (Map.Entry<String, String> entry : headers.entrySet())
            requestBuilder.header(entry.getKey(), entry.getValue());

        HttpRequest request = requestBuilder.build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }


    private static HttpRequest.BodyPublisher buildFormDataFromMap(Map<Object, Object> data) {
        var builder = new StringBuilder();
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }
        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }
}