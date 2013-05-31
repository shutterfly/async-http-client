package com.ning.http.client;

import org.testng.Assert;
import org.testng.annotations.Test;

public class CookieTest {

  @Test(groups = "fast")
  public void testUnquotedCookieValue() {

    String domain = "ning.com";
    String name = "BIGipServerAPP-A-LR";
    String value = "xAWH1b3RKrqo8AE/EbYaKBLdwZe2p5NKjJHPlQZ48BgFF6/CR8lKFTQv0TEhVIpxLL3b1Wd3dpMH";
    String path = "/";
    Cookie cookie = new Cookie(domain, name, value, path, -1, false);
    Assert.assertEquals(cookie.getValue(), value);
    Assert.assertEquals(cookie.getDomain(), domain);
    Assert.assertEquals(cookie.getPath(), path);
  }

}
