package com.github.mgljava.loginterceptor.domain;

import java.util.List;
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
  private List<LogRequest> request;

  // Response
  private List<LogResponse> response;

}
