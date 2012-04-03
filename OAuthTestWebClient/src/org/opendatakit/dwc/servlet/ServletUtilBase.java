/*
 * Copyright (C) 2009 Google Inc. 
 * Copyright (C) 2010 University of Washington.
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
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opendatakit.dwc.server.HtmlConsts;
import org.opendatakit.dwc.server.HtmlUtil;
import org.opendatakit.dwc.server.OAuthToken;

import com.google.gdata.client.authn.oauth.GoogleOAuthHelper;
import com.google.gdata.client.authn.oauth.GoogleOAuthParameters;
import com.google.gdata.client.authn.oauth.OAuthException;
import com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer;

/**
 * Base class for Servlets that contain useful utilities
 * 
 */
@SuppressWarnings("serial")
public class ServletUtilBase extends CommonServletBase {

  private static final String APPLICATION_NAME = "DemoWebClient";
  private static final String MISSING_PARAMS = "Missing parameters";
  private static final String INVALID_PARAMS = "Invalid parameters";


  protected ServletUtilBase() {
    super(APPLICATION_NAME);
  }
  

  protected void errorMissingParam(HttpServletResponse resp) throws IOException {
	    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, MISSING_PARAMS);
  }
  /**
   * Generate error response for invalid parameters
   * 
   * @param resp
   *          The HTTP response to be sent to client
   * @throws IOException
   *           caused by problems writing error information to response
   */
  protected void errorBadParam(HttpServletResponse resp) throws IOException {
    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, INVALID_PARAMS);
  }
  
  // GWT required fields...
  private static final String OAUTH_CONSUMER_KEY = "anonymous";
  private static final String OAUTH_CONSUMER_SECRET = "anonymous";
  private static final String OAUTH_TOKEN_PARAMETER = "oauth_token";
  

  @Override
  protected void beginBasicHtmlResponse(String pageName, HttpServletResponse resp) throws IOException {
	  
	PrintWriter out = beginBasicHtmlResponsePreamble( "", resp );
    out.write(HtmlUtil.createBeginTag(HtmlConsts.CENTERING_DIV));
    out.write(HtmlUtil.wrapWithHtmlTags(HtmlConsts.H1, pageName));
    out.write(HtmlUtil.createEndTag(HtmlConsts.DIV));
}

  protected OAuthToken verifyGDataAuthorization(HttpServletRequest req, HttpServletResponse resp) 
  		throws IOException {
	  
		boolean receivingToken = getParameter(req, OAUTH_TOKEN_PARAMETER) != null;
		if (receivingToken)
		{
		  	GoogleOAuthParameters oauthParameters = new GoogleOAuthParameters();
			oauthParameters.setOAuthConsumerKey(OAUTH_CONSUMER_KEY);
			oauthParameters.setOAuthConsumerSecret(OAUTH_CONSUMER_SECRET);
			GoogleOAuthHelper oauthHelper = new GoogleOAuthHelper(new OAuthHmacSha1Signer());
			oauthHelper.getOAuthParametersFromCallback(req.getQueryString(), oauthParameters);
			try {
				oauthHelper.getAccessToken(oauthParameters);
			} catch (OAuthException e) {
			   e.printStackTrace();
		      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
		        		"Error whine retrieving OAuth token");
				throw new IllegalArgumentException(e.toString());
			}
			
			return new OAuthToken(oauthParameters.getOAuthToken(), oauthParameters.getOAuthTokenSecret());
		}
		else
		{
			return null;
		}
  }
}
