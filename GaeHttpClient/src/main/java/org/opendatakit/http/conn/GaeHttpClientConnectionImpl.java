package org.opendatakit.http.conn;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;

public class GaeHttpClientConnectionImpl implements HttpClientConnection {

    private static final Log logger = LogFactory.getLog(GaeHttpClientConnectionImpl.class);
	
	/** The state object associated with this connection (unused) */
	@SuppressWarnings("unused")
	private Object state;
	
	/** The route of this connection (unused). */
	@SuppressWarnings("unused")
	private HttpRoute route;
	
	/** The context for the open() request (unused) */
	@SuppressWarnings("unused")
	private HttpContext context;
	
	@SuppressWarnings("unused")
	private ConnectionConfig connectionConfig;
	
	private SocketConfig socketConfig;
	
	private RequestConfig requestConfig;
	
	/** The target host of this connection. */
	private HttpHost targetHost;
	
	/** The request to send */
	private HttpRequest request = null;

	/** The expect-continue headers (because we strip them from request */
	private Header[] expectContinueHeaders = null;
	
	/** The returned response */
	private com.google.appengine.api.urlfetch.HTTPResponse response = null;
	
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
	
    /**
     * @throws IllegalStateException    if this manager is shut down
     */
	private void assertNotConnected() throws IllegalStateException {
       if (!reusable) {
           throw new IllegalStateException("Connection is already open.");
       }
       if (broken) {
       	throw new IllegalStateException("Connection is not cleanly closed.");
       }
	}
		
	public void connect(HttpRoute route, SocketConfig socketConfig, ConnectionConfig connectionConfig, RequestConfig requestConfig, HttpContext context) throws IOException {
		assertNotConnected();
		// mark as non-reusable (we are open...)
		reusable = false;
		if ( route != null ) {
			this.route = route;
			this.targetHost = route.getTargetHost();
		}
		this.socketConfig = socketConfig;
		this.connectionConfig = connectionConfig;
		this.requestConfig = requestConfig;
		this.context = context;
	}

	public GaeHttpClientConnectionImpl(HttpRoute route, Object state) {
		this.route = route;
		if ( route != null ) {
			this.targetHost = route.getTargetHost();
		} else {
			this.targetHost = null;
		}
		this.state = state;
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
		return socketConfig.getSoTimeout();
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
		socketConfig = SocketConfig.copy(socketConfig).setSoTimeout(milliseconds).build();
	}

	@Override
	public void shutdown() throws IOException {
		reusable = false;
		broken = true;
	}

	@Override
	public void flush() throws IOException {
		// flush is always called by 
		// org.apache.http.protocol.HttpRequestExecutor.doSendRequest
		
		// Build and issue the URLFetch request here.
		URLFetchService service = URLFetchServiceFactory.getURLFetchService();
		
		boolean redirect = requestConfig.isRedirectsEnabled();
		@SuppressWarnings("unused")
        boolean authenticate = requestConfig.isAuthenticationEnabled();
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
		int deadline = 0;
		int msWaitForContinue = socketConfig.getSoTimeout();
		if ( expectContinueHeaders == null ) {
			msWaitForContinue = 0;
		}
		int soTimeout = socketConfig.getSoTimeout();
		int connTimeout = requestConfig.getConnectTimeout();
		if ( soTimeout <= 0 || connTimeout <= 0  ) {
			deadline = 0; // wait forever...
		} else {
			int maxDelay = Math.max( Math.max(soTimeout, msWaitForContinue), connTimeout);
			deadline = soTimeout + maxDelay;
		}
		
		if ( deadline > 0 ) {
			logger.info("URLFetch timeout (socket + connection) (ms): " + deadline);
			f.setDeadline(new Double(0.001 * (double) deadline));
		}
		f.validateCertificate();

		com.google.appengine.api.urlfetch.HTTPMethod method;
		if ( request instanceof HttpUriRequest ) {
			String name = ((HttpUriRequest) request).getMethod();
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
	public boolean isResponseAvailable(int timeout) throws IOException {
		return !reusable && !broken && (response != null);
	}

	@Override
	public void receiveResponseEntity(HttpResponse resp) throws HttpException, IOException {
		if ( resp == null ) {
			throw new IllegalArgumentException("HttpResponse cannot be null");
		}
		if ( response == null ) {
			throw new IllegalStateException("no response avaliable");
		}
	
		byte[] byteArray = response.getContent();
		if ( byteArray != null ) {
			Header[] headers = resp.getAllHeaders();
			ByteArrayEntity entity = new ByteArrayEntity(response.getContent());
			for ( Header h : headers ) {
				if ( h.getName().equalsIgnoreCase(HTTP.CONTENT_TYPE) ) {
					entity.setContentType(h.getValue());
				}
				if ( h.getName().equalsIgnoreCase(HTTP.CONTENT_ENCODING) ) {
					entity.setContentEncoding(h.getValue());
				}
			}
			resp.setEntity(entity);
		}
	}

	@Override
	public HttpResponse receiveResponseHeader() throws HttpException, IOException {
		if ( response == null ) {
			throw new IllegalStateException("no response avaliable");
		}
		// we don't have access to the protocol version, so assume it is Http 1.1
		HttpResponse resp = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1),
				response.getResponseCode(), null);
		
		for ( com.google.appengine.api.urlfetch.HTTPHeader h : response.getHeaders() ) {
			final String name = h.getName();
			final String value = h.getValue();
			resp.addHeader(new BasicHeader(name, value));
		}

		return resp;
	}

	@Override
	public void sendRequestEntity(HttpEntityEnclosingRequest request) throws HttpException, IOException {
		if ( reusable && !broken ) {
			throw new IllegalStateException("Connection has not yet been opened"); 
		}
		
		if ( this.request != request ) {
			throw new IllegalStateException("Connection already sending a different request");
		}
	}

	@Override
	public void sendRequestHeader(HttpRequest request) throws HttpException, IOException {
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

}
