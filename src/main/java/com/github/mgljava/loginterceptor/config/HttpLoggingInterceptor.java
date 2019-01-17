package com.github.mgljava.loginterceptor.config;

import com.github.mgljava.loginterceptor.domain.LogResponse;
import com.github.mgljava.loginterceptor.domain.Url;
import java.io.EOFException;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import okhttp3.Connection;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;
import okhttp3.internal.platform.Platform;
import okio.Buffer;
import okio.BufferedSource;
import okio.GzipSource;
import org.springframework.beans.factory.annotation.Autowired;

public class HttpLoggingInterceptor implements Interceptor {

  @Autowired
  private ApplicationConfiguration applicationConfiguration;
  private static final Charset UTF8 = Charset.forName("UTF-8");
  private final HttpLoggingInterceptor.Logger logger;
  private volatile Set<String> headersToRedact;
  private volatile HttpLoggingInterceptor.Level level;

  public HttpLoggingInterceptor() {
    this(HttpLoggingInterceptor.Logger.DEFAULT);
  }

  public HttpLoggingInterceptor(HttpLoggingInterceptor.Logger logger) {
    this.headersToRedact = Collections.emptySet();
    this.level = HttpLoggingInterceptor.Level.NONE;
    this.logger = logger;
  }

  public void redactHeader(String name) {
    Set<String> newHeadersToRedact = new TreeSet(String.CASE_INSENSITIVE_ORDER);
    newHeadersToRedact.addAll(this.headersToRedact);
    newHeadersToRedact.add(name);
    this.headersToRedact = newHeadersToRedact;
  }

  public HttpLoggingInterceptor setLevel(HttpLoggingInterceptor.Level level) {
    if (level == null) {
      throw new NullPointerException("level == null. Use Level.NONE instead.");
    } else {
      this.level = level;
      return this;
    }
  }

  public HttpLoggingInterceptor.Level getLevel() {
    return this.level;
  }

  // 是否包含拦截的url
  public boolean matchUrl(String url) {
    return applicationConfiguration.getUrls().stream()
        .anyMatch(item -> item.getUrl().equals(url));
  }

  public Optional<Url> getCurrentConfig(String path) {
    return applicationConfiguration.getUrls().stream()
        .filter(item -> item.getUrl().equals(path)).findFirst();
  }

  public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    final URI uri = request.url().uri();
    String uriPath = uri.getPath();
    if (matchUrl(uriPath)) {
      final Optional<Url> currentConfig = getCurrentConfig(uriPath);
      if (currentConfig.isPresent()) {

        RequestBody requestBody = request.body();
        boolean hasRequestBody = requestBody != null;
        Connection connection = chain.connection();
        String requestStartMessage =
            "--> " + request.method() + ' ' + request.url() + (connection != null ? " " + connection
                .protocol() : "");
        this.logger.log(requestStartMessage);
        final Url url = currentConfig.get();
        final boolean logRequestHeader = url.getRequest().isHeader();
        final boolean logRequestBody = url.getRequest().isRequestBody();
        if (logRequestHeader) {
          Headers headers = request.headers();
          int i = 0;

          for (int count = headers.size(); i < count; ++i) {
            String name = headers.name(i);
            if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length"
                .equalsIgnoreCase(name)) {
              this.logHeader(headers, i);
            }
          }

          if (!logRequestBody || !hasRequestBody) {
            logger.log("--> END " + request.method());
          } else if (bodyHasUnknownEncoding(request.headers())) {
            logger.log("--> END " + request.method() + " (encoded body omitted)");
          } else {
            Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);

            Charset charset = UTF8;
            MediaType contentType = requestBody.contentType();
            if (contentType != null) {
              charset = contentType.charset(UTF8);
            }

            logger.log("");
            if (isPlaintext(buffer)) {
              logger.log(buffer.readString(charset));
              logger.log("--> END " + request.method()
                  + " (" + requestBody.contentLength() + "-byte body)");
            } else {
              logger.log("--> END " + request.method() + " (binary "
                  + requestBody.contentLength() + "-byte body omitted)");
            }
          }
        }
// -------------------- 响应  ----------------
        long startNs = System.nanoTime();
        Response response;
        try {
          response = chain.proceed(request);
        } catch (Exception var27) {
          this.logger.log("<-- HTTP FAILED: " + var27);
          throw var27;
        }
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
        ResponseBody responseBody = response.body();
        long contentLength = responseBody.contentLength();
        String bodySize = contentLength != -1L ? contentLength + "-byte" : "unknown-length";
        final LogResponse responseConfig = url.getResponse();
        final boolean logResponseHeader = responseConfig.isHeader();
        final boolean logResponseBody = responseConfig.isResponseBody();
        this.logger.log(
            "<-- " + response.code() + (response.message().isEmpty() ? ""
                : ' ' + response.message())
                + ' ' + response.request().url() + " (" + tookMs + "ms" + (!logResponseHeader ? ", "
                + bodySize + " body" : "") + ')');

        if (logResponseHeader) {
          Headers headers = response.headers();
          int i = 0;

          for (int count = headers.size(); i < count; ++i) {
            this.logHeader(headers, i);
          }

          if (!logResponseBody || !HttpHeaders.hasBody(response)) {
            logger.log("<-- END HTTP");
          } else if (bodyHasUnknownEncoding(response.headers())) {
            logger.log("<-- END HTTP (encoded body omitted)");
          } else {
            BufferedSource source = responseBody.source();
            source.request(Long.MAX_VALUE); // Buffer the entire body.
            Buffer buffer = source.buffer();

            Long gzippedLength = null;
            if ("gzip".equalsIgnoreCase(headers.get("Content-Encoding"))) {
              gzippedLength = buffer.size();
              GzipSource gzippedResponseBody = null;
              try {
                gzippedResponseBody = new GzipSource(buffer.clone());
                buffer = new Buffer();
                buffer.writeAll(gzippedResponseBody);
              } finally {
                if (gzippedResponseBody != null) {
                  gzippedResponseBody.close();
                }
              }
            }

            Charset charset = UTF8;
            MediaType contentType = responseBody.contentType();
            if (contentType != null) {
              charset = contentType.charset(UTF8);
            }

            if (!isPlaintext(buffer)) {
              logger.log("");
              logger.log("<-- END HTTP (binary " + buffer.size() + "-byte body omitted)");
              return response;
            }

            if (contentLength != 0) {
              logger.log("");
              logger.log(buffer.clone().readString(charset).replaceAll("\n", ""));
            }

            if (gzippedLength != null) {
              logger.log("<-- END HTTP (" + buffer.size() + "-byte, "
                  + gzippedLength + "-gzipped-byte body)");
            } else {
              logger.log("<-- END HTTP (" + buffer.size() + "-byte body)");
            }
          }
        }
      }
    }
    return chain.proceed(request);
  }

  private void logHeader(Headers headers, int i) {
    String value = this.headersToRedact.contains(headers.name(i)) ? "██" : headers.value(i);
    this.logger.log(headers.name(i) + ": " + value);
  }

  static boolean isPlaintext(Buffer buffer) {
    try {
      Buffer prefix = new Buffer();
      long byteCount = buffer.size() < 64L ? buffer.size() : 64L;
      buffer.copyTo(prefix, 0L, byteCount);

      for (int i = 0; i < 16 && !prefix.exhausted(); ++i) {
        int codePoint = prefix.readUtf8CodePoint();
        if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
          return false;
        }
      }

      return true;
    } catch (EOFException var6) {
      return false;
    }
  }

  private static boolean bodyHasUnknownEncoding(Headers headers) {
    String contentEncoding = headers.get("Content-Encoding");
    return contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity")
        && !contentEncoding.equalsIgnoreCase("gzip");
  }

  public interface Logger {

    HttpLoggingInterceptor.Logger DEFAULT = new HttpLoggingInterceptor.Logger() {
      public void log(String message) {
        Platform.get().log(4, message, (Throwable) null);
      }
    };

    void log(String var1);
  }

  public enum Level {
    NONE,
    BASIC,
    HEADERS,
    BODY;

    Level() {
    }
  }
}
