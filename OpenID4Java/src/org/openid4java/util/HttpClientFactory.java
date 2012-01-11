/*
 * Copyright 2006-2008 Sxip Identity Corporation
 */

/*
 * Created on Mar 5, 2007
 */
package org.openid4java.util;

import org.apache.http.client.HttpClient;

import com.google.inject.ImplementedBy;

/**
 * This class handles all HTTPClient connections for the
 * org.openid4java packages.
 *
 * @author Kevin
 */
@ImplementedBy(DefaultHttpClientFactory.class)
public interface HttpClientFactory
{
    public ProxyProperties getProxyProperties();

    public void setProxyProperties(ProxyProperties proxyProperties);

    public HttpClient getInstance(int maxRedirects,
            Boolean allowCircularRedirects,
            int connTimeout, int socketTimeout,
            String cookiePolicy);
}

