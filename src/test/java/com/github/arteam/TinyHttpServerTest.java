package com.github.arteam;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertThat;

/**
 * @author Artem Prigoda
 * @since 05.06.16
 */
public class TinyHttpServerTest {

    private TinyHttpServer httpServer;
    private CloseableHttpClient httpClient = HttpClients.createMinimal();

    @Before
    public void setUp() throws Exception {
        httpServer = new TinyHttpServer().addHandler("/get", (request, response) -> {
            System.out.println(request);
            response.setBody("Hello, World!")
                    .addHeader("content-type", "text/plain");
        }).addHandler("/post", (request, response) -> {
            System.out.println(request);
            System.out.println(request.getFirstHeader("content-type"));
            response.setBody("{\"message\": \"Roger that!\"}")
                    .addHeader("content-type", "application/json");
        }).start();
        System.out.println("Server port is: " + httpServer.getPort());
        System.out.println("Server host is: " + httpServer.getBindHost());
    }

    @After
    public void tearDown() throws Exception {
        httpClient.close();
        httpServer.stop();
    }

    @Test
    public void testHelloWorld() throws Exception {
        assertGetHelloWorld(httpClient);
    }

    @Test
    public void testSeveralHelloWorlds() throws Exception {
        for (int i = 0; i < 100; i++) {
            assertGetHelloWorld(httpClient);
        }
    }

    private void assertGetHelloWorld(CloseableHttpClient httpClient) throws java.io.IOException {
        HttpGet httpGet = new HttpGet(String.format("http://127.0.0.1:%s/get", httpServer.getPort()));
        String response = httpClient.execute(httpGet, httpResponse -> {
            assertThat(httpResponse.getFirstHeader("Content-Type").getValue(), CoreMatchers.equalTo("text/plain"));
            return EntityUtils.toString(httpResponse.getEntity());
        });
        assertThat(response, CoreMatchers.equalTo("Hello, World!"));
    }

    @Test
    public void testPost() throws Exception {
        HttpPost httpPost = new HttpPost(String.format("http://127.0.0.1:%s/post", httpServer.getPort()));
        httpPost.setEntity(new StringEntity("{\"name\":\"Hello, World!\"}", ContentType.APPLICATION_JSON));
        String response = httpClient.execute(httpPost, httpResponse -> {
            assertThat(httpResponse.getFirstHeader("Content-Type").getValue(), CoreMatchers.equalTo("application/json"));
            return EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
        });
        assertThat(response, CoreMatchers.equalTo("{\"message\": \"Roger that!\"}"));
    }
}