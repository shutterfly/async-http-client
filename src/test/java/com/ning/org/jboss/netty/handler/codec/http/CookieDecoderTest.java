/*
 * Copyright (c) 2010-2012 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.ning.org.jboss.netty.handler.codec.http;

import java.util.Set;

import org.jboss.netty.handler.codec.http.DefaultCookie;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.ning.http.client.Cookie;

public class CookieDecoderTest {
    
    @Test(groups = "fast")
    public void testDecodeUnquoted() {
      Cookie first = decodeOneCookie("foo=value; domain=/; path=/");
        Assert.assertEquals(first.getValue(), "value");
        Assert.assertEquals(first.getDomain(), "/");
        Assert.assertEquals(first.getPath(), "/");
    }

    @Test(groups = "fast")
    public void testDecodeNontrivialUnquotedValues() {
        String value = "xAWH1b3RKrqo8AE/EbYaKBLdwZe2p5NKjJHPlQZ48BgFF6/CR8lKFTQv0TEhVIpxLL3b1Wd3dpMH";
      Cookie first = decodeOneCookie("foo=" + value + "; domain=/; path=/");
        Assert.assertEquals(first.getValue(), value);
        Assert.assertEquals(first.getDomain(), "/");
        Assert.assertEquals(first.getPath(), "/");
    }

    @Test(groups = "fast")
    public void testDecodeQuoted() {
      Cookie first = decodeOneCookie("ALPHA=\"VALUE1\"; Domain=docs.foo.com; Path=/accounts; Expires=Wed, 13-Jan-2021 22:23:01 GMT; Secure; HttpOnly");
        Assert.assertEquals(first.getValue(), "VALUE1");
    }

    @Test(groups = "fast")
    public void testDecodeQuotedContainingEscapedQuote() {
      Cookie first = decodeOneCookie("ALPHA=\"VALUE1\\\"\"; Domain=docs.foo.com; Path=/accounts; Expires=Wed, 13-Jan-2021 22:23:01 GMT; Secure; HttpOnly");
        Assert.assertEquals(first.getValue(), "VALUE1\"");
    }

  @Test(groups = "fast")
  public void testRoundtrippingBigIPCookies() {
    String[] expectedValues = new String[]{
        "WRNWIV2HzJ+EfcQcooRUavrGE5YMwetfBEN3wjvFdFr5Fz0wwaUe0Qoi9jf7EQvWnO1WIt3G6uOqwkU="
        , "jKhFCnD1S4oJJpkcooRUavrGE5YMwaObL7dZrjm7kb5MeKsk12haway3pK6TvFatsEatiegIROcwLQ=="
        , "haA24RDHBZKrZmocooRUavrGE5YMwTRJCC1LDIKC80WM0EpMA4h0nZjrgppHAWMeYiwNwBJcBCrqCvE="
        , "GHeO+TrZy53eHoocooRUavrGE5YMwT350zcsPRmqHQ+eS40/orx66Zp2D5SThyAhZ9ycd2a94uSAdjE="
        , "42nSkUcyvjWjgHDvGBi+wqMsPlQd8Qy5JvPfjPp01rjYoOu6dFDoWaP6kzK5R6TlP6NvMep5UjPT2XM="
        , "ISmVAfSbreVYuDPvGBi+wqMsPlQd8VcKzbdXs/X5bNvpumFqJFovxNX9bfOHFIG/QmaAJikiISaYbD4="
        , "6Zkeyygyrw+vS1fvGBi+wqMsPlQd8R23Gr3w51uxNWYd6kbznxueHG8N/1Vjh2UzDlRiia5Uf3UJItE="
        , "tXd5GCDAxBHwz4ZcPwwnvYzefiV4HvY8Zx3PrRhiea2PrgsHG5nUEE/oqKpAEzJ3geKsPbvkZ9Jhdg=="
        , "bf5z2DapQ1iT+gIMeTYoy0WMzP3jtgya9wdZeOomV40GCqcBnozFavRzi0QVtWHR7Q8ieGdDTG0ngw=="
    };

    for (String expectedValue : expectedValues) {
      String cookieName = "BIGipServerSome.POOL";
      String originalNameAndValue = cookieName + "=" + expectedValue;
      String cookieStr = originalNameAndValue + "; path=/";
      Set<Cookie> cookies = CookieDecoder.decode(cookieStr);
      Assert.assertEquals(1, cookies.size());

      Cookie c = cookies.iterator().next();
      Assert.assertEquals(cookieName, c.getName());
      Assert.assertEquals(expectedValue, c.getValue());
      Assert.assertEquals("/", c.getPath());

      CookieEncoder clientEncoder = makeCookieEncoderForClient();
      DefaultCookie cookieDTO = new DefaultCookie(c.getName(), c.getValue());
      cookieDTO.setPath(c.getPath());
      clientEncoder.addCookie(cookieDTO);
      String reEncodedCookieStr = clientEncoder.encode();

      Assert.assertTrue(reEncodedCookieStr.contains(originalNameAndValue),
          "expected:\n" + reEncodedCookieStr + "\nto contain:\n" + originalNameAndValue);

      Set<Cookie> roundTrippedCookies = CookieDecoder.decode(reEncodedCookieStr);
      Assert.assertEquals(1, roundTrippedCookies.size());
      Cookie roundTrippedCookie = roundTrippedCookies.iterator().next();
      Assert.assertEquals(cookieName, roundTrippedCookie.getName());
      Assert.assertEquals(expectedValue, roundTrippedCookie.getValue());
      Assert.assertNull(roundTrippedCookie.getPath(),
          "a path=/ will be 'lost' during roundtripping because rfc6265, section 5.2.4 says '/' should be treated as the default");
    }

  }

  private CookieEncoder makeCookieEncoderForClient() {
    return new CookieEncoder();
  }

  @Test
  public void testCookiesHandledLikeFirefox20_JSESSIONID(){
    //JSESSIONID=9C827DD791A2E7E8875A0881A05A7DDB; Path=/; HttpOnly
    String setCookieStr = "JSESSIONID=9C827DD791A2E7E8875A0881A05A7DDB; Path=/; HttpOnly";
    Cookie cookie = decodeOneCookie(setCookieStr);
    Assert.assertEquals(cookie.getName(), "JSESSIONID");
    Assert.assertEquals(cookie.getValue(), "9C827DD791A2E7E8875A0881A05A7DDB");
    Assert.assertNull(cookie.getDomain());
    Assert.assertEquals(cookie.getPath(), "/");

    String reEncodedCookieStr = encode(cookie);

    Assert.assertEquals(reEncodedCookieStr, "JSESSIONID=9C827DD791A2E7E8875A0881A05A7DDB");

  }

  @Test
  public void testCookiesHandledLikeFirefox20_BIGipServer(){
    //BIGipServerAPP1-A-LR.FOXTROT=FfWZLexNlW5LuwV8kaCXrO6b44/ZiJV05ZNVophFlDa+b0zvj22WJIHSb+w7Dj5JxiivbJksSB1h0g==; path=/
    String setCookieStr = "BIGipServerAPP1-A-LR.FOXTROT=FfWZLexNlW5LuwV8kaCXrO6b44/ZiJV05ZNVophFlDa+b0zvj22WJIHSb+w7Dj5JxiivbJksSB1h0g==; path=/";
    Cookie cookie = decodeOneCookie(setCookieStr);
    Assert.assertEquals(cookie.getName(), "BIGipServerAPP1-A-LR.FOXTROT");
    Assert.assertEquals(cookie.getValue(), "FfWZLexNlW5LuwV8kaCXrO6b44/ZiJV05ZNVophFlDa+b0zvj22WJIHSb+w7Dj5JxiivbJksSB1h0g==");
    Assert.assertNull(cookie.getDomain());
    Assert.assertEquals(cookie.getPath(), "/");

    String reEncodedCookieStr = encode(cookie);

    Assert.assertEquals(reEncodedCookieStr, "BIGipServerAPP1-A-LR.FOXTROT=FfWZLexNlW5LuwV8kaCXrO6b44/ZiJV05ZNVophFlDa+b0zvj22WJIHSb+w7Dj5JxiivbJksSB1h0g==");
  }

  private String encode(Cookie cookie) {
    DefaultCookie cookieDTO = new DefaultCookie(cookie.getName(), cookie.getValue());
    cookieDTO.setPath(cookie.getPath());
    cookieDTO.setDomain(cookie.getDomain());
    CookieEncoder encoder = new CookieEncoder();
    encoder.addCookie(cookieDTO);
    return encoder.encode();
  }

  private Cookie decodeOneCookie(String setCookieStr) {
    Set<Cookie> cookies = CookieDecoder.decode(setCookieStr);
    Assert.assertEquals(cookies.size(), 1);

    return cookies.iterator().next();
  }

}