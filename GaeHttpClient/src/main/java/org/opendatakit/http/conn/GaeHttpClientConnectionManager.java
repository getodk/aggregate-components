package org.opendatakit.http.conn;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpClientConnection;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.protocol.HttpContext;

public class GaeHttpClientConnectionManager implements HttpClientConnectionManager {

    /** URLFetchService handles all lifetime management */

    /** Indicates whether this connection manager is shut down. */
    protected volatile boolean isShutDown;

    private final Log log = LogFactory.getLog(getClass());

    private SocketConfig defaultSocketConfig;

    private ConnectionConfig defaultConnectionConfig;
    
    private RequestConfig defaultRequestConfig;
    
    public GaeHttpClientConnectionManager(SocketConfig socketConfig, ConnectionConfig connectionConfig, RequestConfig requestConfig) {
    	defaultSocketConfig = (socketConfig != null) ? socketConfig : SocketConfig.DEFAULT;
    	defaultConnectionConfig = (connectionConfig != null) ? connectionConfig : ConnectionConfig.DEFAULT;
    	defaultRequestConfig = (requestConfig != null) ? requestConfig : RequestConfig.DEFAULT;
    }
    
    public void setDefaultSocketConfig(SocketConfig defaultSocketConfig) {
    	this.defaultSocketConfig = defaultSocketConfig;
    }
    
    public void setDefaultConnectionConfig(ConnectionConfig defaultConnectionConfig) {
    	this.defaultConnectionConfig = defaultConnectionConfig;
    }
    
    @Override
    protected void finalize() throws Throwable {
        shutdown();
    }

    /**
     * Asserts that this manager is not shut down.
     *
     * @throws IllegalStateException    if this manager is shut down
     */
    protected final void assertStillUp() throws IllegalStateException {
        if (this.isShutDown)
            throw new IllegalStateException("Manager is shut down.");
    }

	@Override
	public void closeExpiredConnections() {
        assertStillUp();
        // lifetimes are handled by URLFetchService
	}

	@Override
	public void closeIdleConnections(long idleTime, TimeUnit unit) {
        assertStillUp();
        // lifetimes are handled by URLFetchService
	}

	@Override
	public void connect(HttpClientConnection conn, HttpRoute route, int connectTimeout, HttpContext context) throws IOException {
		if ( conn instanceof GaeHttpClientConnectionImpl ) {
			GaeHttpClientConnectionImpl connImpl = (GaeHttpClientConnectionImpl) conn;
			RequestConfig forThis = RequestConfig.copy(defaultRequestConfig).setConnectTimeout(connectTimeout).build();
			connImpl.connect(route, SocketConfig.copy(defaultSocketConfig).build(), defaultConnectionConfig, forThis, context);
		} else {
			throw new IllegalStateException("connecton is not an instance of GaeHttpClientConnectionImpl!");
		}
	}

	@Override
	public void releaseConnection(HttpClientConnection conn, Object newState, long lifeTime, TimeUnit unit) {
        assertStillUp();
        // lifetimes are handled by URLFetchService
	}

	@Override
	public ConnectionRequest requestConnection(
        final HttpRoute route,
        final Object state) {
    
		return new ConnectionRequest() {

			@Override
			public boolean cancel() {
	            // Nothing to abort, since requests are immediate.
				return false;
			}

			@Override
			public HttpClientConnection get(long interval, TimeUnit unit)
					throws InterruptedException, ExecutionException, ConnectionPoolTimeoutException {
	            return GaeHttpClientConnectionManager.this.getConnection(
	                    route, state);
			}
			
		};
	}

    /**
     * Obtains a connection.
     *
     * @param route     where the connection should point to
     *
     * @return  a connection that can be used to communicate
     *          along the given route
     */
    public synchronized HttpClientConnection getConnection(HttpRoute route, Object state) {
        if (route == null) {
            throw new IllegalArgumentException("Route may not be null.");
        }
        assertStillUp();

        if (log.isDebugEnabled()) {
            log.debug("Get connection for route " + route);
        }

        return new GaeHttpClientConnectionImpl(route, state);
    }

	@Override
	public void routeComplete(HttpClientConnection conn, HttpRoute route, HttpContext context) throws IOException {
		// no-op - using URLConnection
	}

	@Override
	public void shutdown() {
		this.isShutDown = true;
	}

	@Override
	public void upgrade(HttpClientConnection conn, HttpRoute route, HttpContext context) throws IOException {
		// no-op - using URLConnection
	}

}
