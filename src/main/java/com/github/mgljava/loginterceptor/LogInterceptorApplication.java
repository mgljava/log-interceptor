package com.github.mgljava.loginterceptor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan({"com.github.mgljava.loginterceptor"})
@EnableFeignClients({"com.github.mgljava.loginterceptor.client"})
@SpringBootApplication
public class LogInterceptorApplication {

  public static void main(String[] args) {
    SpringApplication.run(LogInterceptorApplication.class, args);
  }

}

