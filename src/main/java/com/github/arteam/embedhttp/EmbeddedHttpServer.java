package com.github.arteam.embedhttp;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Represents a simple HTTP server (a facade around {@link com.sun.net.httpserver.HttpServer for unit testing.
 * The server is started after invoking the {@link EmbeddedHttpServer#start()} method. It's a good practice
 * to shutdown it with {@link EmbeddedHttpServer#stop()} method.
 */
public class EmbeddedHttpServer implements Closeable {

    private HttpServer sunHttpServer;
    private final List<HandlerConfig> handlers = new ArrayList<>();
    private Executor executor;

    /**
     * Adds a new handler to the server to a path.
     */
    public EmbeddedHttpServer addHandler(String path, HttpHandler handler) {
        return addHandler(path, handler, null);
    }

    /**
     * Adds a new handler to the server to a path with an authenticator.
     */
    public EmbeddedHttpServer addHandler(String path, HttpHandler handler, Authenticator authenticator) {
        handlers.add(new HandlerConfig(path, handler, authenticator));
        return this;
    }

    /**
     * Adds a new handler to the server to a path with an authenticator.
     */
    EmbeddedHttpServer addHandler(HandlerConfig handler) {
        handlers.add(handler);
        return this;
    }


    /**
     * Adds a list handler to the server to a path with an authenticator.
     */
    EmbeddedHttpServer addHandlers(Collection<HandlerConfig> handler) {
        handlers.addAll(handler);
        return this;
    }

    void addExecutor(Executor executor) {
        this.executor = executor;
    }

    /**
     * Starts up the current server on a free port on the localhost.
     */
    public EmbeddedHttpServer start() {
        return start(0);
    }

    /**
     * Starts up the server on the provided port on the provided port on the localhost.
     */
    public EmbeddedHttpServer start(int port) {
        return start(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
    }

    /**
     * Starts up the server on provided address.
     */
    public EmbeddedHttpServer start(InetSocketAddress address) {
        try {
            sunHttpServer = HttpServer.create(address, 50);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (HandlerConfig config : handlers) {
            HttpContext context = sunHttpServer.createContext(config.getPath(), httpExchange -> {
                try {
                    Headers requestHeaders = httpExchange.getRequestHeaders();
                    HttpResponse response = new HttpResponse();
                    HttpHandler handler = config.getHttpHandler();
                    handler.handle(new HttpRequest(httpExchange.getRequestMethod(),
                            httpExchange.getRequestURI(), httpExchange.getProtocol(), requestHeaders,
                            readFromStream(httpExchange.getRequestBody())), response);
                    for (Map.Entry<String, List<String>> e : response.getHeaders().entrySet()) {
                        httpExchange.getResponseHeaders().put(e.getKey(), e.getValue());
                    }
                    byte[] byteBody = response.getBody().getBytes(StandardCharsets.UTF_8);
                    httpExchange.sendResponseHeaders(response.getStatusCode(), byteBody.length);
                    httpExchange.getResponseBody().write(byteBody);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    httpExchange.close();
                }
            });
            if (config.getAuthenticator() != null) {
                context.setAuthenticator(config.getAuthenticator());
            }
        }
        if (executor != null) {
            sunHttpServer.setExecutor(executor);
        }
        sunHttpServer.start();
        return this;
    }

    /**
     * Stops the current server and frees resources.
     */
    public void stop() {
        sunHttpServer.stop(0);
    }

    /**
     * Invokes {@link EmbeddedHttpServer#stop()}.
     */
    @Override
    public void close() throws IOException {
        stop();
    }

    /**
     * Get the port on which server has been started.
     */
    public int getPort() {
        return sunHttpServer.getAddress().getPort();
    }

    /**
     * Gets the host on which server has been bound.
     */
    public String getBindHost() {
        return sunHttpServer.getAddress().getHostName();
    }

    /**
     * Reads the provided input stream to a string in the UTF-8 encoding
     */
    private static String readFromStream(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

}
