package com.github.mgljava.loginterceptor.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Url {

  // 拦截地址
  private String url;

  // 拦截状态码
  private String[] status;

  // Request
  private LogRequest request;

  // Response
  private LogResponse response;

}
