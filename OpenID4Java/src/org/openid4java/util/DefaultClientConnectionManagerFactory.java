package org.openid4java.util;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;

public class DefaultClientConnectionManagerFactory implements ClientConnectionManagerFactory {
  private SchemeRegistry registry;
  
  public DefaultClientConnectionManagerFactory() {
    
    registry = new SchemeRegistry();

    registry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
    registry.register(new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));
  }

  public ClientConnectionManager getConnectionManager() {
    return new ThreadSafeClientConnManager(registry);
  }

}
