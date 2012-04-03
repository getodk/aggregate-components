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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opendatakit.dwc.server.GreetingServiceImpl;
import org.opendatakit.dwc.server.GreetingServiceImpl.Context;

public class CallbackServlet extends ServletUtilBase {

  /**
   * Serial number for serialization
   */
  private static final long serialVersionUID = -2220827379519391127L;

  /**
   * URI from base
   */
  public static final String ADDR = "auth/cb";


  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    
    // get parameters
    String code = getParameter(req, "code");

    // get parameters
    String state = getParameter(req, "state");

    Context ctxt = GreetingServiceImpl.getStateContext(state);
    
    ctxt.putContext("code", code);
    
    resp.sendRedirect("/DemoWebClient.html" + 
    		((GreetingServiceImpl.CLIENT_WEBSITE_CODESVR_PORT.length() == 0) ? "" :
    			"?gwt.codesvr=127.0.0.1:" + GreetingServiceImpl.CLIENT_WEBSITE_CODESVR_PORT));
  }
}
