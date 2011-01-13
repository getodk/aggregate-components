package org.opendatakit.uploadform;
/*
 * Copyright (C) 2010 University of Washington
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


import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

/**
 * The implementation of the Briefcase functionality which uses ODK
 * Aggregate's /csvFragment and /binaryData interfaces to 
 * extract data from Aggregate into one or more local csv files and 
 * multiple local binary files.
 * 
 * @author mitchellsundt@gmail.com
 */
public class FormUpload {
	
	/**
	 * Notification interface to inform our creator what 
	 * URL we are currently fetching data from. 
	 */
	public interface ActionListener {
		/**
		 * Called just before a URL connection is attempted.
		 * 
		 * @param summary of what we are uploading.
		 */
		public void beforeUploadUrl( String summary ); 
	}

	/** version of this application */
	public static final String APP_VERSION = "1.0";

    private static long MAX_BYTES = 1048576 - 1024; // 1MB less 1KB overhead
    private static final int CONNECTION_TIMEOUT = 30000;

	/** mapping of file extensions to content type for binary data */
	private static final Map<String, String> mediaExtToMimeType;
	static {
		mediaExtToMimeType = new HashMap<String, String>();
		mediaExtToMimeType.put(".xml", "text/xml");
		mediaExtToMimeType.put(".html", "text/html");
		mediaExtToMimeType.put(".jpeg", "image/jpeg");
		mediaExtToMimeType.put(".jpg", "image/jpeg");
		mediaExtToMimeType.put(".png", "image/png");
		mediaExtToMimeType.put(".3gpp", "audio/3gpp");
		mediaExtToMimeType.put(".wav", "audio/x-wav");
		mediaExtToMimeType.put(".3gp", "video/3gpp");
		mediaExtToMimeType.put(".mp4", "video/mp4");
	}

	/** final path element for uploading Xform definitions on ODK Aggregate 1.0 */
	private static final String XFORM_UPLOAD_URL_PATH = "admin/upload";

	/**
	 * Parameters on a csvFragment request
	 */

	/** parameter to specify the key set reference to fetch */
	public static final String FORM_ID = "formId";

	/**
	 * Pieces of a HTTP request
	 */
	/** slash */
	private static final String SLASH = "/";

	/***************************************************************************
	 * Member variables
	 **************************************************************************/

	/**
	 * Constants across all fetch actions.
	 */

	/** the base URL of the server. e.g., http://localhost:8888/App/ */
	private final String serverUrl;
	/** xform filename */
	private final File xformFilename;
	/** xform media directory */
	private final List<File> xformMediaFileList;
	/** callback for reporting activity to the ui layer */
	private final ActionListener uiNotify;
	

	/**
	 * Formatter for directory-of-file-related messages.
	 * 
	 * @param format
	 *            valid MessageFormat string where {0} will be substituted with
	 *            the path of the dof.
	 * @param dof
	 *            File object of the directory-or-file
	 * @return formatted string
	 */
	static String dofExceptionString(String format, File dof) {
		String path = null;
		try {
			path = dof.getCanonicalPath();
		} catch (IOException eIgnore) {
			path = dof.getPath();
		}
		Object[] args = { path };
		return MessageFormat.format(format, args);
	}


	/**
	 * Constructor.
	 * 
	 * Throws an exception if the directory structure rooted at the
	 * destinationDirectoryName cannot be created or accessed or if the manifest
	 * file within that directory structure exists or cannot be created.
	 * 
	 * @param serverUrl
	 * @param xformFullFilename
	 * @param uiUpdate a listener for reporting what we are currently working on...
	 * @throws IllegalArgumentException
	 */
	public FormUpload(final String serverUrlArg,
			final String xformFullFilename,
			ActionListener uiNotifyArg ) {

		uiNotify = uiNotifyArg; 
		
		String workingServerUrl;
		// ensure that serverUrl looks like a http string and ends in slash
		if (!serverUrlArg.endsWith(SLASH)) {
			workingServerUrl = serverUrlArg + SLASH;
		} else {
			workingServerUrl = serverUrlArg;
		}

		try {
			URL u = new URL(workingServerUrl);
			if (!u.getProtocol().equals("http")
					&& !u.getProtocol().equals("https")) {
				throw new IllegalArgumentException("server URL (" + workingServerUrl
						+ ") must begin either http: or https:");
			}
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("server URL (" + workingServerUrl
					+ " is invalid", e);
		}
		serverUrl = workingServerUrl + XFORM_UPLOAD_URL_PATH;
		
		// ensure that the xform file exists.
		xformFilename = new File(xformFullFilename);

		if (xformFilename.exists()) {
			if (!xformFilename.isFile() ) {
				throw new IllegalArgumentException(dofExceptionString(
						"xform filename ({0}) is not a file",
						xformFilename));
			}
			if (!xformFullFilename.endsWith(".xml")) {
				throw new IllegalArgumentException(dofExceptionString(
						"xform filename ({0}) does not end in .xml",
						xformFilename));
			}
		} else {
			throw new IllegalArgumentException(dofExceptionString(
						"xform filename ({0}) does not exist",
						xformFilename));
		}

		String xformMediaDirName = xformFilename.getName();
		// strip extension and add "-media"
		xformMediaDirName = xformMediaDirName.substring(0,
								xformMediaDirName.lastIndexOf(".xml")) +
								"-media";
		
		File xformMediaDirectory = new File(xformFilename.getParentFile(),
								xformMediaDirName);

		List<File> mediaFileList = Collections.emptyList();
		if ( xformMediaDirectory.exists()) {
			if (!xformMediaDirectory.isDirectory()) {
				throw new IllegalArgumentException(dofExceptionString(
						"xform media directory ({0}) is not a directory",
						xformMediaDirectory));
			}
			mediaFileList = Arrays.asList(xformMediaDirectory.listFiles());
		}
		
		xformMediaFileList = mediaFileList;
	}

    private void publishProgress(String summary) {
    	uiNotify.beforeUploadUrl(summary);
    }
    
    public void uploadXForm() throws ClientProtocolException, IOException {
	
	    publishProgress("starting");
	
	    // configure connection
	    HttpParams params = new BasicHttpParams();
	    HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
	    HttpConnectionParams.setSoTimeout(params, CONNECTION_TIMEOUT);
	    HttpClientParams.setRedirecting(params, false);
	
	    int postCount = 1;
	    List<File> current = xformMediaFileList;
	    for (;;) {
		    // setup client
		    DefaultHttpClient httpclient = new DefaultHttpClient(params);

		    long messageSize = 0L;
		    List<File> remaining = new ArrayList<File>();
		    HttpPost httppost = new HttpPost(serverUrl);
		
		    // mime post
		    MultipartEntity entity = new MultipartEntity();
		    // the xform itself...
		    {
		        FileBody fb;
		        fb = new FileBody(xformFilename, "text/xml");
		        messageSize += fb.getContentLength();
		        entity.addPart("form_def_file", fb);
		    }
	
		    boolean firstMediaFile = true;
		    boolean oversizeRequest = false;
		    // add media files
		    for (File f : current) {
		        FileBody fb;
		        String extension = f.getName().substring(f.getName().lastIndexOf("."));
		        String mimeType = mediaExtToMimeType.get(extension);
		        if ( mimeType == null ) {
		        	throw new IllegalStateException("Unknown file extension: " + extension);
		        }
		        fb = new FileBody(f, mimeType);
		        if ( oversizeRequest ||
		        	( !firstMediaFile && 
		        	(fb.getContentLength() > (MAX_BYTES - messageSize)) ) ) {
		        	oversizeRequest = true;
		        	remaining.add(f);
		        } else {
			        messageSize += fb.getContentLength();
		        	entity.addPart(f.getName(), fb);
		        }
		        firstMediaFile = false;
		    }
		    httppost.setEntity(entity);
		
		    publishProgress("posting (" + Integer.toString(postCount++) + ")");
			// prepare response and return uploaded
		    HttpResponse response = null;
	        response = httpclient.execute(httppost);
		
		    // check response.
		    // TODO: This isn't handled correctly.
		    String serverLocation = null;
		    Header[] h = response.getHeaders("Location");
		    if (h != null && h.length > 0) {
		        serverLocation = h[0].getValue();
		    } else {
		        // something should be done here...
		        throw new IllegalStateException("Location header was absent");
		    }
		    int responseCode = response.getStatusLine().getStatusCode();
		    Logger.getLogger(FormUpload.class.getName()).info(
		    			"Response code:" + Integer.toString(responseCode));
		
		    // verify that your response came from a known server
		    if (!serverUrl.contains(serverLocation) || responseCode != 201) {
		        throw new IllegalStateException("Failed to receive success response during upload: " +
		        		Integer.toString(responseCode));
		    }
		    
		    if ( remaining.isEmpty() ) break;
		    current = remaining;
	    }
    }
}
