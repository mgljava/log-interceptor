package com.github.mgljava.loginterceptor.config;

import com.github.mgljava.loginterceptor.config.HttpLoggingInterceptor.Level;
import feign.Client;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.commons.httpclient.OkHttpClientConnectionPoolFactory;
import org.springframework.cloud.commons.httpclient.OkHttpClientFactory;
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@ConditionalOnMissingBean(okhttp3.OkHttpClient.class)
public class OkHttpFeignConfiguration {

  @Value("${feign.okhttp.logging.level}")
  private String level;
  private okhttp3.OkHttpClient okHttpClient;

  @Bean
  @Order(5)
  public HttpLoggingInterceptor httpLoggingInterceptor() {
    final HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
    httpLoggingInterceptor.setLevel(Level.valueOf(level));
    return httpLoggingInterceptor;
  }

  @Bean
  @ConditionalOnMissingBean(ConnectionPool.class)
  public ConnectionPool httpClientConnectionPool(FeignHttpClientProperties httpClientProperties,
      OkHttpClientConnectionPoolFactory connectionPoolFactory) {
    Integer maxTotalConnections = httpClientProperties.getMaxConnections();
    Long timeToLive = httpClientProperties.getTimeToLive();
    TimeUnit ttlUnit = httpClientProperties.getTimeToLiveUnit();
    return connectionPoolFactory.create(maxTotalConnections, timeToLive, ttlUnit);
  }

  @Bean
  public OkHttpClient client(OkHttpClientFactory httpClientFactory,
      ConnectionPool connectionPool, FeignHttpClientProperties httpClientProperties,
      List<Interceptor> interceptors) {
    Boolean followRedirects = httpClientProperties.isFollowRedirects();
    Integer connectTimeout = httpClientProperties.getConnectionTimeout();
    OkHttpClient.Builder builder = httpClientFactory
        .createBuilder(httpClientProperties.isDisableSslValidation())
        .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
        .followRedirects(followRedirects)
        .connectionPool(connectionPool);
    interceptors.forEach(builder::addInterceptor);
    this.okHttpClient = builder.build();
    return this.okHttpClient;
  }

  @PreDestroy
  public void destroy() {
    if (okHttpClient != null) {
      okHttpClient.dispatcher().executorService().shutdown();
      okHttpClient.connectionPool().evictAll();
    }
  }

  @Bean
  @ConditionalOnMissingBean(Client.class)
  public Client feignClient(okhttp3.OkHttpClient client) {
    return new feign.okhttp.OkHttpClient(client);
  }
}
