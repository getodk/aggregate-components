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

package org.opendatakit.dwc.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.SyncBasicHttpContext;
import org.codehaus.jackson.map.ObjectMapper;
import org.opendatakit.dwc.client.Configuration;
import org.opendatakit.dwc.client.GreetingService;
import org.opendatakit.dwc.servlet.OAuthServlet;
import org.opendatakit.dwc.shared.FieldVerifier;

import com.google.gdata.client.authn.oauth.GoogleOAuthHelper;
import com.google.gdata.client.authn.oauth.GoogleOAuthParameters;
import com.google.gdata.client.authn.oauth.OAuthException;
import com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class GreetingServiceImpl extends RemoteServiceServlet implements GreetingService {
  public static OAuthToken theAuthToken;
  
  private static final Log logger = LogFactory.getLog(GreetingServiceImpl.class);

  private static boolean firstTime = true;
  // CLIENT_WEBSITE is this project's website.  
  // It needs a Dynamic DNS name set up so that Google can redirect back to this project's website.
  public static String CLIENT_WEBSITE_HOSTNAME = "127.0.0.1";
  public static String CLIENT_WEBSITE_PORT = "8888";
  public static String CLIENT_WEBSITE_CODESVR_PORT = "9997";
  public static String CLIENT_ID = "322300403941-0cvom80einmvnff7rt95iese90r7klof.apps.googleusercontent.com";
  public static String CLIENT_SECRET = "og5s65IHph_DET3zICUi0JoI";

  private static final String OAUTH_CONSUMER_KEY = "anonymous";
  private static final String OAUTH_CONSUMER_SECRET = "anonymous";

  private static final int SERVICE_TIMEOUT_MILLISECONDS = 60000;

  private static final int SOCKET_ESTABLISHMENT_TIMEOUT_MILLISECONDS = 60000;

  private static final String UTF_8 = "UTF-8";

  private static final String OAUTH_TOKEN_SECRET_PARAMETER = "oauth_token_secret";

  public String greetServer(String input) throws IllegalArgumentException {
    // Verify that the input is valid. 
    if (!FieldVerifier.isValidName(input)) {
      // If the input is not valid, throw an IllegalArgumentException back to
      // the client.
      throw new IllegalArgumentException("Name must be at least 4 characters long");
    }

    String serverInfo = getServletContext().getServerInfo();
    String userAgent = getThreadLocalRequest().getHeader("User-Agent");

    // Escape data from the client to avoid cross-site script vulnerabilities.
    input = escapeHtml(input);
    userAgent = escapeHtml(userAgent);

    return "Hello, " + input + "!<br><br>I am running " + serverInfo
        + ".<br><br>It looks like you are using:<br>" + userAgent;
  }

  /**
   * Escape an html string. Escaping data received from the client helps to
   * prevent cross-site script vulnerabilities.
   * 
   * @param html the html string to escape
   * @return the escaped string
   */
  private String escapeHtml(String html) {
    if (html == null) {
      return null;
    }
    return html.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
  }
  
  protected GoogleOAuthParameters getOAuthParams() {
    OAuthToken token = theAuthToken;  
    GoogleOAuthParameters oauthParameters = new GoogleOAuthParameters();
    oauthParameters.setOAuthConsumerKey(OAUTH_CONSUMER_KEY);
    oauthParameters.setOAuthConsumerSecret(OAUTH_CONSUMER_SECRET);
    oauthParameters.setOAuthToken(token.getToken());
    oauthParameters.setOAuthTokenSecret(token.getTokenSecret());
    return oauthParameters;
  }
  
  public void deleteToken() {
    // remove fusion table permission as no longer needed
    // TODO: test that the revoke REALLY works, can be easy to miss since we
    // ignore exception
    try {
      GoogleOAuthHelper oauthHelper = new GoogleOAuthHelper(new OAuthHmacSha1Signer());
      oauthHelper.revokeToken(getOAuthParams());
      
    } catch (OAuthException e) {
      // just moving on, as we still want to delete
      e.printStackTrace();
    } finally {
      theAuthToken = null;
    }
  }
  
  public static String getServerURL(HttpServletRequest req, String relativeServletPath, boolean preserveQS) {
    // for now, only store the servlet context and the serverUrl
    String path = req.getContextPath();

    Integer identifiedPort = req.getServerPort();
    String identifiedHostname = req.getServerName();
    
    if ( identifiedHostname == null ||
    	 identifiedHostname.length() == 0 ||  
         identifiedHostname.equals("0.0.0.0") ) {
      try {
         identifiedHostname = InetAddress.getLocalHost().getCanonicalHostName();
      } catch (UnknownHostException e) {
         identifiedHostname = "127.0.0.1";
      }
    }
    
    String identifiedScheme = "http";
    
    if ( identifiedPort == null || identifiedPort == 0 ) {
       if ( req.getScheme().equals(identifiedScheme) ) {
          identifiedPort = req.getServerPort();
       } else {
          identifiedPort = HtmlConsts.WEB_PORT;
       }
    }
    
    boolean expectedPort = 
       (identifiedScheme.equalsIgnoreCase("http") &&
             identifiedPort == HtmlConsts.WEB_PORT) ||
       (identifiedScheme.equalsIgnoreCase("https") &&
             identifiedPort == HtmlConsts.SECURE_WEB_PORT);
    
    String serverUrl;
    if (!expectedPort) {
       serverUrl = identifiedScheme + "://" + identifiedHostname + BasicConsts.COLON + 
          Integer.toString(identifiedPort) + path;
     } else {
       serverUrl = identifiedScheme + "://" +identifiedHostname + path;
     }
    
    String query = req.getQueryString();
    if ( query == null ) {
      if ( req.getServletPath().equals("/demowebclient/greet") ) {
        if ( CLIENT_WEBSITE_CODESVR_PORT.length() != 0 ) {
        	query = "gwt.codesvr=127.0.0.1:" + CLIENT_WEBSITE_CODESVR_PORT;
        } else {
        	query = "";
        }
      } else {
        query = "";
      }
    }
    
    if ( query.length() != 0 ) {
      query = "?" + query;
    }
    return serverUrl + BasicConsts.FORWARDSLASH + relativeServletPath + (preserveQS ? query : "");
  }

  @Override
  public String obtainToken(String destinationUrl) throws IllegalArgumentException {
    HttpServletRequest req = this.getThreadLocalRequest();

    try {

      String scope = "https://www.googleapis.com/auth/userinfo.email";

      GoogleOAuthParameters oauthParameters = new GoogleOAuthParameters();
      oauthParameters.setOAuthConsumerKey(OAUTH_CONSUMER_KEY);
      oauthParameters.setOAuthConsumerSecret(OAUTH_CONSUMER_SECRET);
      oauthParameters.setScope(scope);

      GoogleOAuthHelper oauthHelper = new GoogleOAuthHelper(new OAuthHmacSha1Signer());
      oauthHelper.getUnauthorizedRequestToken(oauthParameters);
      Map<String, String> params = new HashMap<String, String>();
      params.put("additionalTerm", "Term");
      params.put("xoauth_displayname", getServerURL(req,"",false));
      params.put(OAUTH_TOKEN_SECRET_PARAMETER, oauthParameters.getOAuthTokenSecret());
      String addr = getServerURL(req, OAuthServlet.ADDR, true);
      String callbackUrl = HtmlUtil.createLinkWithProperties(addr, params);

      oauthParameters.setOAuthCallback(callbackUrl);

      String url =  oauthHelper.createUserAuthorizationUrl(oauthParameters);
      logger.info("The constructed URL is: " + url);
      
      return url;

    } catch (OAuthException e) {
      e.printStackTrace();
    }
    return null;
  }
  
  @Override
  public String obtainOauth1Data(String destinationUrl) throws IllegalArgumentException {
    OAuthToken authToken = theAuthToken;
    
    OAuthConsumer consumer = new CommonsHttpOAuthConsumer(OAUTH_CONSUMER_KEY,
        OAUTH_CONSUMER_SECRET);
    consumer.setTokenWithSecret(authToken.getToken(), authToken.getTokenSecret());

    URI uri;
    try {
      uri = new URI(destinationUrl);
    } catch ( Exception e ) {
      throw new IllegalArgumentException(e.toString());
    }
    
    System.out.println(uri.toString());
    HttpParams httpParams = new BasicHttpParams();
    HttpConnectionParams.setConnectionTimeout(httpParams, SERVICE_TIMEOUT_MILLISECONDS);
    HttpConnectionParams.setSoTimeout(httpParams, SOCKET_ESTABLISHMENT_TIMEOUT_MILLISECONDS);
    
    HttpClientFactory factory = new GaeHttpClientFactoryImpl();;
    HttpClient client = factory.createHttpClient(httpParams);
    HttpGet get = new HttpGet(uri);

    try {
      consumer.sign(get);
    } catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Failed to sign request: " + e.getMessage());
    }

    try {
      HttpResponse resp = client.execute(get);
      // TODO: this section of code is possibly causing 'WARNING: Going to buffer
      // response body of large or unknown size. Using getResponseBodyAsStream
      // instead is recommended.'
      // The WARNING is most likely only happening when running appengine locally,
      // but we should investigate to make sure
      StringBuffer response = new StringBuffer();
      if ( resp.getEntity() != null ) {
	      BufferedReader reader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
	      String responseLine;
	      while ((responseLine = reader.readLine()) != null) {
	        response.append(responseLine);
	      }
      }
      if (resp.getStatusLine().getStatusCode() != HttpServletResponse.SC_OK) {
        throw new IllegalArgumentException("Error: " + resp.getStatusLine().getReasonPhrase() + " status code: " +
        				resp.getStatusLine().getStatusCode() + " body: " + response.toString());
      }
      return response.toString();
    } catch ( IOException e ) {
      throw new IllegalArgumentException(e.toString());
    }
  }

  public class Context {
    String key;
    Map<String, Object> contextMap = new HashMap<String, Object>();
    
    public Context() {
      key = "uuid:" + UUID.randomUUID().toString();
    }
    
    public String getKey() {
      return key;
    }
    
    public void putContext(String key, Object o) {
      contextMap.put(key, o);
    }
    
    public Object getContext(String key) {
      return contextMap.get(key);
    }
  }
  
  private static Map<String,Context> stateMap = new HashMap<String, Context>();

  public static Context getStateContext(String state) {
    return stateMap.get(state);
  }
  
  private static HttpContext localContext = null;
  
  public static synchronized final HttpContext getHttpContext() {
    if (localContext == null) {
      // set up one context for all HTTP requests so that authentication
      // and cookies can be retained.
      localContext = new SyncBasicHttpContext(new BasicHttpContext());

      // establish a local cookie store for this attempt at downloading...
      CookieStore cookieStore = new BasicCookieStore();
      localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

      // and establish a credentials provider.
      CredentialsProvider credsProvider = new BasicCredentialsProvider();
      localContext.setAttribute(ClientContext.CREDS_PROVIDER, credsProvider);
  }
  return localContext;

  }
  
  public static final List<AuthScope> buildAuthScopes(String host) {
      List<AuthScope> asList = new ArrayList<AuthScope>();

      AuthScope a;
      // allow digest auth on any port...
      a = new AuthScope(host, -1, null, AuthPolicy.DIGEST);
      asList.add(a);
      // and allow basic auth on the standard TLS/SSL ports...
      a = new AuthScope(host, 443, null, AuthPolicy.BASIC);
      asList.add(a);
      a = new AuthScope(host, 8443, null, AuthPolicy.BASIC);
      asList.add(a);

      return asList;
  }

  public static final void clearAllCredentials() {
      HttpContext localContext = getHttpContext();
      CredentialsProvider credsProvider =
          (CredentialsProvider) localContext.getAttribute(ClientContext.CREDS_PROVIDER);
      credsProvider.clear();
  }

  public static final void addCredentials(String userEmail, String password, String host) {
      HttpContext localContext = getHttpContext();
      Credentials c = new UsernamePasswordCredentials(userEmail, password);
      addCredentials(localContext, c, host);
  }


  private static final void addCredentials(HttpContext localContext, Credentials c, String host) {
      CredentialsProvider credsProvider =
          (CredentialsProvider) localContext.getAttribute(ClientContext.CREDS_PROVIDER);

      List<AuthScope> asList = buildAuthScopes(host);
      for (AuthScope a : asList) {
          credsProvider.setCredentials(a, c);
      }
  }

  // builds and returns an HttpRequestInterceptor that implements preemptive authentication
    // code taken from http://dlinsin.blogspot.com/2009/08/http-basic-authentication-with-android.html
    private static HttpRequestInterceptor getPreemptiveAuth() {
        return new HttpRequestInterceptor() {
            public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
                AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);
                CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(
                        ClientContext.CREDS_PROVIDER);
                HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);

                if (authState.getAuthScheme() == null) {
                    AuthScope authScope = new AuthScope(targetHost.getHostName(), targetHost.getPort());
                    Credentials creds = credsProvider.getCredentials(authScope);
                    if (creds != null) {
                        authState.setAuthScheme(new BasicScheme());
                        authState.setCredentials(creds);
                    }
                }
            }    
        };
    }

    private static String authUrl = "https://accounts.google.com/o/oauth2/auth";
    private static String tokenUrl = "https://accounts.google.com/o/oauth2/token";
    private static String userInfoUrl = "https://www.googleapis.com/oauth2/v1/userinfo";
    private static String scope = "https://www.googleapis.com/auth/userinfo.email";
    private static String ctxtKey = null;
    
    private String getSelfUrl() {
        String url = "http://" + CLIENT_WEBSITE_HOSTNAME + ":" + CLIENT_WEBSITE_PORT + "/DemoWebCLient.html" +
      				((CLIENT_WEBSITE_CODESVR_PORT.length() == 0) ? "" : "?gwt.codesvr=127.0.0.1:" + CLIENT_WEBSITE_CODESVR_PORT);
        return url;
    }
    
    private String getOauth2CallbackUrl() {
    	String url = "http://" + CLIENT_WEBSITE_HOSTNAME + ":" + CLIENT_WEBSITE_PORT + "/auth/cb" +
    					((CLIENT_WEBSITE_CODESVR_PORT.length() == 0) ? "" : "?gwt.codesvr=127.0.0.1:" + CLIENT_WEBSITE_CODESVR_PORT);
    	return url;
    }
    
  @Override
  public String obtainOauth2Code(String destinationUrl) throws IllegalArgumentException {
    URI nakedUri;
    try {
      nakedUri = new URI(authUrl);
    } catch (URISyntaxException e2) {
      e2.printStackTrace();
      logger.error(e2.toString());
      return getSelfUrl();
    }
    addCredentials(CLIENT_ID, CLIENT_SECRET, nakedUri.getHost());
    Context ctxt = new Context();
    stateMap.put(ctxt.getKey(), ctxt);
    ctxtKey = ctxt.getKey();
    
    List<NameValuePair> qparams = new ArrayList<NameValuePair>();
    qparams.add(new BasicNameValuePair("response_type", "code"));
    qparams.add(new BasicNameValuePair("client_id", CLIENT_ID));
    qparams.add(new BasicNameValuePair("scope", scope));
    qparams.add(new BasicNameValuePair("redirect_uri", getOauth2CallbackUrl()));
    qparams.add(new BasicNameValuePair("state", ctxt.getKey()));
    URI uri;
    try {
      uri = URIUtils.createURI(nakedUri.getScheme(), nakedUri.getHost(), nakedUri.getPort(), nakedUri.getPath(), 
          URLEncodedUtils.format(qparams, "UTF-8"), null);
    } catch (URISyntaxException e1) {
      e1.printStackTrace();
      logger.error(e1.toString());
      return getSelfUrl();
    }
    
    String toString = uri.toString();
    return toString;
  }

  @Override
  public String getOauth2UserEmail() throws IllegalArgumentException {

    // get the auth code...
    Context ctxt = getStateContext(ctxtKey);
    String code = (String) ctxt.getContext("code");
    { 
      // convert the auth code into an auth token
      URI nakedUri;
      try {
        nakedUri = new URI(tokenUrl);
      } catch (URISyntaxException e2) {
        e2.printStackTrace();
        logger.error(e2.toString());
        return getSelfUrl();
      }
      
      // DON'T NEED clientId on the toke request...
      // addCredentials(clientId, clientSecret, nakedUri.getHost());
      // setup request interceptor to do preemptive auth
      // ((DefaultHttpClient) client).addRequestInterceptor(getPreemptiveAuth(), 0);
      
      HttpClientFactory factory = new GaeHttpClientFactoryImpl();
      
      HttpParams httpParams = new BasicHttpParams();
      HttpConnectionParams.setConnectionTimeout(httpParams, SERVICE_TIMEOUT_MILLISECONDS);
      HttpConnectionParams.setSoTimeout(httpParams, SOCKET_ESTABLISHMENT_TIMEOUT_MILLISECONDS);
      // support redirecting to handle http: => https: transition
      HttpClientParams.setRedirecting(httpParams, true);
      // support authenticating
      HttpClientParams.setAuthenticating(httpParams, true);
      
      httpParams.setParameter(ClientPNames.MAX_REDIRECTS, 1);
      httpParams.setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
      // setup client
      HttpClient client = factory.createHttpClient(httpParams);
  
      HttpPost httppost = new HttpPost(nakedUri);
      logger.info(httppost.getURI().toString());
      
      // THESE ARE POST BODY ARGS...    
      List<NameValuePair> qparams = new ArrayList<NameValuePair>();
      qparams.add(new BasicNameValuePair("grant_type", "authorization_code"));
      qparams.add(new BasicNameValuePair("client_id", CLIENT_ID));
      qparams.add(new BasicNameValuePair("client_secret", CLIENT_SECRET));
      qparams.add(new BasicNameValuePair("code", code));
      qparams.add(new BasicNameValuePair("redirect_uri", getOauth2CallbackUrl()));
      UrlEncodedFormEntity postentity;
      try {
        postentity = new UrlEncodedFormEntity(qparams,"UTF-8");
      } catch (UnsupportedEncodingException e1) {
        e1.printStackTrace();
        logger.error(e1.toString());
        throw new IllegalArgumentException("Unexpected");
      }
      
      httppost.setEntity(postentity);
  
      HttpResponse response = null;
      try {
          response = client.execute(httppost, localContext);
          int statusCode = response.getStatusLine().getStatusCode();
  
          if ( statusCode != HttpStatus.SC_OK ) {
            logger.error("not 200: " + statusCode);
            return "Error with Oauth2 token request - reason: " + response.getStatusLine().getReasonPhrase() + " status code: " + statusCode;
          } else {
            HttpEntity entity = response.getEntity();
    
            if (entity != null && entity.getContentType().getValue().toLowerCase()
                            .contains("json")) {
              ObjectMapper mapper = new ObjectMapper();
              BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
              Map<String,Object> userData = mapper.readValue(reader, Map.class);
              // stuff the map in the Context...
              for ( Map.Entry<String, Object> e : userData.entrySet()) {
                ctxt.putContext(e.getKey(), e.getValue());
              }
            } else {
              logger.error("unexpected body");
              return "Error with Oauth2 token request - missing body";
            }
          }
      } catch ( IOException e ) {
        throw new IllegalArgumentException(e.toString());
      }
    }

    // OK if we got here, we have a valid token.  
    // Issue the request...
    String email = null;
    {
      URI nakedUri;
      try {
        nakedUri = new URI(userInfoUrl);
      } catch (URISyntaxException e2) {
        e2.printStackTrace();
        logger.error(e2.toString());
        return getSelfUrl();
      }
      
      List<NameValuePair> qparams = new ArrayList<NameValuePair>();
      qparams.add(new BasicNameValuePair("access_token", (String) ctxt.getContext("access_token")));
      URI uri;
      try {
        uri = URIUtils.createURI(nakedUri.getScheme(), nakedUri.getHost(), nakedUri.getPort(), nakedUri.getPath(), 
            URLEncodedUtils.format(qparams, "UTF-8"), null);
      } catch (URISyntaxException e1) {
        e1.printStackTrace();
        logger.error(e1.toString());
        return getSelfUrl();
      }
      
      // DON'T NEED clientId on the toke request...
      // addCredentials(clientId, clientSecret, nakedUri.getHost());
      // setup request interceptor to do preemptive auth
      // ((DefaultHttpClient) client).addRequestInterceptor(getPreemptiveAuth(), 0);
      
      HttpClientFactory factory = new GaeHttpClientFactoryImpl();
      
      HttpParams httpParams = new BasicHttpParams();
      HttpConnectionParams.setConnectionTimeout(httpParams, SERVICE_TIMEOUT_MILLISECONDS);
      HttpConnectionParams.setSoTimeout(httpParams, SOCKET_ESTABLISHMENT_TIMEOUT_MILLISECONDS);
      // support redirecting to handle http: => https: transition
      HttpClientParams.setRedirecting(httpParams, true);
      // support authenticating
      HttpClientParams.setAuthenticating(httpParams, true);
      
      httpParams.setParameter(ClientPNames.MAX_REDIRECTS, 1);
      httpParams.setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
      // setup client
      HttpClient client = factory.createHttpClient(httpParams);
  
      HttpGet httpget = new HttpGet(uri);
      logger.info(httpget.getURI().toString());
  
      HttpResponse response = null;
      try {
          response = client.execute(httpget, localContext);
          int statusCode = response.getStatusLine().getStatusCode();
  
          if ( statusCode != HttpStatus.SC_OK ) {
            logger.error("not 200: " + statusCode);
            return "Error - reason: " + response.getStatusLine().getReasonPhrase() + " status code: " + statusCode;
          } else {
            HttpEntity entity = response.getEntity();
    
            if (entity != null && entity.getContentType().getValue().toLowerCase()
                            .contains("json")) {
              ObjectMapper mapper = new ObjectMapper();
              BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
              Map<String,Object> userData = mapper.readValue(reader, Map.class);

              email = (String) userData.get("email");
            } else {
              logger.error("unexpected body");
              return "Error - missing body";
            }
          }
      } catch ( IOException e ) {
        throw new IllegalArgumentException(e.toString());
      }
    }

    return email;
  }

  @Override
  public String obtainOauth2Data(String destinationUrl) throws IllegalArgumentException {

    // get the auth code...
    Context ctxt = getStateContext(ctxtKey);
    String code = (String) ctxt.getContext("code");
    { 
      // convert the auth code into an auth token
      URI nakedUri;
      try {
        nakedUri = new URI(tokenUrl);
      } catch (URISyntaxException e2) {
        e2.printStackTrace();
        logger.error(e2.toString());
        return getSelfUrl();
      }
      
      // DON'T NEED clientId on the toke request...
      // addCredentials(clientId, clientSecret, nakedUri.getHost());
      // setup request interceptor to do preemptive auth
      // ((DefaultHttpClient) client).addRequestInterceptor(getPreemptiveAuth(), 0);
      
      HttpClientFactory factory = new GaeHttpClientFactoryImpl();
      
      HttpParams httpParams = new BasicHttpParams();
      HttpConnectionParams.setConnectionTimeout(httpParams, SERVICE_TIMEOUT_MILLISECONDS);
      HttpConnectionParams.setSoTimeout(httpParams, SOCKET_ESTABLISHMENT_TIMEOUT_MILLISECONDS);
      // support redirecting to handle http: => https: transition
      HttpClientParams.setRedirecting(httpParams, true);
      // support authenticating
      HttpClientParams.setAuthenticating(httpParams, true);
      
      httpParams.setParameter(ClientPNames.MAX_REDIRECTS, 1);
      httpParams.setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
      // setup client
      HttpClient client = factory.createHttpClient(httpParams);
  
      HttpPost httppost = new HttpPost(nakedUri);
      logger.info(httppost.getURI().toString());
      
      // THESE ARE POST BODY ARGS...    
      List<NameValuePair> qparams = new ArrayList<NameValuePair>();
      qparams.add(new BasicNameValuePair("grant_type", "authorization_code"));
      qparams.add(new BasicNameValuePair("client_id", CLIENT_ID));
      qparams.add(new BasicNameValuePair("client_secret", CLIENT_SECRET));
      qparams.add(new BasicNameValuePair("code", code));
      qparams.add(new BasicNameValuePair("redirect_uri", getOauth2CallbackUrl()));
      UrlEncodedFormEntity postentity;
      try {
        postentity = new UrlEncodedFormEntity(qparams,"UTF-8");
      } catch (UnsupportedEncodingException e1) {
        e1.printStackTrace();
        logger.error(e1.toString());
        throw new IllegalArgumentException("Unexpected");
      }
      
      httppost.setEntity(postentity);
  
      HttpResponse response = null;
      try {
          response = client.execute(httppost, localContext);
          int statusCode = response.getStatusLine().getStatusCode();
  
          if ( statusCode != HttpStatus.SC_OK ) {
            logger.error("not 200: " + statusCode);
            return "Error with Oauth2 token request - reason: " + response.getStatusLine().getReasonPhrase() + " status code: " + statusCode;
          } else {
            HttpEntity entity = response.getEntity();
    
            if (entity != null && entity.getContentType().getValue().toLowerCase()
                            .contains("json")) {
              ObjectMapper mapper = new ObjectMapper();
              BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
              Map<String,Object> userData = mapper.readValue(reader, Map.class);
              // stuff the map in the Context...
              for ( Map.Entry<String, Object> e : userData.entrySet()) {
                ctxt.putContext(e.getKey(), e.getValue());
              }
            } else {
              logger.error("unexpected body");
              return "Error with Oauth2 token request - unexpected body";
            }
          }
      } catch ( IOException e ) {
        throw new IllegalArgumentException(e.toString());
      }
    }

    // OK if we got here, we have a valid token.  
    // Issue the request...
    {
      URI nakedUri;
      try {
        nakedUri = new URI(destinationUrl);
      } catch (URISyntaxException e2) {
        e2.printStackTrace();
        logger.error(e2.toString());
        return getSelfUrl();
      }
      
      List<NameValuePair> qparams = new ArrayList<NameValuePair>();
      qparams.add(new BasicNameValuePair("access_token", (String) ctxt.getContext("access_token")));
      URI uri;
      try {
        uri = URIUtils.createURI(nakedUri.getScheme(), nakedUri.getHost(), nakedUri.getPort(), nakedUri.getPath(), 
            URLEncodedUtils.format(qparams, "UTF-8"), null);
      } catch (URISyntaxException e1) {
        e1.printStackTrace();
        logger.error(e1.toString());
        return getSelfUrl();
      }
      
      // DON'T NEED clientId on the toke request...
      // addCredentials(clientId, clientSecret, nakedUri.getHost());
      // setup request interceptor to do preemptive auth
      // ((DefaultHttpClient) client).addRequestInterceptor(getPreemptiveAuth(), 0);
      
      HttpClientFactory factory = new GaeHttpClientFactoryImpl();
      
      HttpParams httpParams = new BasicHttpParams();
      HttpConnectionParams.setConnectionTimeout(httpParams, SERVICE_TIMEOUT_MILLISECONDS);
      HttpConnectionParams.setSoTimeout(httpParams, SOCKET_ESTABLISHMENT_TIMEOUT_MILLISECONDS);
      // support redirecting to handle http: => https: transition
      HttpClientParams.setRedirecting(httpParams, true);
      // support authenticating
      HttpClientParams.setAuthenticating(httpParams, true);
      
      httpParams.setParameter(ClientPNames.MAX_REDIRECTS, 1);
      httpParams.setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
      // setup client
      HttpClient client = factory.createHttpClient(httpParams);
  
      HttpGet httpget = new HttpGet(uri);
      logger.info(httpget.getURI().toString());
  
      HttpResponse response = null;
      try {
          response = client.execute(httpget, localContext);
          int statusCode = response.getStatusLine().getStatusCode();
  
          if ( statusCode != HttpStatus.SC_OK ) {
            logger.error("not 200: " + statusCode);
            return "Error";
          } else {
            HttpEntity entity = response.getEntity();
    
            if (entity != null ) {
              String contentType = entity.getContentType().getValue();
              if ( contentType.toLowerCase().contains("xml")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
                StringBuilder b = new StringBuilder();
                String line;
                while ( (line = reader.readLine()) != null ) {
                  b.append(line);
                }
                String value = b.toString();
                return value;
              } else {
                logger.error("unexpected body");
                return "Error";
              } 
            } else {
              logger.error("unexpected missing body");
              return "Error";
            }
          }
      } catch ( IOException e ) {
        throw new IllegalArgumentException(e.toString());
      }
    }
  }

	@Override
	public void setConfiguration(Configuration config)
			throws IllegalArgumentException {
		{
			String value = config.get(Configuration.CLIENT_WEBSITE_HOSTNAME_KEY);
			if ( value != null && value.length() > 0 ) {
				CLIENT_WEBSITE_HOSTNAME = value;
			}
		}
		{
			String value = config.get(Configuration.CLIENT_WEBSITE_PORT_KEY);
			if ( value != null && value.length() > 0 ) {
				CLIENT_WEBSITE_PORT = value;
			}
		}
		{
			String value = config.get(Configuration.CLIENT_WEBSITE_CODESVR_PORT_KEY);
			if ( value != null ) {
				CLIENT_WEBSITE_CODESVR_PORT = value;
			}
		}
		{
			String value = config.get(Configuration.CLIENT_ID_KEY);
			if ( value != null && value.length() > 0 ) {
				CLIENT_ID = value;
			}
		}
		{
			String value = config.get(Configuration.CLIENT_SECRET_KEY);
			if ( value != null && value.length() > 0 ) {
				CLIENT_SECRET = value;
			}
		}
	}

	@Override
	public Configuration getConfiguration() throws IllegalArgumentException {
		if ( firstTime ) {
			firstTime = false;
		    HttpServletRequest req = this.getThreadLocalRequest();
		    Integer identifiedPort = req.getServerPort();
		    if ( identifiedPort != null && identifiedPort != 0 ) {
		    	CLIENT_WEBSITE_PORT = Integer.toString(identifiedPort);
		    }
		    String identifiedHostname = req.getServerName();
		    
		    if ( identifiedHostname == null ||
		    	 identifiedHostname.length() == 0 ||  
		         identifiedHostname.equals("0.0.0.0") ) {
		      try {
		         identifiedHostname = InetAddress.getLocalHost().getCanonicalHostName();
		      } catch (UnknownHostException e) {
		         identifiedHostname = "127.0.0.1";
		      }
		    }
		    CLIENT_WEBSITE_HOSTNAME = identifiedHostname;
		    if ( CLIENT_WEBSITE_PORT.equals("8888") ) {
		    	CLIENT_WEBSITE_CODESVR_PORT = "9997";
		    } else if ( CLIENT_WEBSITE_PORT.equals("8777") ) {
		    	CLIENT_WEBSITE_CODESVR_PORT = "9777";
		    } else {
		    	CLIENT_WEBSITE_CODESVR_PORT = "9777";
		    }
		}
		Configuration config = new Configuration();
		config.put(Configuration.CLIENT_WEBSITE_HOSTNAME_KEY, CLIENT_WEBSITE_HOSTNAME);
		config.put(Configuration.CLIENT_WEBSITE_PORT_KEY, CLIENT_WEBSITE_PORT);
		config.put(Configuration.CLIENT_WEBSITE_CODESVR_PORT_KEY, CLIENT_WEBSITE_CODESVR_PORT);
		config.put(Configuration.CLIENT_ID_KEY, CLIENT_ID);
		config.put(Configuration.CLIENT_SECRET_KEY, CLIENT_SECRET);
		
		return config;
	}
}
