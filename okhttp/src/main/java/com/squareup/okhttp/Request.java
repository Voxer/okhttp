/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.okhttp;

import com.squareup.okhttp.internal.http.HttpMethod;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 * An HTTP request. Instances of this class are immutable if their {@link #body}
 * is null or itself immutable.
 */
public final class Request {
  private final HttpUrl url;
  private final InetAddress hostIP;
  private final String method;
  private final Headers headers;
  private final RequestBody body;
  private final Object tag;
  private final PushObserver pushObserver;
  private final long readTimeout;
  private final long writeTimeout;
  private final boolean dontRetry;

  // Ok, I violated the immutability to put this here...
  private Headers trailers;

  private volatile URL javaNetUrl; // Lazily initialized.
  private volatile URI javaNetUri; // Lazily initialized.
  private volatile CacheControl cacheControl; // Lazily initialized.

  private Request(Builder builder) {
    this.url = builder.url;
    this.hostIP = builder.hostIP;
    this.method = builder.method;
    this.headers = builder.headers.build();
    this.body = builder.body;
    this.tag = builder.tag != null ? builder.tag : this;
    this.pushObserver = builder.pushObserver;
    this.readTimeout = builder.readTimeout;
    this.writeTimeout = builder.writeTimeout;
    this.dontRetry = builder.dontRetry;
  }

  public HttpUrl httpUrl() {
    return url;
  }

  public URL url() {
    URL result = javaNetUrl;
    return result != null ? result : (javaNetUrl = url.url());
  }

  public URI uri() throws IOException {
    try {
      URI result = javaNetUri;
      return result != null ? result : (javaNetUri = url.uri());
    } catch (IllegalStateException e) {
      throw new IOException(e.getMessage());
    }
  }

  public String urlString() {
    return url.toString();
  }

  public InetAddress hostIP() {
    return hostIP;
  }

  public String method() {
    return method;
  }

  public Headers headers() {
    return headers;
  }

  public String header(String name) {
    return headers.get(name);
  }

  public List<String> headers(String name) {
    return headers.values(name);
  }

  public RequestBody body() {
    return body;
  }

  public Object tag() {
    return tag;
  }

  public PushObserver pushObserver() {
    return pushObserver;
  }

  public long readTimeout() {
    return readTimeout;
  }

  public long writeTimeout() {
    return writeTimeout;
  }

  public Builder newBuilder() {
    return new Builder(this);
  }

  public void setTrailers(Headers headers) {
    this.trailers = headers;
  }

  public Headers trailers() {
    return trailers;
  }

  public boolean dontRetry() {
    return dontRetry;
  }

  /**
   * Returns the cache control directives for this response. This is never null,
   * even if this response contains no {@code Cache-Control} header.
   */
  public CacheControl cacheControl() {
    CacheControl result = cacheControl;
    return result != null ? result : (cacheControl = CacheControl.parse(headers));
  }

  public boolean isHttps() {
    return url.isHttps();
  }

  @Override public String toString() {
    return "Request{method="
        + method
        + ", url="
        + url
        + ", tag="
        + (tag != this ? tag : null)
        + '}';
  }

  public static class Builder {
    private HttpUrl url;
    private InetAddress hostIP;
    private String method;
    private Headers.Builder headers;
    private RequestBody body;
    private Object tag;
    private PushObserver pushObserver = null;
    private long readTimeout;
    private long writeTimeout;
    private boolean dontRetry;

    public Builder() {
      this.method = "GET";
      this.headers = new Headers.Builder();
    }

    private Builder(Request request) {
      this.url = request.url;
      this.hostIP = request.hostIP;
      this.method = request.method;
      this.body = request.body;
      this.tag = request.tag;
      this.headers = request.headers.newBuilder();
      this.pushObserver = request.pushObserver;
      this.readTimeout = request.readTimeout;
      this.writeTimeout = request.writeTimeout;
      this.dontRetry = request.dontRetry;
    }

    public Builder url(HttpUrl url) {
      if (url == null) throw new IllegalArgumentException("url == null");
      this.url = url;
      return this;
    }

    public Builder url(String url) {
      if (url == null) throw new IllegalArgumentException("url == null");

      // Silently replace websocket URLs with HTTP URLs.
      if (url.regionMatches(true, 0, "ws:", 0, 3)) {
        url = "http:" + url.substring(3);
      } else if (url.regionMatches(true, 0, "wss:", 0, 4)) {
        url = "https:" + url.substring(4);
      }

      HttpUrl parsed = HttpUrl.parse(url);
      if (parsed == null) throw new IllegalArgumentException("unexpected url: " + url);
      return url(parsed);
    }

    public Builder url(URL url) {
      if (url == null) throw new IllegalArgumentException("url == null");
      HttpUrl parsed = HttpUrl.get(url);
      if (parsed == null) throw new IllegalArgumentException("unexpected url: " + url);
      return url(parsed);
    }

    public Builder hostIP(InetAddress hostIP) {
      this.hostIP = hostIP;
      return this;
    }

    /**
     * Sets the header named {@code name} to {@code value}. If this request
     * already has any headers with that name, they are all replaced.
     */
    public Builder header(String name, String value) {
      headers.set(name, value);
      return this;
    }

    /**
     * Adds a header with {@code name} and {@code value}. Prefer this method for
     * multiply-valued headers like "Cookie".
     */
    public Builder addHeader(String name, String value) {
      headers.add(name, value);
      return this;
    }

    public Builder removeHeader(String name) {
      headers.removeAll(name);
      return this;
    }

    /** Removes all headers on this builder and adds {@code headers}. */
    public Builder headers(Headers headers) {
      this.headers = headers.newBuilder();
      return this;
    }

    /**
     * Sets this request's {@code Cache-Control} header, replacing any cache
     * control headers already present. If {@code cacheControl} doesn't define
     * any directives, this clears this request's cache-control headers.
     */
    public Builder cacheControl(CacheControl cacheControl) {
      String value = cacheControl.toString();
      if (value.isEmpty()) return removeHeader("Cache-Control");
      return header("Cache-Control", value);
    }

    public Builder get() {
      return method("GET", null);
    }

    public Builder head() {
      return method("HEAD", null);
    }

    public Builder post(RequestBody body) {
      return method("POST", body);
    }

    public Builder delete(RequestBody body) {
      return method("DELETE", body);
    }

    public Builder delete() {
      return delete(RequestBody.create(null, new byte[0]));
    }

    public Builder put(RequestBody body) {
      return method("PUT", body);
    }

    public Builder patch(RequestBody body) {
      return method("PATCH", body);
    }

    public Builder method(String method, RequestBody body) {
      if (method == null || method.length() == 0) {
        throw new IllegalArgumentException("method == null || method.length() == 0");
      }
      if (body != null && !HttpMethod.permitsRequestBody(method)) {
        throw new IllegalArgumentException("method " + method + " must not have a request body.");
      }
      if (body == null && HttpMethod.requiresRequestBody(method)) {
        throw new IllegalArgumentException("method " + method + " must have a request body.");
      }
      this.method = method;
      this.body = body;
      return this;
    }

    /**
     * Attaches {@code tag} to the request. It can be used later to cancel the
     * request. If the tag is unspecified or null, the request is canceled by
     * using the request itself as the tag.
     */
    public Builder tag(Object tag) {
      this.tag = tag;
      return this;
    }

    public Builder pushObserver(PushObserver pushObserver) {
      this.pushObserver = pushObserver;
      return this;
    }

    public Builder readTimeout(long readTimeout) {
      this.readTimeout = readTimeout;
      return this;
    }

    public Builder writeTimeout(long writeTimeout) {
      this.writeTimeout = writeTimeout;
      return this;
    }

    public Builder dontRetry(boolean dontRetry) {
      this.dontRetry = dontRetry;
      return this;
    }

    public Request build() {
      if (url == null) throw new IllegalStateException("url == null");
      return new Request(this);
    }
  }
}
