package airsquared.blobsaver.app;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
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

    static JsonElement makeRequest(String url) throws IOException {
        try (var inputStream = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {
            return JsonParser.parseReader(inputStream);
        }
    }

    /**
     * Source: https://github.com/jcodec/jcodec/blob/6e1ec651eca92d21b41f9790143a0e6e4d26811e/android/src/main/org/jcodec/common/io/HttpChannel.java
     *
     * @author The JCodec project
     */
    static final class HttpChannel implements SeekableByteChannel {

        private final URL url;
        private ReadableByteChannel ch;
        private long pos;
        private long length;

        public HttpChannel(URL url) {
            this.url = url;
        }

        @Override
        public long position() {
            return pos;
        }

        @Override
        public SeekableByteChannel position(long newPosition) throws IOException {
            if (newPosition == pos) {
                return this;
            } else if (ch != null) {
                ch.close();
                ch = null;
            }
            pos = newPosition;
            return this;
        }

        @Override
        public long size() throws IOException {
            ensureOpen();
            return length;
        }

        @Override
        public SeekableByteChannel truncate(long size) {
            throw new UnsupportedOperationException("Truncate on HTTP is not supported.");
        }

        @Override
        public int read(ByteBuffer buffer) throws IOException {
            ensureOpen();
            int read = ch.read(buffer);
            if (read != -1)
                pos += read;
            return read;
        }

        @Override
        public int write(ByteBuffer buffer) {
            throw new UnsupportedOperationException("Write to HTTP is not supported.");
        }

        @Override
        public boolean isOpen() {
            return ch != null && ch.isOpen();
        }

        @Override
        public void close() throws IOException {
            ch.close();
        }

        private void ensureOpen() throws IOException {
            if (ch == null) {
                URLConnection connection = url.openConnection();
                if (pos > 0)
                    connection.addRequestProperty("Range", "bytes=" + pos + "-");
                ch = Channels.newChannel(connection.getInputStream());
                String resp = connection.getHeaderField("Content-Range");
                if (resp != null) {
                    length = Long.parseLong(resp.split("/")[1]);
                } else {
                    resp = connection.getHeaderField("Content-Length");
                    length = Long.parseLong(resp);
                }
            }
        }

    }
}