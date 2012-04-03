/*
 * Copyright (C) 2012 University of Washington.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.dwc.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opendatakit.dwc.server.GreetingServiceImpl;
import org.opendatakit.dwc.server.OAuthToken;

public class OAuthServlet extends ServletUtilBase {

  /**
   * Serial number for serialization
   */
  private static final long serialVersionUID = -2220857379519391127L;

  /**
   * URI from base
   */
  public static final String ADDR = "auth/auth";

  /**
   * Callback from external service acknowledging acceptable authentication
   * token.
   * 
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    // get parameters
    String uri = getParameter(req, "additionalTerm");

    // get authToken
    OAuthToken authToken = null;
    try {
      authToken = verifyGDataAuthorization(req, resp);
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
      return; // verifyGDataAuthroization function formats response
    }

    try {
      if (authToken != null) {
        GreetingServiceImpl.theAuthToken = authToken;
      }
    } catch (Exception e) {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      e.printStackTrace();
      return;
    }

    String redirectTo = GreetingServiceImpl.getServerURL(req,"DemoWebClient.html",true);
    redirectTo = redirectTo.replace("gwt.codesvr=127.0.0.1%3A" + GreetingServiceImpl.CLIENT_WEBSITE_CODESVR_PORT,
    								"gwt.codesvr=127.0.0.1:" + GreetingServiceImpl.CLIENT_WEBSITE_CODESVR_PORT);
    resp.sendRedirect(redirectTo);
  }
}
