package org.opendatakit.uploadsubmissions.utils;

import org.apache.http.client.HttpClient;
import org.apache.http.params.HttpParams;

public interface IHttpClientFactory {
	/**
	 * Return an http client configured with the given parameters.
	 * The client is not thread-safe.  Abstracted as a factory
	 * interface in support of testing.
	 *
	 * @param params the HttpParams for the client and its connections
	 * @return new HttpClient instance
	 */
	public HttpClient getHttpClient(HttpParams params);	
}
