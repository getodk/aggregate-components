/*
 * Copyright 2006-2008 Sxip Identity Corporation
 */

package org.openid4java.discovery.html;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.openid4java.discovery.DiscoveryException;

/**
 * @author Sutra Zhou
 * 
 */
public class CyberNekoDOMHtmlParserTest {
  private CyberNekoDOMHtmlParser parser;

  @Before
  public void setUp() throws Exception {
    parser = new CyberNekoDOMHtmlParser();
  }

  /**
   * Test method for
   * {@link org.openid4java.discovery.html.CyberNekoDOMHtmlParser#parseHtml(java.lang.String, org.openid4java.discovery.html.HtmlResult)}
   * .
   * 
   * @throws IOException
   * @throws DiscoveryException
   */
  @Test
  public void testParseHtml() throws IOException, DiscoveryException {
    String htmlData = IOUtils.toString(this.getClass().getResourceAsStream("identityPage.html"));
    HtmlResult result = new HtmlResult();
    parser.parseHtml(htmlData, result);
    assertEquals("http://www.example.com:8080/openidserver/users/myusername", result.getDelegate1());
    System.out.println(result.getOP1Endpoint());
    assertEquals("http://www.example.com:8080/openidserver/openid.server", result.getOP1Endpoint()
        .toExternalForm());
  }

  /**
   * Test method for
   * {@link org.openid4java.discovery.html.CyberNekoDOMHtmlParser#parseHtml(java.lang.String, org.openid4java.discovery.html.HtmlResult)}
   * .
   * 
   * @throws IOException
   * @throws DiscoveryException
   */
  @Test
  public void testParseHtmlWithXmlNamespace() throws IOException, DiscoveryException {
    String htmlData = IOUtils.toString(this.getClass().getResourceAsStream(
        "identityPage-with-xml-namespace.html"));
    HtmlResult result = new HtmlResult();
    parser.parseHtml(htmlData, result);
    assertEquals("http://www.example.com:8080/openidserver/users/myusername", result.getDelegate1());
    assertEquals("http://www.example.com:8080/openidserver/openid.server", result.getOP1Endpoint()
        .toExternalForm());
  }

}
