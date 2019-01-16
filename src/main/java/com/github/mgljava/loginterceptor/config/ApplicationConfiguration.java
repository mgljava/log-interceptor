package com.github.mgljava.loginterceptor.config;

import com.github.mgljava.loginterceptor.domain.Url;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "log")
public class ApplicationConfiguration {

  private List<Url> urls;
}
