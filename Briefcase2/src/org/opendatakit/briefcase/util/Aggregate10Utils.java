package org.opendatakit.briefcase.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.protocol.HttpContext;
import org.opendatakit.briefcase.model.ServerConnectionInfo;

import eu.medsea.mimeutil.MimeUtil;

public class Aggregate10Utils {

	private static final Logger log = Logger.getLogger(Aggregate10Utils.class.getName());
	
	public static class ServerConnectionOutcome {
		boolean isSuccessful = false;
		boolean isCompleteTransfer = false;
		String errorMessage = "Unknown Error";
		
		public boolean isSuccessful() {
			return isSuccessful;
		}
		public boolean isCompleteTransfer() {
			return isCompleteTransfer;
		}
		public String getErrorMessage() {
			return errorMessage;
		}
	}

	private static ServerConnectionOutcome outcome;
	
	public static final ServerConnectionOutcome getOutcome() {
		return outcome;
	}

	public static final void testServerDownloadConnection( ServerConnectionInfo serverInfo ) {
		outcome = new ServerConnectionOutcome();
		
        String urlString = serverInfo.getUrl();
        if ( urlString.endsWith("/") ) {
        	urlString = urlString + "formList";
        } else {
        	urlString = urlString + "/formList";
        }

        URI u = null;
        try {
            URL url = new URL(urlString);
            u = url.toURI();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            outcome.errorMessage = "Invalid url: " + urlString + ".\nFailed with error: " + e.getMessage();
            log.severe(outcome.errorMessage);
            return;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            outcome.errorMessage = "Invalid uri: " + urlString + ".\nFailed with error: " + e.getMessage();
            log.severe(outcome.errorMessage);
            return;
        }

        HttpClient httpClient = serverInfo.getHttpClient();
        if ( httpClient == null ) {
        	httpClient = WebUtils.createHttpClient(5000);
        	serverInfo.setHttpClient(httpClient);
        }
        
        // get shared HttpContext so that authentication and cookies are retained.
        HttpContext localContext = serverInfo.getHttpContext();
        if ( localContext == null ) {
        	localContext = WebUtils.createHttpContext();
        	serverInfo.setHttpContext(localContext);
        }

        {
            // we need to issue a head request
            HttpGet httpGet = WebUtils.createOpenRosaHttpGet(u);

            // prepare response
            HttpResponse response = null;
            try {
                response = httpClient.execute(httpGet, localContext);
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                	Header[] openRosaVersions = response.getHeaders(WebUtils.OPEN_ROSA_VERSION_HEADER);
                	if ( openRosaVersions != null && openRosaVersions.length != 0 ) {
                		serverInfo.setOpenRosaServer(true);
                	}
                    Header[] locations = response.getHeaders("Location");
                    if (locations != null && locations.length == 1) {
                        try {
                            URL url = new URL(locations[0].getValue());
                            URI uNew = url.toURI();
                            if (u.getHost().equalsIgnoreCase(uNew.getHost())) {
                                // trust the server to tell us a new location
                                // ... and possibly to use https instead.
                            	String fullUrl = url.toExternalForm();
                            	int idx = fullUrl.lastIndexOf("/");
                                serverInfo.setUrl(fullUrl.substring(0,idx));
                            } else {
                                // Don't follow a redirection attempt to a different host.
                                // We can't tell if this is a spoof or not.
                            	outcome.errorMessage = "Unexpected redirection attempt to a different host: "
                                    + uNew.toString();
                            	log.severe(outcome.errorMessage);
                            	return;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            outcome.errorMessage = "Unexpected exception: " + e.getMessage();
                            return;
                        }
                    }

                    try {
                        // don't really care about the stream...
                        InputStream is = response.getEntity().getContent();
                        // read to end of stream...
                        final long count = 1024L;
                        while (is.skip(count) == count)
                            ;
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    // something is wrong...
                    try {
                        // don't really care about the stream...
                        InputStream is = response.getEntity().getContent();
                        // read to end of stream...
                        final long count = 1024L;
                        while (is.skip(count) == count)
                            ;
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    log.warning("Status code on Get request: " + statusCode);
                    outcome.errorMessage = "A network login screen may be interfering" +
        				" with the transmission to the server. Status code: " + Integer.toString(statusCode);
                    return;
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                outcome.errorMessage = "Unexpected protocol exception: " + e.getMessage();
                return;
            } catch (Exception e) {
                e.printStackTrace();
                outcome.errorMessage = "Unexpected exception: " + e.getMessage();
                return;
            }
        }
        // At this point, we may have updated the uri to use https.
        // This occurs only if the Location header keeps the host name
        // the same. If it specifies a different host name, we error
        // out.
        // 
        // And we may have set authentication cookies in our
        // cookiestore (referenced by localContext) that will enable
        // authenticated publication to the server.
        // 
        outcome.isSuccessful = true;
	}

	public static final void testServerUploadConnection( ServerConnectionInfo serverInfo ) {
		outcome = new ServerConnectionOutcome();
		
        String urlString = serverInfo.getUrl();
        if ( urlString.endsWith("/") ) {
        	urlString = urlString + "submission";
        } else {
        	urlString = urlString + "/submission";
        }

        URI u = null;
        try {
            URL url = new URL(urlString);
            u = url.toURI();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            outcome.errorMessage = "Invalid url: " + urlString + ".\nFailed with error: " + e.getMessage();
            log.severe(outcome.errorMessage);
            return;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            outcome.errorMessage = "Invalid uri: " + urlString + ".\nFailed with error: " + e.getMessage();
            log.severe(outcome.errorMessage);
            return;
        }

        HttpClient httpClient = serverInfo.getHttpClient();
        if ( httpClient == null ) {
        	httpClient = WebUtils.createHttpClient(5000);
        	serverInfo.setHttpClient(httpClient);
        }
        
        // get shared HttpContext so that authentication and cookies are retained.
        HttpContext localContext = serverInfo.getHttpContext();
        if ( localContext == null ) {
        	localContext = WebUtils.createHttpContext();
        	serverInfo.setHttpContext(localContext);
        }

        {
            // we need to issue a head request
            HttpHead httpHead = WebUtils.createOpenRosaHttpHead(u);

            // prepare response
            HttpResponse response = null;
            try {
                response = httpClient.execute(httpHead, localContext);
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 204) {
                	Header[] openRosaVersions = response.getHeaders(WebUtils.OPEN_ROSA_VERSION_HEADER);
                	if ( openRosaVersions != null && openRosaVersions.length != 0 ) {
                		serverInfo.setOpenRosaServer(true);
                	}
                    Header[] locations = response.getHeaders("Location");
                    if (locations != null && locations.length == 1) {
                        try {
                            URL url = new URL(locations[0].getValue());
                            URI uNew = url.toURI();
                            if (u.getHost().equalsIgnoreCase(uNew.getHost())) {
                                // trust the server to tell us a new location
                                // ... and possibly to use https instead.
                            	String fullUrl = url.toExternalForm();
                            	int idx = fullUrl.lastIndexOf("/");
                                serverInfo.setUrl(fullUrl.substring(0,idx));
                            } else {
                                // Don't follow a redirection attempt to a different host.
                                // We can't tell if this is a spoof or not.
                            	outcome.errorMessage = "Unexpected redirection attempt to a different host: "
                                    + uNew.toString();
                            	log.severe(outcome.errorMessage);
                            	return;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            outcome.errorMessage = "Unexpected exception: " + e.getMessage();
                            return;
                        }
                    }
                } else {
            		serverInfo.setOpenRosaServer(false);
                    // may be a server that does not handle HEAD requests
                    try {
                        // don't really care about the stream...
                        InputStream is = response.getEntity().getContent();
                        // read to end of stream...
                        final long count = 1024L;
                        while (is.skip(count) == count)
                            ;
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    log.warning("Status code on Head request: " + statusCode);
                    if (statusCode >= 200 && statusCode <= 299) {
                    	outcome.errorMessage = "A network login screen may be interfering" +
                    			" with the transmission to the server. Status code: " + Integer.toString(statusCode);
                        return;
                    }
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                outcome.errorMessage = "Unexpected protocol exception: " + e.getMessage();
                return;
            } catch (Exception e) {
                e.printStackTrace();
                outcome.errorMessage = "Unexpected exception: " + e.getMessage();
                return;
            }
        }

        // At this point, we may have updated the uri to use https.
        // This occurs only if the Location header keeps the host name
        // the same. If it specifies a different host name, we error
        // out.
        // 
        // And we may have set authentication cookies in our
        // cookiestore (referenced by localContext) that will enable
        // authenticated publication to the server.
        //
        outcome.isSuccessful = true;
	}
	
	public static final void submitToServerConnection( ServerConnectionInfo serverInfo, File instanceDir ) {
		outcome = new ServerConnectionOutcome();
		
        String urlString = serverInfo.getUrl();

        URI u = null;
        try {
            URL url = new URL(urlString);
            u = url.toURI();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            outcome.errorMessage = "Invalid url: " + urlString + ".\nFailed with error: " + e.getMessage();
            log.severe(outcome.errorMessage);
            return;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            outcome.errorMessage = "Invalid uri: " + urlString + ".\nFailed with error: " + e.getMessage();
            log.severe(outcome.errorMessage);
            return;
        }

        HttpClient httpClient = serverInfo.getHttpClient();
        if ( httpClient == null ) {
        	httpClient = WebUtils.createHttpClient(5000);
        	serverInfo.setHttpClient(httpClient);
        }
        
        // get shared HttpContext so that authentication and cookies are retained.
        HttpContext localContext = serverInfo.getHttpContext();
        if ( localContext == null ) {
        	localContext = WebUtils.createHttpContext();
        	serverInfo.setHttpContext(localContext);
        }

        boolean openRosaServer = false;
        {
            // we need to issue a head request
            HttpHead httpHead = WebUtils.createOpenRosaHttpHead(u);

            // prepare response
            HttpResponse response = null;
            try {
                response = httpClient.execute(httpHead, localContext);
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 204) {
                    Header[] locations = response.getHeaders("Location");
                    if (locations != null && locations.length == 1) {
                        try {
                            URL url = new URL(locations[0].getValue());
                            URI uNew = url.toURI();
                            if (u.getHost().equalsIgnoreCase(uNew.getHost())) {
                                openRosaServer = true;
                                // trust the server to tell us a new location
                                // ... and possibly to use https instead.
                                u = uNew;
                            } else {
                                // Don't follow a redirection attempt to a different host.
                                // We can't tell if this is a spoof or not.
                            	outcome.errorMessage = "Unexpected redirection attempt to a different host: "
                                    + uNew.toString();
                            	log.severe(outcome.errorMessage);
                            	return;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            outcome.errorMessage = "Unexpected exception: " + e.getMessage();
                            return;
                        }
                    }
                } else {
                    // may be a server that does not handle HEAD requests
                    try {
                        // don't really care about the stream...
                        InputStream is = response.getEntity().getContent();
                        // read to end of stream...
                        final long count = 1024L;
                        while (is.skip(count) == count)
                            ;
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    log.warning("Status code on Head request: " + statusCode);
                    if (statusCode >= 200 && statusCode <= 299) {
                    	outcome.errorMessage = "A network login screen may be interfering" +
                    			" with the transmission to the server. Status code: " + Integer.toString(statusCode);
                        return;
                    }
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                outcome.errorMessage = "Unexpected protocol exception: " + e.getMessage();
                return;
            } catch (Exception e) {
                e.printStackTrace();
                outcome.errorMessage = "Unexpected exception: " + e.getMessage();
                return;
            }
        }
        // At this point, we may have updated the uri to use https.
        // This occurs only if the Location header keeps the host name
        // the same. If it specifies a different host name, we error
        // out.
        // 
        // And we may have set authentication cookies in our
        // cookiestore (referenced by localContext) that will enable
        // authenticated publication to the server.
        // 

        // get instance file
        File file = new File(FormFileUtils.getInstanceFilePath(instanceDir));
        String submissionFile = file.getName();

        if (!file.exists()) {
            outcome.errorMessage = "Submission file does not exist: " + file.getAbsolutePath();
            log.severe(outcome.errorMessage);
            return;
        }

        // find all files in parent directory
        File[] allFiles = file.getParentFile().listFiles();

        boolean someFilesNotUploaded = false;
        // clean up the list, removing anything that is suspicious
        // or that we won't attempt to upload. For OpenRosa servers,
        // we'll upload just about everything...
        List<File> files = new ArrayList<File>();
        for (File f : allFiles) {
            String fileName = f.getName();
            int idx = fileName.lastIndexOf(".");
            String extension = "";
            if (idx != -1) {
                extension = fileName.substring(idx + 1);
            }
            if (fileName.startsWith(".")) {
                // potential Apple file attributes file -- ignore it
                continue;
            }
            if (fileName.equals(submissionFile)) {
                continue; // this is always added
            } else if (openRosaServer) {
                files.add(f);
            } else if (extension.equals("jpg")) { // legacy 0.9x
                files.add(f);
            } else if (extension.equals("3gpp")) { // legacy 0.9x
                files.add(f);
            } else if (extension.equals("3gp")) { // legacy 0.9x
                files.add(f);
            } else if (extension.equals("mp4")) { // legacy 0.9x
                files.add(f);
            } else {
            	log.warning("unrecognized file type " + f.getName());
                someFilesNotUploaded = true;
            }
        }

        boolean successfulAttemptSoFar = true;
        StringBuilder b = new StringBuilder();
        boolean first = true;
        int j = 0;
        while (j < files.size() || first) {
            first = false;

            HttpPost httppost = WebUtils.createOpenRosaHttpPost(u);
            
            MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");

            long byteCount = 0L;

            // mime post
            MultipartEntity entity = new MultipartEntity();

            // add the submission file first...
            FileBody fb = new FileBody(file, "text/xml");
            entity.addPart("xml_submission_file", fb);
            log.info("added xml_submission_file: " + file.getName());
            byteCount += file.length();

            for (; j < files.size(); j++) {
                File f = files.get(j);
                String fileName = f.getName();
                int idx = fileName.lastIndexOf(".");
                String extension = "";
                if (idx != -1) {
                    extension = fileName.substring(idx + 1);
                }
                Collection<?> contentTypes = MimeUtil.getMimeTypes(f);
                String contentType = null;
                if (contentTypes != null && contentTypes.size() >= 1) {
                	String[] array = null;
                	array = contentTypes.toArray(new String[1]);
                	// take first match...
                	contentType = array[0];
                }

                // we will be processing every one of these, so
                // we only need to deal with the content type determination...
                if (extension.equals("xml")) {
                    fb = new FileBody(f, "text/xml");
                    entity.addPart(f.getName(), fb);
                    byteCount += f.length();
                    log.info("added xml file " + f.getName());
                } else if (extension.equals("jpg")) {
                    fb = new FileBody(f, "image/jpeg");
                    entity.addPart(f.getName(), fb);
                    byteCount += f.length();
                    log.info("added image file " + f.getName());
                } else if (extension.equals("3gpp")) {
                    fb = new FileBody(f, "audio/3gpp");
                    entity.addPart(f.getName(), fb);
                    byteCount += f.length();
                    log.info("added audio file " + f.getName());
                } else if (extension.equals("3gp")) {
                    fb = new FileBody(f, "video/3gpp");
                    entity.addPart(f.getName(), fb);
                    byteCount += f.length();
                    log.info("added video file " + f.getName());
                } else if (extension.equals("mp4")) {
                    fb = new FileBody(f, "video/mp4");
                    entity.addPart(f.getName(), fb);
                    byteCount += f.length();
                    log.info("added video file " + f.getName());
                } else if (extension.equals("csv")) {
                    fb = new FileBody(f, "text/csv");
                    entity.addPart(f.getName(), fb);
                    byteCount += f.length();
                    log.info("added csv file " + f.getName());
                } else if (extension.equals("xls")) {
                    fb = new FileBody(f, "application/vnd.ms-excel");
                    entity.addPart(f.getName(), fb);
                    byteCount += f.length();
                    log.info("added xls file " + f.getName());
                } else if (contentType != null) {
                    fb = new FileBody(f, contentType);
                    entity.addPart(f.getName(), fb);
                    byteCount += f.length();
                    log.info("added recognized filetype (" + contentType + ") "
                            + f.getName());
                } else {
                    contentType = "application/octet-stream";
                    fb = new FileBody(f, contentType);
                    entity.addPart(f.getName(), fb);
                    byteCount += f.length();
                    log.warning("added unrecognized file (" + contentType + ") "
                                    + f.getName());
                }

                // we've added at least one attachment to the request...
                if (j + 1 < files.size()) {
                    if (byteCount + files.get(j + 1).length() > 10000000L) {
                        // the next file would exceed the 10MB threshold...
                    	log.info("Extremely long post is being split into multiple posts");
                        try {
                            StringBody sb = new StringBody("yes", Charset.forName("UTF-8"));
                            entity.addPart("*isIncomplete*", sb);
                        } catch (Exception e) {
                            e.printStackTrace(); // never happens...
                        }
                        ++j; // advance over the last attachment added...
                        break;
                    }
                }
            }

            httppost.setEntity(entity);

            // prepare response and return uploaded
            HttpResponse response = null;
            try {
                response = httpClient.execute(httppost, localContext);
                int responseCode = response.getStatusLine().getStatusCode();
                log.info("Response code:" + responseCode);
                // verify that the response was a 201 or 202.
                // If it wasn't, the submission has failed.
                if (responseCode != 201 && responseCode != 202) {
                    if (responseCode == 200) {
                        b.append("A network login screen may be interfering with the transmission to the server.");
                    } else {
                        b.append(response.getStatusLine().getReasonPhrase() + " ("
                                + responseCode + ")");
                    }
                    successfulAttemptSoFar = false;
                }
                // read the body of the response (needed before we can reuse connection).
                InputStream is = null;
                BufferedReader r = null;
                try {
                    is = response.getEntity().getContent();
                    r = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = r.readLine()) != null) {
                        if (responseCode == 201 || responseCode == 202) {
                        	log.info(line);
                        } else {
                        	log.severe(line);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    b.append(e.getLocalizedMessage());
                    b.append("\n");
                } finally {
                    if (r != null) {
                        try {
                            r.close();
                        } catch (Exception e) {
                        } finally {
                            r = null;
                        }
                    }
                    if (is != null) {
                        try {
                            is.close();
                        } catch (Exception e) {
                        } finally {
                            is = null;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                successfulAttemptSoFar = false;
                b.append(e.getLocalizedMessage());
                b.append("\n");
            }
        }

        // ok, all the parts of the submission were sent...
        // If it wasn't, the submission has failed.
        if (successfulAttemptSoFar && b.length() == 0 && !someFilesNotUploaded) {
        	outcome.errorMessage = "";
        	outcome.isCompleteTransfer = true;
        	outcome.isSuccessful = true;
        	return;
        } else if (successfulAttemptSoFar && b.length() == 0 && someFilesNotUploaded) {
        	outcome.errorMessage = "";
        	outcome.isCompleteTransfer = false;
        	outcome.isSuccessful = true;
        	return;
        } else {
        	outcome.errorMessage = b.toString();
        	outcome.isCompleteTransfer = false;
        	outcome.isSuccessful = false;
        	return;
        }
	}
}
