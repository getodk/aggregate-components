package org.opendatakit.briefcase.model;

import org.apache.http.client.HttpClient;
import org.apache.http.protocol.HttpContext;

public class ServerConnectionInfo {
	private String url;
	private final String username;
	private final String password;
	boolean isOpenRosaServer = false;
	HttpClient httpClient = null;
	HttpContext httpContext = null;
	
	public ServerConnectionInfo(String url, String username, String password) {
		this.url = url;
		this.username = username;
		this.password = password;
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

	public String getPassword() {
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

	public HttpContext setHttpContext(HttpContext httpContext) {
		return httpContext;
	}

	public boolean isOpenRosaServer() {
		return isOpenRosaServer;
	}

	public void setOpenRosaServer(boolean isOpenRosaServer) {
		this.isOpenRosaServer = isOpenRosaServer;
	}
}
