/*
 * Copyright 2006-2008 Sxip Identity Corporation
 */

package org.openid4java.samples;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.jwebunit.junit.WebTester;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.openid4java.consumer.SampleConsumer;
import org.openid4java.message.ParameterList;
import org.openid4java.server.SampleServer;
import org.openid4java.server.ServerException;

public class ConsumerAndProviderTest {

  static {
    System.getProperties().put("org.apache.commons.logging.Log",
        "org.apache.commons.logging.impl.SimpleLog");
    System.getProperties().put("org.apache.commons.logging.simplelog.defaultlog", "trace");
  }

  private static Server _server;
  private static String _baseUrl;

  @BeforeClass
  public static void setUpServer() throws Exception {
    int servletPort = Integer.parseInt(System.getProperty("SERVLET_PORT", "8989"));
    _server = new Server(servletPort);

    Context context = new Context(_server, "/", Context.SESSIONS);
    _baseUrl = "http://localhost:" + servletPort; // +
    // context.getContextPath();

    SampleConsumer consumer = new SampleConsumer(_baseUrl + "/loginCallback");
    context.addServlet(new ServletHolder(new LoginServlet(consumer)), "/login");
    context.addServlet(new ServletHolder(new LoginCallbackServlet(consumer)), "/loginCallback");

    context.addServlet(new ServletHolder(new UserInfoServlet()), "/user");

    SampleServer server = new SampleServer(_baseUrl + "/provider") {
      @SuppressWarnings({ "rawtypes", "unchecked" })
      protected List userInteraction(ParameterList request) throws ServerException {
        List back = new ArrayList();
        back.add("userSelectedClaimedId"); // userSelectedClaimedId
        back.add(Boolean.TRUE); // authenticatedAndApproved
        back.add("user@example.com"); // email
        return back;
      }
    };
    context.addServlet(new ServletHolder(new ProviderServlet(server)), "/provider");
  }

  WebTester wc = null;

  @Before
  public void setUp() throws Exception {
    _server.start();
    wc = new WebTester();
  }

  @After
  public void tearDown() throws Exception {
    wc.closeBrowser();
    _server.stop();
    _server.join();
  }

  @Test
  public void testCycleWithXrdsUser() throws Exception {
    HttpServletSupport.lastException = null;
    HttpServletSupport.count_ = 0;
    try {
      wc.setScriptingEnabled(false);
      wc.beginAt(_baseUrl + "/login");
      wc.setTextField("openid_identifier", _baseUrl + "/user");
      wc.submit();
      wc.clickLink("login");
      wc.assertTextPresent("success");
      wc.assertTextPresent("emailFromFetch:user@example.com");
      wc.assertTextPresent("emailFromSReg:user@example.com");
    } catch (Exception exc) {
      System.err.println(exc.toString());
      System.err.println("last page before exception :" + wc.getPageSource());
      if (HttpServletSupport.lastException != null) {
        throw HttpServletSupport.lastException;
      } else {
        throw exc;
      }
    }
  }

  @Test
  public void testCycleWithHtmlUser() throws Exception {
    HttpServletSupport.lastException = null;
    HttpServletSupport.count_ = 0;
    try {
      wc.setScriptingEnabled(false);
      wc.beginAt(_baseUrl + "/login");
      wc.setTextField("openid_identifier", _baseUrl + "/user?format=html");
      wc.submit();
      wc.clickLink("login");
      wc.assertTextPresent("success");
      wc.assertTextPresent("emailFromFetch:user@example.com");
      wc.assertTextPresent("emailFromSReg:user@example.com");
    } catch (Exception exc) {
      System.err.println("last page before exception :" + wc.getPageSource());
      if (HttpServletSupport.lastException != null) {
        throw HttpServletSupport.lastException;
      } else {
        throw exc;
      }
    }
  }
}
