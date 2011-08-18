/*
 * Copyright (C) 2011 University of Washington
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

package org.opendatakit.http.conn;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.EntityEnclosingRequestWrapper;
import org.apache.http.impl.client.RequestWrapper;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;

/**
 * Implementation of a ManagedClientConnection that uses Google's
 * URLFetchService under the covers to send and receive the message.
 * Handles all message types (DELETE, GET, HEAD, PUT, POST).
 * 
 * @author mitchellsundt@gmail.com
 *
 */
public class GaeManagedClientConnection implements ManagedClientConnection {

    private static final Log logger = LogFactory.getLog(GaeManagedClientConnection.class);
	
	/** The state object associated with this connection */
	private Object state;
	
	/** The route of this connection. */
	private HttpRoute route;
	
	/** The context for the open() request (unused) */
	private HttpContext context = null;
	
	/** The parameters on the open() request */
	private HttpParams params = null;
	
	/** The target host of this connection. */
	private HttpHost targetHost;
	
	/** The request to send */
	private HttpRequest request = null;

	/** The expect-continue headers (because we strip them from request */
	private Header[] expectContinueHeaders = null;
	
	/** The returned response */
	private com.google.appengine.api.urlfetch.HTTPResponse response = null;
	
	/** default is to not set this */
	private int timeoutMilliseconds = -1;
	
	/** The communications are in a reusable state (i.e., not open) */
	private boolean reusable = true;
	private boolean broken = false;

	/**
	 * Reset the state of this object (for reuse)
	 */
    private void reset() {
    	request = null;
    	expectContinueHeaders = null;
    	response = null;
    	reusable = true;
    	broken = false;
    }

	GaeManagedClientConnection(HttpRoute route, Object state ) {
		this.route = route;
		if ( route != null ) {
			this.targetHost = route.getTargetHost();
		} else {
			this.targetHost = null;
		}
		this.state = state;
	}
	
    /**
     * Asserts that this connection is not open.
     *
     * @throws IllegalStateException    if this manager is shut down
     */
    protected final void assertNotOpen() throws IllegalStateException {
        if (!reusable)
            throw new IllegalStateException("Connection is already open.");
        if (broken)
        	throw new IllegalStateException("Connection is not cleanly closed.");
    }

    public final HttpHost getTargetHost() {
        return this.targetHost;
    }

	@Override
	public HttpRoute getRoute() {
		return route;
	}

	@Override
	public javax.net.ssl.SSLSession getSSLSession() {
		return null;
	}

	@Override
	public Object getState() {
		return state;
	}

	@Override
	public boolean isMarkedReusable() {
		return reusable && !broken;
	}

	@Override
	public boolean isSecure() {
		return route.isSecure();
	}

	@Override
	public void layerProtocol(HttpContext context, HttpParams params)
			throws IOException {
		// handled by URLFetch layer
		throw new IllegalStateException("not supported");
	}

	@Override
	public void markReusable() {
		reset();
	}

	@Override
	public void open(HttpRoute route, HttpContext context, HttpParams params)
			throws IOException {
		assertNotOpen();
		// mark as non-reusable (we are open...)
		reusable = false;
		if ( route != null ) {
			this.route = route;
			this.targetHost = route.getTargetHost();
		}
		this.context = context;
		this.params = params;
	}

	@Override
	public void setIdleDuration(long duration, TimeUnit unit) {
		// don't care -- connection is never reused
	}

	@Override
	public void setState(Object state) {
		this.state = state;
	}

	@Override
	public void tunnelProxy(HttpHost next, boolean secure, HttpParams params)
			throws IOException {
		// handled by URLFetch layer
		throw new IllegalStateException("not supported");
	}

	@Override
	public void tunnelTarget(boolean secure, HttpParams params)
			throws IOException {
		// handled by URLFetch layer
		throw new IllegalStateException("not supported");
	}

	@Override
	public void unmarkReusable() {
		reusable = false;
		broken = false;
	}

	@Override
	public void flush() throws IOException {
		// flush is always called by 
		// org.apache.http.protocol.HttpRequestExecutor.doSendRequest
		
		// Build and issue the URLFetch request here.
		URLFetchService service = URLFetchServiceFactory.getURLFetchService();
		
		boolean redirect = HttpClientParams.isRedirecting(params);
		boolean authenticate = HttpClientParams.isAuthenticating(params);
		// TODO: verify that authentication is handled by URLFetchService...
		
		// default is to throw an exception on a overly-large request
		// follow redirects (e.g., to https), and to validate server
		// certificates.
		com.google.appengine.api.urlfetch.FetchOptions f = 
			com.google.appengine.api.urlfetch.FetchOptions.Builder.withDefaults();
		f.disallowTruncate();
		f.validateCertificate();
		if ( redirect ) {
			f.followRedirects();
		} else {
			f.doNotFollowRedirects();
		}
		
		// set a deadline if we have a wait-for-continue limit
		// in an expectContinue situation 
		// or a timeout value set on the connection.
		HttpParams params = request.getParams();
		int deadline = 0;
		int msWaitForContinue = params.getIntParameter(
                					CoreProtocolPNames.WAIT_FOR_CONTINUE, 2000);
		if ( expectContinueHeaders == null ) {
			msWaitForContinue = 0;
		}
		int soTimeout = org.apache.http.params.HttpConnectionParams.getSoTimeout(params);
		int connTimeout = org.apache.http.params.HttpConnectionParams.getConnectionTimeout(params);
		if ( soTimeout <= 0 || connTimeout <= 0  ) {
			deadline = 0; // wait forever...
		} else {
			int maxDelay = Math.max( Math.max(timeoutMilliseconds, msWaitForContinue), connTimeout);
			deadline = soTimeout + maxDelay;
		}
		
		if ( deadline > 0 ) {
			logger.info("URLFetch timeout (socket + connection) (ms): " + deadline);
			f.setDeadline(new Double(0.001 * (double) deadline));
		}
		f.validateCertificate();

		com.google.appengine.api.urlfetch.HTTPMethod method;
		if ( request instanceof HttpGet ) {
			method = HTTPMethod.GET;
		} else if ( request instanceof HttpPut ) {
			method = HTTPMethod.PUT;
		} else if ( request instanceof HttpPost ) {
			method = HTTPMethod.POST;
		} else if ( request instanceof HttpHead ) {
			method = HTTPMethod.HEAD;
		} else if ( request instanceof HttpDelete ) {
			method = HTTPMethod.DELETE;
		} else if ( request instanceof EntityEnclosingRequestWrapper ) {
			String name = ((EntityEnclosingRequestWrapper) request).getMethod();
			method = HTTPMethod.valueOf(name);
		} else if ( request instanceof RequestWrapper ) {
			String name = ((RequestWrapper) request).getMethod();
			method = HTTPMethod.valueOf(name);
		} else {
			throw new IllegalStateException("Unrecognized Http request method");
		}
		
		
		// we need to construct the URL for the request
		// to the target host.  The request line, for, e.g., 
		// a get, needs to be added to the URL.
		URL url = new URL( targetHost.getSchemeName(),
							targetHost.getHostName(), 
							targetHost.getPort(),
							request.getRequestLine().getUri());
		
		com.google.appengine.api.urlfetch.HTTPRequest req =
			new com.google.appengine.api.urlfetch.HTTPRequest(url, method, f);

		Header[] headers = request.getAllHeaders();
		for ( Header h : headers ) {
			req.addHeader(new com.google.appengine.api.urlfetch.HTTPHeader(h.getName(), h.getValue()));
		}
		// restore the expect-continue header
		if ( expectContinueHeaders != null ) {
			for ( Header h : expectContinueHeaders ) {
				req.addHeader(new com.google.appengine.api.urlfetch.HTTPHeader(h.getName(), h.getValue()));
			}
		}

		// see if we need to copy entity body over...
		if ( request instanceof HttpEntityEnclosingRequest) {
			HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
			if ( entity != null ) {
				ByteArrayOutputStream blobStream = new ByteArrayOutputStream();
				entity.writeTo(blobStream);
				req.setPayload(blobStream.toByteArray());
			}
		}

		response = service.fetch(req);
	}

	@Override
	public boolean isResponseAvailable(int arg0) throws IOException {
		return !reusable && !broken && (response != null);
	}

	@Override
	public void receiveResponseEntity(HttpResponse resp) throws HttpException,
			IOException {
		if ( resp == null ) {
			throw new IllegalArgumentException("HttpResponse cannot be null");
		}
		if ( response == null ) {
			throw new IllegalStateException("no response avaliable");
		}

		byte[] byteArray = response.getContent();
		if ( byteArray != null ) {
			ByteArrayEntity entity = new ByteArrayEntity(response.getContent());
			entity.setContentType(resp.getFirstHeader(HTTP.CONTENT_TYPE));
			resp.setEntity(entity);
		}
	}

	@Override
	public HttpResponse receiveResponseHeader() throws HttpException,
			IOException {
		if ( response == null ) {
			throw new IllegalStateException("no response avaliable");
		}
		// we don't have access to the protocol version, so assume it is Http 1.1
		HttpResponse resp = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1),
				response.getResponseCode(), null);
		
		for ( com.google.appengine.api.urlfetch.HTTPHeader h : response.getHeaders() ) {
			resp.addHeader(new BasicHeader(h.getName(), h.getValue()));
		}

		return resp;
	}

	@Override
	public void sendRequestEntity(HttpEntityEnclosingRequest request)
			throws HttpException, IOException {
		if ( reusable && !broken ) {
			throw new IllegalStateException("Connection has not yet been opened"); 
		}
		
		if ( this.request != request ) {
			throw new IllegalStateException("Connection already sending a different request");
		}
	}

	@Override
	public void sendRequestHeader(HttpRequest request) throws HttpException,
			IOException {
		if ( reusable && !broken ) {
			throw new IllegalStateException("Connection has not yet been opened"); 
		}
		this.request = request;
		if ( request instanceof HttpEntityEnclosingRequest) {
			HttpEntityEnclosingRequest req = (HttpEntityEnclosingRequest) request;
			expectContinueHeaders = req.getHeaders(HTTP.EXPECT_DIRECTIVE);
			req.removeHeaders(HTTP.EXPECT_DIRECTIVE);
		}
	}

	@Override
	public void close() throws IOException {
		reset();
	}

	@Override
	public HttpConnectionMetrics getMetrics() {
		// none available...
		return null;
	}

	@Override
	public int getSocketTimeout() {
		return timeoutMilliseconds;
	}

	@Override
	public boolean isOpen() {
		return !reusable;
	}

	@Override
	public boolean isStale() {
		// assume it isn't...
		return false;
	}

	@Override
	public void setSocketTimeout(int milliseconds) {
		this.timeoutMilliseconds = milliseconds;
	}

	@Override
	public void shutdown() throws IOException {
		reusable = false;
		broken = true;
	}

	@Override
	public InetAddress getLocalAddress() {
		return route.getLocalAddress();
	}

	@Override
	public int getLocalPort() {
		// not available...
		return 0;
	}

	@Override
	public InetAddress getRemoteAddress() {
		// not available...
		return null;
	}

	@Override
	public int getRemotePort() {
		HttpHost host = route.getTargetHost();
		return host.getPort();
	}

	@Override
	public void abortConnection() throws IOException {
		reset();
		reusable = false;
		broken = true;
	}

	@Override
	public void releaseConnection() throws IOException {
		reset();
	}
}
