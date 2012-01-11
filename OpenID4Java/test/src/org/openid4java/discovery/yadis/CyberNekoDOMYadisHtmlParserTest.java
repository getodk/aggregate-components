/*
 * Copyright 2006-2008 Sxip Identity Corporation
 */

package org.openid4java.discovery.yadis;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Sutra Zhou
 * 
 */
public class CyberNekoDOMYadisHtmlParserTest {
  private CyberNekoDOMYadisHtmlParser parser;

  @Before
  public void setUp() throws Exception {
    parser = new CyberNekoDOMYadisHtmlParser();
  }

  /**
   * Test method for
   * {@link org.openid4java.discovery.yadis.CyberNekoDOMYadisHtmlParser#getHtmlMeta(java.lang.String)}
   * .
   * 
   * @throws IOException
   * @throws YadisException
   */
  @Test
  public final void testGetHtmlMetaIssue83() throws IOException, YadisException {
    String htmlData = getResourceAsString("issue83.html");
    String s = parser.getHtmlMeta(htmlData);
    assertEquals("http://edevil.livejournal.com/data/yadis", s);
  }

  /**
   * Read the resource as string.
   * 
   * @param name
   *          the resource name
   * @return a string
   * @throws IOException
   *           if an I/O error occurs
   */
  private String getResourceAsString(String name) throws IOException {
    InputStream inputStream = CyberNekoDOMYadisHtmlParserTest.class.getResourceAsStream(name);
    try {
      return IOUtils.toString(inputStream);
    } finally {
      inputStream.close();
    }
  }
}
