package com.ning.org.jboss.netty.handler.codec.http;

/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.DefaultCookie;
import org.jboss.netty.handler.codec.http.HttpConstants;

import java.util.Set;
import java.util.TreeSet;


/**
 * Encodes {@link org.jboss.netty.handler.codec.http.Cookie}s into an HTTP header value.  This encoder can encode
 * the HTTP cookie version 0, 1, and 2.
 * <p/>
 * This encoder is stateful.  It maintains an internal data structure that
 * holds the {@link org.jboss.netty.handler.codec.http.Cookie}s added by the {@link #addCookie(String, String)}
 * method.  Once {@link #encode()} is called, all added {@link org.jboss.netty.handler.codec.http.Cookie}s are
 * encoded into an HTTP header value and all {@link org.jboss.netty.handler.codec.http.Cookie}s in the internal
 * data structure are removed so that the encoder can start over.
 * <pre>
 * // Client-side example
 * {@link org.jboss.netty.handler.codec.http.HttpRequest} req = ...;
 * {@link CookieEncoder} encoder = new {@link CookieEncoder}();
 * encoder.addCookie("JSESSIONID", "1234");
 * res.setHeader("Cookie", encoder.encode());
 * </pre>
 *
 * @see org.jboss.netty.handler.codec.http.CookieDecoder
 */
public class CookieEncoder {

  private final Set<Cookie> cookies = new TreeSet<Cookie>();

  /**
   * Creates a new client-side-only CookieEncoder.
   */
  public CookieEncoder() {
  }

  /**
   * Adds a new {@link Cookie} created with the specified name and value to
   * this encoder.
   */
  public void addCookie(String name, String value) {
    cookies.add(new DefaultCookie(name, value));
  }

  /**
   * Adds the specified {@link Cookie} to this encoder.
   */
  public void addCookie(Cookie cookie) {
    cookies.add(cookie);
  }

  /**
   * Encodes the {@link Cookie}s which were added by {@link #addCookie(Cookie)}
   * so far into an HTTP header value.  If no {@link Cookie}s were added,
   * an empty string is returned.
   * <p/>
   * <strong>Be aware that calling this method will clear the content of the {@link CookieEncoder}</strong>
   */
  public String encode() {
    String answer = encodeClientSide();
    cookies.clear();
    return answer;
  }

  private String encodeClientSide() {
    StringBuilder sb = new StringBuilder();

    for (Cookie cookie : cookies) {
      if (cookie.getVersion() >= 1) {
        add(sb, '$' + CookieHeaderNames.VERSION, 1);
      }

      addSimple(sb, cookie.getName(), cookie.getValue());

      if (cookie.getPath() != null) {
        add(sb, '$' + CookieHeaderNames.PATH.toLowerCase(), cookie.getPath());
      }

      if (cookie.getDomain() != null) {
        add(sb, '$' + CookieHeaderNames.DOMAIN, cookie.getDomain());
      }

      if (cookie.getVersion() >= 1) {
        if (!cookie.getPorts().isEmpty()) {
          sb.append('$');
          sb.append(CookieHeaderNames.PORT);
          sb.append((char) HttpConstants.EQUALS);
          sb.append((char) HttpConstants.DOUBLE_QUOTE);
          for (int port : cookie.getPorts()) {
            sb.append(port);
            sb.append((char) HttpConstants.COMMA);
          }
          sb.setCharAt(sb.length() - 1, (char) HttpConstants.DOUBLE_QUOTE);
          sb.append((char) HttpConstants.SEMICOLON);
          sb.append((char) HttpConstants.SP);
        }
      }
    }

    if (sb.length() > 0) {
      sb.setLength(sb.length() - 2);
    }
    return sb.toString();
  }

  private static void addSimple(StringBuilder sb, String name, String val) {
    if (val == null) {
      addQuoted(sb, name, "");
      return;
    }

    for (int i = 0; i < val.length(); i++) {
      char c = val.charAt(i);
      switch (c) {
        case '\t':
        case ' ':
        case '"':
        case '(':
        case ')':
        case ',':
          /*case '/':*/
        case ':':
        case ';':
        case '<':
          /*case '=':*/
        case '>':
        case '?':
        case '@':
        case '[':
        case '\\':
        case ']':
        case '{':
        case '}':
          addQuoted(sb, name, val);
          return;
      }
    }

    addUnquoted(sb, name, val);
  }

  private static void add(StringBuilder sb, String name, String val) {
    if (val == null) {
      addQuoted(sb, name, "");
      return;
    }

    for (int i = 0; i < val.length(); i++) {
      char c = val.charAt(i);
      switch (c) {
        case '\t':
        case ' ':
        case '"':
        case '(':
        case ')':
        case ',':
        case '/':
        case ':':
        case ';':
        case '<':
        case '=':
        case '>':
        case '?':
        case '@':
        case '[':
        case '\\':
        case ']':
        case '{':
        case '}':
          addQuoted(sb, name, val);
          return;
      }
    }

    addUnquoted(sb, name, val);
  }

  private static void addUnquoted(StringBuilder sb, String name, String val) {
    sb.append(name);
    sb.append((char) HttpConstants.EQUALS);
    sb.append(val);
    sb.append((char) HttpConstants.SEMICOLON);
    sb.append((char) HttpConstants.SP);
  }

  private static void addQuoted(StringBuilder sb, String name, String val) {
    if (val == null) {
      val = "";
    }

    sb.append(name);
    sb.append((char) HttpConstants.EQUALS);
    sb.append((char) HttpConstants.DOUBLE_QUOTE);
    sb.append(val.replace("\\", "\\\\").replace("\"", "\\\""));
    sb.append((char) HttpConstants.DOUBLE_QUOTE);
    sb.append((char) HttpConstants.SEMICOLON);
    sb.append((char) HttpConstants.SP);
  }

  private static void add(StringBuilder sb, String name, int val) {
    sb.append(name);
    sb.append((char) HttpConstants.EQUALS);
    sb.append(val);
    sb.append((char) HttpConstants.SEMICOLON);
    sb.append((char) HttpConstants.SP);
  }
}
