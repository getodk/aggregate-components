/*
 * Copyright 2006-2008 Sxip Identity Corporation
 */

package org.openid4java.util;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.AllClientPNames;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public final class HttpUtils
{
    private HttpUtils()
    {
        // empty
    }

    public static void dispose(final org.apache.http.HttpResponse response)
    {
        if (response != null)
        {
            HttpEntity e = response.getEntity();
            if (e != null)
            {
                dispose(e);
            }
        }
    }

    public static void dispose(final HttpEntity entity)
    {
        if (entity != null)
        {
            try
            {
              EntityUtils.consume(entity);
            }
            catch (IOException ignored)
            {
                // ignored
            }
        }
    }

    public static void setRequestOptions(HttpRequestBase request, HttpRequestOptions requestOptions)
    {
        request.getParams().setParameter(AllClientPNames.MAX_REDIRECTS,
                new Integer(requestOptions.getMaxRedirects()));
        request.getParams().setParameter(AllClientPNames.SO_TIMEOUT,
                new Integer(requestOptions.getSocketTimeout()));
        request.getParams().setParameter(AllClientPNames.CONNECTION_TIMEOUT,
                new Integer(requestOptions.getConnTimeout()));
        request.getParams().setParameter(AllClientPNames.ALLOW_CIRCULAR_REDIRECTS,
                Boolean.valueOf(requestOptions.getAllowCircularRedirects()));

        Map<String,String> requestHeaders = requestOptions.getRequestHeaders();
        if (requestHeaders != null)
        {
            Iterator<String> iter = requestHeaders.keySet().iterator();
            String headerName;
            while (iter.hasNext())
            {
                headerName = iter.next();
                request.addHeader(headerName,
                    (String) requestHeaders.get(headerName));
            }
        }
    }
}
