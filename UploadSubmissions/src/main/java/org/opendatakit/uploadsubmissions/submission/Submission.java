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
package org.opendatakit.uploadsubmissions.submission;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.opendatakit.uploadsubmissions.utils.DeleteDirectory;
import org.opendatakit.uploadsubmissions.utils.IHttpClientFactory;

/**
 * Submission represents a single submission destined for a specific ODK Aggregate server.
 * A single submission is a folder which contains a filled out xform and accompanying files.
 * 
 * @author dylan@cs.washington.edu
 *
 */
public class Submission
{
	static final String XML_MIME_TYPE;
	// exposed for unit tests...
	static final String JPEG_MIME_TYPE;
	static final String PNG_MIME_TYPE;
	
	/** mapping of file extensions to content type for binary data */
	static final Map<String, String> mediaExtToMimeType;
	static {
		XML_MIME_TYPE = "text/xml";
		JPEG_MIME_TYPE = "image/jpeg";
		PNG_MIME_TYPE = "image/png";
		mediaExtToMimeType = new HashMap<String, String>();
		mediaExtToMimeType.put(".xml", XML_MIME_TYPE);
		mediaExtToMimeType.put(".html", "text/html");
		mediaExtToMimeType.put(".jpeg", JPEG_MIME_TYPE);
		mediaExtToMimeType.put(".jpg", JPEG_MIME_TYPE);
		mediaExtToMimeType.put(".png", PNG_MIME_TYPE);
		mediaExtToMimeType.put(".3gpp", "audio/3gpp");
		mediaExtToMimeType.put(".wav", "audio/x-wav");
		mediaExtToMimeType.put(".3gp", "video/3gpp");
		mediaExtToMimeType.put(".mp4", "video/mp4");
	}
	
	public static final String FORM_PART_XML_SUBMISSION_FILE = "xml_submission_file";

	private static final String BODY_CLOSE = "</body>";
	private static final String BODY_OPEN = "<body>";

	private final IHttpClientFactory _factory;
	private final HttpParams _httpParams;
	private final URL _aggregateURL;
	private final File _submissionDir;
	private Logger _logger = Logger.getLogger(Submission.class.getName());

	/**
	 * Construct a Submission using the given http parameters, aggregate url,
	 * submission dir.
	 * 
	 * @param httpParams configuration for the HttpClient we will be creating...
	 * @param aggregateURL
	 * @param submissionDir
	 */
	public Submission(IHttpClientFactory factory, HttpParams httpParams, URL aggregateURL, File submissionDir)
	{
		_factory = factory;
		_httpParams = httpParams;
		_aggregateURL = aggregateURL;
		_submissionDir = submissionDir;
	}

	public SubmissionResult submitAndDeleteLocalCopy() 
	{
        SubmissionResult result = new SubmissionResult(_submissionDir, _aggregateURL, false);

        // prepare response and return uploaded
        HttpResponse response = null;
        String body = "";
        try 
        {
        	// TODO: expand for multi-part submissions
        	HttpClient httpClient = _factory.getHttpClient(_httpParams);
    		HttpPost httppost = buildSubmissionPost(_aggregateURL, _submissionDir);
            response = httpClient.execute(httppost);

            // check response.
            if ( response == null ) {
                // something should be done here...
            	_logger.severe("HttpResponse is null");
            	result.setSuccess(false);
            	result.setFailureReason("HttpResponse is null");
            	return result;
            }

            HttpEntity entity = response.getEntity();
            if ( entity == null ) {
            	body = "";
            } else {
	            String fullResponseBody = EntityUtils.toString(entity);
	            body = fullResponseBody;
	            int idx = fullResponseBody.indexOf(BODY_OPEN);
	            if ( idx != -1 ) {
	               	body = fullResponseBody.substring(idx+BODY_OPEN.length());
	                body = body.substring(0,body.indexOf(BODY_CLOSE));
	            }
            }
            httpClient.getConnectionManager().shutdown();
        } 
        catch (Exception e)
        {
        	e.printStackTrace();
        	result.setSuccess(false);
        	result.setFailureReason(e.getMessage());
        	return result;
        }
        
        // check response.
        int responseCode = response.getStatusLine().getStatusCode();
        _logger.info("Response code:" + responseCode);

        // verify that your response is 201
	    if (responseCode != 201) {
        	result.setSuccess(false);
        	result.setFailureReason(
        			"<b><em>" + response.getStatusLine().getReasonPhrase() +
        			"</em></b><br/>" + body);
        	return result;
	    }

        String serverLocation = null;
        Header[] h = response.getHeaders("Location");
        if (h != null && h.length > 0) {
            serverLocation = h[0].getValue();
        } else {
            // something should be done here...
        	_logger.severe("Location header was absent");
        	result.setSuccess(false);
        	result.setFailureReason("Location header was absent");
        	return result;
        }

	    // verify that your response came from a known server
	    if (!_aggregateURL.toString().contains(serverLocation)) {
        	result.setSuccess(false);
	        result.setFailureReason("Response `Location` (" + serverLocation +
	        		") does not match request URL: " + _aggregateURL.toString());
        	return result;
        }
        
        // OK the response from the server was good, so we can now
        // delete the submissions in the local directory...
		try
		{
			boolean deleted = DeleteDirectory.deleteDirectory(_submissionDir);
			if (!deleted)
			{
				result.setSuccess(false);
				result.setFailureReason("Successful upload but unable to delete submission.");
				return result;
			}
		}
		catch (SecurityException e)
		{
			result.setSuccess(false);
			result.setFailureReason(e.getMessage());
			return result;
		}

        result.setSuccess(true);
        return result;
	}

	/**
	 * Returns an org.apache.http.client.methods.HttpPost built to post to the
	 * given url. Builds a post that ODK Aggregate will recognize as a
	 * submission.
	 * 
	 * @param url
	 *            the URL of the ODK Aggregate submission servlet
	 * @param submissionDir
	 *            the submission, i.e. a File representing a directory
	 *            containing submission files
	 * @return an HttpPost
	 * @throws URISyntaxException
	 *             if the given url could not be converted to a URI for the
	 *             HttpPost
	 * @throws IllegalArgumentException
	 *             if the submissionDir is invalid, i.e. it has more than one
	 *             XML file
	 */
	protected HttpPost buildSubmissionPost(URL url, File submissionDir) throws URISyntaxException
	{
		URI uri = url.toURI();
		HttpPost post = new HttpPost(uri);
		boolean seenXML = false;

		// mime post
        MultipartEntity entity = new MultipartEntity();
        for (File f : submissionDir.listFiles()) 
        {
        	String extension = f.getName().substring(f.getName().lastIndexOf("."));
        	String mimeType = mediaExtToMimeType.get(extension);
        	if ( mimeType == null ) {
        		_logger.warning("unsupported file type, not adding file: " + f.getName());
        	} else {
        		if ( mimeType.equals(XML_MIME_TYPE) ) {
                	if (seenXML)
                	{
                		throw new IllegalArgumentException(String.format("submissionDir (%s) has more than one xml file!", submissionDir));
                	}
                	seenXML = true;
                    entity.addPart(FORM_PART_XML_SUBMISSION_FILE, new FileBody(f, mimeType));
                    _logger.info("added xml file " + f.getName());
        			
        		} else {
                    entity.addPart(f.getName(), new FileBody(f, mimeType));
                    _logger.info("added " + mimeType + " file: " + f.getName());
        		}
        	}
        }
        if (!seenXML)
        {
        	throw new IllegalArgumentException(String.format("submissionDir (%s) has no xml file!", submissionDir));
        }
        post.setEntity(entity);
        return post;
	}
}
