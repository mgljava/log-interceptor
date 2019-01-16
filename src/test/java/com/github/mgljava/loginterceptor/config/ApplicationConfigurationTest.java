package com.github.mgljava.loginterceptor.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ApplicationConfigurationTest {

  @Autowired
  ApplicationConfiguration applicationConfiguration;

  @Test
  public void test() {
    applicationConfiguration.getUrls().stream()
        .forEach(url -> System.out.println(url.getStatus()[0]));
  }
}