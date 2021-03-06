package com.github.mgljava.loginterceptor.controller;

import com.github.mgljava.loginterceptor.client.MockClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HelloController {

  private final MockClient mockClient;

  @GetMapping("/index")
  public String index(@RequestParam("id") Integer id, @RequestHeader("auth") String auth) {
    mockClient.getUser();
    return "OK";
  }
}
