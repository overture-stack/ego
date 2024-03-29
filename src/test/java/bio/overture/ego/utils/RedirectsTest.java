/*
 * Copyright (c) 2019. The Ontario Institute for Cancer Research. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package bio.overture.ego.utils;

import static bio.overture.ego.utils.Redirects.*;
import static org.junit.Assert.*;

import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.exceptions.UnauthorizedClientException;
import lombok.val;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RedirectsTest {

  @Rule public ExpectedException exceptionRule = ExpectedException.none();

  @Test
  public void testNullRedirect() {
    val app = appWithUrls("https://example.com");

    val redirect = getRedirectUri(app, null);
    val errorRedirect = getErrorRedirectUri(app, null);

    assertEquals("https://example.com", redirect);
    assertEquals("https://example.com", errorRedirect);
  }

  @Test
  public void testEmptyRedirect() {
    val app = appWithUrls("https://example.com");

    val redirect = getRedirectUri(app, "");
    val errorRedirect = getRedirectUri(app, "");

    assertEquals("https://example.com", redirect);
    assertEquals("https://example.com", errorRedirect);
  }

  @Test
  public void testBlankRedirect() {
    val app = appWithUrls("https://example.com");

    val redirect = getRedirectUri(app, "          ");
    val errorRedirect = getRedirectUri(app, "          ");

    assertEquals("https://example.com", redirect);
    assertEquals("https://example.com", errorRedirect);
  }

  @Test
  public void testSameDomainNoPath() {
    val app = appWithUrls("https://example.com");

    val redirect = getRedirectUri(app, "https://example.com");
    val errorRedirect = getRedirectUri(app, "https://example.com");

    assertEquals("https://example.com", redirect);
    assertEquals("https://example.com", errorRedirect);
  }

  @Test
  public void testSameDomainNoPathWithParams() {
    val app = appWithUrls("https://example.com");

    val redirect = getRedirectUri(app, "https://example.com?foo=1&bar=2");
    val errorRedirect = getRedirectUri(app, "https://example.com?foo=1&bar=2");

    assertEquals("https://example.com?foo=1&bar=2", redirect);
    assertEquals("https://example.com?foo=1&bar=2", errorRedirect);
  }

  @Test
  public void testSameDomainWithPath() {
    val app = appWithUrls("https://example.com");

    val redirect = getRedirectUri(app, "https://example.com/foobar");
    val errorRedirect = getRedirectUri(app, "https://example.com/foobar");

    assertEquals("https://example.com/foobar", redirect);
    assertEquals("https://example.com/foobar", errorRedirect);
  }

  @Test
  public void testSameDomainWithPathAndParams() {
    val app = appWithUrls("https://example.com");

    val redirect = getRedirectUri(app, "https://example.com/foobar?over=9000");
    val errorRedirect = getRedirectUri(app, "https://example.com/foobar?over=9000");

    assertEquals("https://example.com/foobar?over=9000", redirect);
    assertEquals("https://example.com/foobar?over=9000", errorRedirect);
  }

  @Test
  public void testFirstFromList() {
    val app = appWithUrls("https://example.com:5555,https://other.example.com,https://google.ca");

    val redirect = getRedirectUri(app, "");
    val errorRedirect = getRedirectUri(app, "");

    assertEquals("https://example.com:5555", redirect);
    assertEquals("https://example.com:5555", errorRedirect);
  }

  @Test
  public void testExactFromList() {
    val app = appWithUrls("https://example.com:5555,https://other.example.com,https://google.ca");

    val redirect = getRedirectUri(app, "https://other.example.com");
    val errorRedirect = getRedirectUri(app, "https://other.example.com");

    assertEquals("https://other.example.com", redirect);
    assertEquals("https://other.example.com", errorRedirect);
  }

  @Test
  public void testPathFromList() {
    val app = appWithUrls("https://example.com:5555,https://other.example.com,https://google.ca");

    val redirect = getRedirectUri(app, "https://other.example.com/super/secret/path");
    val errorRedirect = getRedirectUri(app, "https://other.example.com/super/secret/path");

    assertEquals("https://other.example.com/super/secret/path", redirect);
    assertEquals("https://other.example.com/super/secret/path", errorRedirect);
  }

  @Test
  public void testPathFromListWithParam() {
    val app = appWithUrls("https://example.com:5555,https://other.example.com,https://google.ca");

    val redirect = getRedirectUri(app, "https://other.example.com/super/secret/path?maximum=power");
    val errorRedirect =
        getRedirectUri(app, "https://other.example.com/super/secret/path?maximum=power");

    assertEquals("https://other.example.com/super/secret/path?maximum=power", redirect);
    assertEquals("https://other.example.com/super/secret/path?maximum=power", errorRedirect);
  }

  @Test
  public void testPathFromListWithParams() {
    val app = appWithUrls("https://example.com:5555,https://other.example.com,https://google.ca");

    val redirect = getRedirectUri(app, "https://other.example.com/super/secret/path?foo=1&bar=2");
    val errorRedirect =
        getRedirectUri(app, "https://other.example.com/super/secret/path?foo=1&bar=2");

    assertEquals("https://other.example.com/super/secret/path?foo=1&bar=2", redirect);
    assertEquals("https://other.example.com/super/secret/path?foo=1&bar=2", errorRedirect);
  }

  @Test
  public void testWrongPort() {
    val app = appWithUrls("https://example.com:5555,https://other.example.com,https://google.ca");
    exceptionRule.expect(UnauthorizedClientException.class);
    getRedirectUri(app, "https://example.com:8080");

    exceptionRule.expect(UnauthorizedClientException.class);
    getErrorRedirectUri(app, "https://example.com:8080");
  }

  @Test
  public void testWrongScheme() {
    val app = appWithUrls("https://example.com:5555,https://other.example.com,https://google.ca");
    exceptionRule.expect(UnauthorizedClientException.class);
    getRedirectUri(app, "http://example.com:8080");

    exceptionRule.expect(UnauthorizedClientException.class);
    getErrorRedirectUri(app, "http://example.com:8080");
  }

  private static Application appWithUrls(String urls) {
    val app = new Application();
    app.setRedirectUri(urls);
    app.setErrorRedirectUri(urls);
    return app;
  }
}
