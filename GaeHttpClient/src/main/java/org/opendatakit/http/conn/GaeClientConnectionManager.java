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

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.params.HttpParams;

/**
 * Rewrite of the Apache HttpClient 4.0.3 SingleClientConnManager
 * to use the URLFetchService under its covers for issuing a request.
 * 
 * This rewrite supports http: and https: connections to the remote
 * server.  It also supports expect-continue interactions if the 
 * underlying UrlFetchService mechanism honors them.
 * 
 * @author mitchellsundt@gmail.com
 *
 */
public class GaeClientConnectionManager  implements ClientConnectionManager {
	static public final SchemeRegistry DEFAULT_SCHEME_REGISTRY;
	
	static {
		DEFAULT_SCHEME_REGISTRY = new SchemeRegistry();
		SocketFactory f = new SocketFactory() {
			@Override
			public Socket connectSocket(Socket sock, String host, int port,
					InetAddress localAddress, int localPort, HttpParams params)
					throws IOException, UnknownHostException,
					ConnectTimeoutException {
				return null;
			}

			@Override
			public Socket createSocket() throws IOException {
				return null;
			}

			@Override
			public boolean isSecure(Socket sock)
					throws IllegalArgumentException {
				return false;
			}
		};
		DEFAULT_SCHEME_REGISTRY.register(new Scheme("http",f, 80));
		DEFAULT_SCHEME_REGISTRY.register(new Scheme("http",f, 8080));
		DEFAULT_SCHEME_REGISTRY.register(new Scheme("https",f, 443));
		DEFAULT_SCHEME_REGISTRY.register(new Scheme("https",f, 8443));
	}

    private final Log log = LogFactory.getLog(getClass());

    /** The schemes supported by this connection manager. */
    protected final SchemeRegistry schemeRegistry; 

    /** URLFetchService handles all lifetime management */

    /** Indicates whether this connection manager is shut down. */
    protected volatile boolean isShutDown;

    /**
     * Creates a new simple connection manager.
     *
     * @param params    the parameters for this manager
     * @param schreg    the scheme registry, or
     *                  <code>null</code> for the default registry
     */
    public GaeClientConnectionManager(HttpParams params,
                                   SchemeRegistry schreg) {
        if (schreg == null) {
            throw new IllegalArgumentException
                ("Scheme registry must not be null.");
        }
        this.schemeRegistry  = schreg;
        this.isShutDown      = false;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            shutdown();
        } finally { // Make sure we call overridden method even if shutdown barfs
            super.finalize();
        }
    }

    public SchemeRegistry getSchemeRegistry() {
        return this.schemeRegistry;
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

    public final ClientConnectionRequest requestConnection(
            final HttpRoute route,
            final Object state) {
        
        return new ClientConnectionRequest() {
            
            public void abortRequest() {
                // Nothing to abort, since requests are immediate.
            }
            
            public ManagedClientConnection getConnection(
                    long timeout, TimeUnit tunit) {
                return GaeClientConnectionManager.this.getConnection(
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
    public synchronized ManagedClientConnection getConnection(HttpRoute route, Object state) {
        if (route == null) {
            throw new IllegalArgumentException("Route may not be null.");
        }
        assertStillUp();

        if (log.isDebugEnabled()) {
            log.debug("Get connection for route " + route);
        }

        return new GaeManagedClientConnection(route, state);
    }

    public synchronized void releaseConnection(
    		ManagedClientConnection conn, 
    		long validDuration, TimeUnit timeUnit) {
        assertStillUp();
        // lifetimes are handled by URLFetchService
    }
    
    public synchronized void closeExpiredConnections() {
        // lifetimes are handled by URLFetchService
    }

    public synchronized void closeIdleConnections(long idletime, TimeUnit tunit) {
        assertStillUp();
        // lifetimes are handled by URLFetchService
    }

    public synchronized void shutdown() {
        this.isShutDown = true;
    }

    /**
     * @deprecated no longer used
     */
    @Deprecated
    protected synchronized void revokeConnection() {
    }
}
