package org.openid4java.util;

import org.apache.http.conn.ClientConnectionManager;

import com.google.inject.ImplementedBy;

/**
 * This interface can be overridden to provide a suitable 
 * Google AppEngine implementation.
 *  
 * @author mitchellsundt@gmail.com
 *
 */
@ImplementedBy(DefaultClientConnectionManagerFactory.class)
public interface ClientConnectionManagerFactory {

  public ClientConnectionManager getConnectionManager();
  
}
