/*
 * Copyright 2006-2008 Sxip Identity Corporation
 */

package org.openid4java.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;
import org.openid4java.discovery.yadis.YadisResolver;
import org.openid4java.util.DefaultHttpClientFactory;
import org.openid4java.util.HttpClientFactory;
import org.openid4java.util.HttpFetcherFactory;

/**
 * @author Marius Scurtescu, Johnny Bufu
 */
public class RealmVerifierTest {
  private static final String TEST_DATA_FILE = "RealmTestData.xml";
  private static final Map<String, Integer> _resultCodes = new HashMap<String, Integer>();

  private String _testDataPath;

  static {
    _resultCodes.put("OK", new Integer(RealmVerifier.OK));
    _resultCodes.put("DENIED_REALM", new Integer(RealmVerifier.DENIED_REALM));
    _resultCodes.put("MALFORMED_REALM", new Integer(RealmVerifier.MALFORMED_REALM));
    _resultCodes.put("MALFORMED_RETURN_TO_URL", new Integer(RealmVerifier.MALFORMED_RETURN_TO_URL));
    _resultCodes.put("FRAGMENT_NOT_ALLOWED", new Integer(RealmVerifier.FRAGMENT_NOT_ALLOWED));
    _resultCodes.put("PROTOCOL_MISMATCH", new Integer(RealmVerifier.PROTOCOL_MISMATCH));
    _resultCodes.put("PORT_MISMATCH", new Integer(RealmVerifier.PORT_MISMATCH));
    _resultCodes.put("PATH_MISMATCH", new Integer(RealmVerifier.PATH_MISMATCH));
    _resultCodes.put("DOMAIN_MISMATCH", new Integer(RealmVerifier.DOMAIN_MISMATCH));
  }

  private RealmVerifier _realmVerifier;

  @Before
  public void setUp() throws Exception {

    _testDataPath = System.getProperty("TEST_DATA");

    if (_testDataPath == null)
      throw new ServletException("TEST_DATA path not initialized");

    HttpClientFactory clientFactory = new DefaultHttpClientFactory();
    _realmVerifier = new RealmVerifier(false, new YadisResolver(new HttpFetcherFactory(
        clientFactory)));
  }

  @Test
  public void testXmlFile() throws IOException, JDOMException {
    InputStream in = new BufferedInputStream(new FileInputStream(_testDataPath + "/server/"
        + TEST_DATA_FILE));

    assertNotNull("XML data file could not be loaded: " + TEST_DATA_FILE, in);

    SAXBuilder saxBuilder = new SAXBuilder();
    Document document = saxBuilder.build(in);
    Element testSuite = document.getRootElement();
    @SuppressWarnings("unchecked")
    List<Element> tests = testSuite.getChildren("test");
    for (int i = 0; i < tests.size(); i++) {
      Element test = (Element) tests.get(i);

      String result = test.getAttributeValue("result");
      String realm = test.getAttributeValue("realm");
      String returnTo = test.getAttributeValue("returnTo");
      String message = test.getAttributeValue("message");

      Integer resultCode = (Integer) _resultCodes.get(result);

      if (message == null)
        assertEquals(resultCode.intValue(), _realmVerifier.match(realm, returnTo));
      else
        assertEquals(message, resultCode.intValue(), _realmVerifier.match(realm, returnTo));
    }
  }
}
