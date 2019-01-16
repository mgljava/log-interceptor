package com.github.mgljava.loginterceptor.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "Local", url = "http://localhost:8080")
public interface MockClient {

  @GetMapping("/user/get")
  String getUser();
}
