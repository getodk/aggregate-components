/*
 * Copyright (C) 2011 University of Washington.
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

package org.opendatakit.briefcase.model;

import org.apache.http.client.HttpClient;
import org.apache.http.protocol.HttpContext;

public class ServerConnectionInfo {
  private String url;
  private final String username;
  private final char[] password;
  boolean isOpenRosaServer = false;
  HttpClient httpClient = null;
  HttpContext httpContext = null;

  public ServerConnectionInfo(String url, String username, char[] cs) {
    this.url = url;
    this.username = username;
    this.password = cs;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUsername() {
    return username;
  }

  public char[] getPassword() {
    return password;
  }

  public HttpClient getHttpClient() {
    return httpClient;
  }

  public void setHttpClient(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  public HttpContext getHttpContext() {
    return httpContext;
  }

  public void setHttpContext(HttpContext httpContext) {
    this.httpContext = httpContext;
  }

  public boolean isOpenRosaServer() {
    return isOpenRosaServer;
  }

  public void setOpenRosaServer(boolean isOpenRosaServer) {
    this.isOpenRosaServer = isOpenRosaServer;
  }
}
