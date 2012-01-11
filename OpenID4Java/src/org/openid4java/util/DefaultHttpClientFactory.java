/*
 * Copyright 2006-2008 Sxip Identity Corporation
 */

/*
 * Created on Mar 5, 2007
 */
package org.openid4java.util;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.params.AllClientPNames;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import com.google.inject.Inject;

/**
 * This class handles all HTTPClient connections for the
 * org.openid4java packages.
 *
 * @author Kevin
 */
public class DefaultHttpClientFactory implements HttpClientFactory
{
  
    private ClientConnectionManagerFactory clientConnectionManagerFactory = null;
    
    /**
     * proxy properties for HTTPClient calls
     */
    private ProxyProperties proxyProperties = null;

    @Inject
    public DefaultHttpClientFactory() {
    }

    public ProxyProperties getProxyProperties()
    {
        return proxyProperties;
    }

    public void setProxyProperties(ProxyProperties proxyProperties)
    {
        this.proxyProperties = proxyProperties;
    }
    
    public void setClientConnectionManagerFactory(ClientConnectionManagerFactory factory) {
      this.clientConnectionManagerFactory = factory;
    }
    
    private synchronized ClientConnectionManagerFactory getClientConnectionManagerFactory() {
      if ( clientConnectionManagerFactory == null ) {
        clientConnectionManagerFactory = new DefaultClientConnectionManagerFactory();
      }
      
      return clientConnectionManagerFactory;
    }

    public HttpClient getInstance(int maxRedirects,
            Boolean allowCircularRedirects,
            int connTimeout, int socketTimeout,
            String cookiePolicy)
    {
        ClientConnectionManagerFactory connManagerFactory = getClientConnectionManagerFactory();
        ClientConnectionManager connManager = connManagerFactory.getConnectionManager();

        HttpParams httpParams = new BasicHttpParams();

        DefaultHttpClient client = new DefaultHttpClient(connManager, httpParams);

        client.getParams().setParameter(AllClientPNames.MAX_REDIRECTS,
                                        new Integer(maxRedirects));
        client.getParams().setParameter(AllClientPNames.ALLOW_CIRCULAR_REDIRECTS,
                                        allowCircularRedirects);
        client.getParams().setParameter(AllClientPNames.SO_TIMEOUT,
        								   new Integer(socketTimeout));
        client.getParams().setParameter(AllClientPNames.CONNECTION_TIMEOUT,
				   					   new Integer(connTimeout));

        if (cookiePolicy == null)
        {
            client.setCookieStore(null);
        }
        else
        {
            client.getParams().setParameter(AllClientPNames.COOKIE_POLICY,
                    cookiePolicy);
        }
        

        if (proxyProperties != null)
        {
            HttpHost proxy = new HttpHost(
                    proxyProperties.getProxyHostName(), 
                    proxyProperties.getProxyPort()); 

	        client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);

            //now set headers for auth
            AuthScope authScope = new AuthScope(AuthScope.ANY_HOST,
                    AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthScope.ANY_SCHEME);
            Credentials credentials = proxyProperties.getCredentials();
            client.getCredentialsProvider().setCredentials(authScope, credentials);
        }

        return client;
    }
}

