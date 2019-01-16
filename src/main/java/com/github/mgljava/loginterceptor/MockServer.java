package com.github.mgljava.loginterceptor;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;

public class MockServer {

  private static WireMockServer wireMockServer = new WireMockServer();

  public static void main(String[] args) {
    startServer();
  }

  private static void startServer() {

    wireMockServer.start();
    configureFor("localhost", 8080);
    stubFor(
        get(urlEqualTo("/user/get"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{ \"id\": \"1234\", name: \"John Smith\" }")));

    stubFor(
        post(urlEqualTo("/user/create"))
            .withHeader("content-type", equalTo("application/json"))
            .withRequestBody(containing("id"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{ \"id\": \"1234\", name: \"John Smith\" }")));
  }
}
