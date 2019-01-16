package com.github.mgljava.loginterceptor.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LogResponse {

  private boolean header;
  private boolean responseBody;
}
