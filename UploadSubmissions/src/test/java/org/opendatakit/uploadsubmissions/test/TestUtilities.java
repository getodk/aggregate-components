/*
 * Copyright (C) 2010 University of Washington.
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
package org.opendatakit.uploadsubmissions.test;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.junit.Ignore;
import org.opendatakit.uploadsubmissions.applet.UploadWorker;
import org.opendatakit.uploadsubmissions.utils.IHttpClientFactory;

/**
 * In addition to providing useful test utilities to set up 
 * working copies of the submissions test files, instances
 * of this class implement the {@link IHttpClientFactory} interface
 * returning a mock HttpClient that returns each of the responses in 
 * the list of responses, in order, for each call to 
 * HttpClient.execute(HttpUriRequest)
 */
@Ignore
public class TestUtilities implements IHttpClientFactory {
	
	public static File createTemporaryDirectory()
	{
		try
		{
			File tempDir = File.createTempFile("temp", Long.toString(System.nanoTime()));
		    if(!(tempDir.delete()))
		    {
		        throw new IOException("Could not delete temp file: " + tempDir.getAbsolutePath());
		    }
	
		    if(!(tempDir.mkdir()))
		    {
		        throw new IOException("Could not create temp directory: " + tempDir.getAbsolutePath());
		    }
		    return tempDir;
		}
		catch (IOException e)
		{
			fail(e.toString());
		}
		return null;
	}

	/**
	 * Same as buildSubmission, except a new temporary directory is
	 * automatically created to hold the submission and the file returned is the
	 * submission directory instead of the parent dir.
	 * 
	 * @param submissionName
	 *            the name of the folder to contain the submission
	 * @param submissionXMLName
	 *            the name of the XML submission file (should end in .xml) -
	 *            ends up in "submissionName/submissionXMLName".
	 * @param otherFileNames
	 *            names of other files to create in the submission dir (e.g.
	 *            "pic.jpg", "randomfile.exe")
	 * @return a File representing the newly created submission
	 * @throws IOException
	 */
	public static File buildSingleSubmission(String submissionName, String submissionXMLName, String... otherFileNames)
	{
		File parentDirectory = buildSubmission(createTemporaryDirectory(), submissionName, submissionXMLName, otherFileNames);
		return new File(parentDirectory.getAbsolutePath() + File.separator + submissionName);
	}
	
	/**
	 * Builds a submission under the specified parentDirectory.
	 * 
	 * @param parentDirectory
	 *            the directory to create the submission under
	 * @param submissionName
	 *            the name of the folder to contain the submission
	 * @param submissionXMLName
	 *            the name of the XML submission file (should end in .xml) -
	 *            ends up in "submissionName/submissionXMLName".
	 * @param otherFileNames
	 *            names of other files to create in the submission dir (e.g.
	 *            "pic.jpg", "randomfile.exe")
	 * @return the parentDirectory
	 * @throws IOException
	 */
	public static File buildSubmission(File parentDirectory, String submissionName, String submissionXMLName, String... otherFileNames)
	{
		try
		{
			if (!parentDirectory.exists())
			{
				throw new IllegalArgumentException("parentDirectory: " + parentDirectory + " must exist already.");
			}
			else
			{
				File submissionFolder = new File(parentDirectory.getAbsolutePath() + File.separator + submissionName);
				submissionFolder.mkdir();
				if (!submissionXMLName.equals(""))
				{
					File submissionXMLFile = new File(submissionFolder.getAbsolutePath() + File.separator + submissionXMLName);
					submissionXMLFile.createNewFile();
				}
				for (String otherFileName : otherFileNames)
				{
					if (!otherFileName.equals(""))
					{
						File otherFile = new File(submissionFolder.getAbsolutePath() + File.separator + otherFileName);
						otherFile.createNewFile();
					}
				}
			}
		}
		catch (IOException e)
		{
			fail(e.toString());
		}
		return parentDirectory;
	}

	/**
	 * Builds a set of submissions under a freshly created temporary directory.
	 * Returns this directory, the submissions parent directory (i.e. the
	 * directory containing all the submission directories)
	 * 
	 * @param submissionCountStart the start of the count for submissions
	 * @param submissionCountEnd the end of the count for submissions
	 * @param submissionNamePrefix the prefix to use to name all the submissions (submission names go from 'submissionPrefix + submissionCountStart' --> 'submissionPrefix + submissionCountEnd')
	 * @param submissionXMLName the name of the submission XML file to create in each submission (should end in .xml)
	 * @param otherFileNames other files to create under each submission directory
	 * @return a File representing the submission parent directory
	 */
	public static File buildSubmissionSet(int submissionCountStart, int submissionCountEnd, String submissionNamePrefix, String submissionXMLName, String... otherFileNames)
	{
		File tempDir = TestUtilities.createTemporaryDirectory();
		for (int i = submissionCountStart; i <= submissionCountEnd; i++)
		{
			TestUtilities.buildSubmission(tempDir, submissionNamePrefix + i, submissionXMLName, otherFileNames);
		}
		return tempDir;
	}

	public static HttpParams getHttpParams() {

        // configure connection
	    HttpParams params = new BasicHttpParams();
	    HttpConnectionParams.setConnectionTimeout(params, UploadWorker.CONNECTION_TIMEOUT);
	    HttpConnectionParams.setSoTimeout(params, UploadWorker.CONNECTION_TIMEOUT);
	    HttpClientParams.setRedirecting(params, false);

	    return params;
	}

	public static HttpResponse buildSubmissionResponse(URL serverUrl, int status, String body) {
		ProtocolVersion pv = HttpVersion.HTTP_1_1;
		HttpContext ctxt = new BasicHttpContext();
		DefaultHttpResponseFactory drf = new DefaultHttpResponseFactory();
		StatusLine sl = new BasicStatusLine(pv, status, body);
		HttpResponse r = drf.newHttpResponse(sl,ctxt);

		String path = serverUrl.getPath();
		int idx = path.lastIndexOf("/");
		if ( idx != -1 ) {
			path = path.substring(0,idx);
		}
		if ( !path.startsWith("/") ) {
			path = "/" + path;
		}
		
		String serverString = serverUrl.getHost();
		if (( serverUrl.getPort() == -1 ) ||
			( serverUrl.getProtocol().compareToIgnoreCase("http") == 0 &&
			  serverUrl.getPort() == 80 ) ||
			( serverUrl.getProtocol().compareToIgnoreCase("https") == 0 &&
			  serverUrl.getPort() == 443 )) {
			serverString += path;
		} else {
			serverString += ":" + serverUrl.getPort() + path;
		}
		
		if ( status == 201 ) {
			r.addHeader("Location", serverString);
		}
		
		return r;
	}
	
	private static class HttpConnectionManagerImpl implements ClientConnectionManager {

		@Override
		public void closeExpiredConnections() {
		}

		@Override
		public void closeIdleConnections(long idletime, TimeUnit tunit) {
		}

		@Override
		public SchemeRegistry getSchemeRegistry() {
			return null;
		}

		@Override
		public void releaseConnection(ManagedClientConnection conn,
				long validDuration, TimeUnit timeUnit) {
		}

		@Override
		public ClientConnectionRequest requestConnection(HttpRoute route,
				Object state) {
			return null;
		}

		@Override
		public void shutdown() {
		}
	}
	
	List<HttpResponse> responses = new LinkedList<HttpResponse>();
	
	public TestUtilities(List<HttpResponse> responses) {
		this.responses.addAll(responses);
	}
	
	@Override
	public HttpClient getHttpClient(HttpParams params) {
		HttpClient mockHttpClient = null;
		try
		{
			HttpResponse r = responses.remove(0);
			mockHttpClient = mock(HttpClient.class);
			when(mockHttpClient.execute(any(HttpUriRequest.class))).thenReturn(r);
			when(mockHttpClient.getConnectionManager()).thenReturn(new HttpConnectionManagerImpl());
		}
		catch(Exception e)
		{
			fail(e.toString());
		}
		return mockHttpClient;
	}
}
