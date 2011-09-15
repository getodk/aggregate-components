package org.opendatakit.briefcase.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
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
import org.kxml2.io.KXmlParser;
import org.kxml2.io.KXmlSerializer;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class Aggregate10Utils {

  private static final Logger log = Logger.getLogger(Aggregate10Utils.class.getName());
  public static final String NAMESPACE_ODK = "http://www.opendatakit.org/xforms";

  public static final String FORM_ID_ATTRIBUTE_NAME = "id";
  public static final String MODEL_VERSION_ATTRIBUTE_NAME = "version";
  public static final String UI_VERSION_ATTRIBUTE_NAME = "uiVersion";
  public static final String INSTANCE_ID_ATTRIBUTE_NAME = "instanceID";
  public static final String SUBMISSION_DATE_ATTRIBUTE_NAME = "submissionDate";
  public static final String IS_COMPLETE_ATTRIBUTE_NAME = "isComplete";
  public static final String MARKED_AS_COMPLETE_DATE_ATTRIBUTE_NAME = "markedAsCompleteDate";

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

  public static interface ServerXmlStreamHandler {
    /**
     * Process the response stream for a request.  May throw exceptions if a processing error occurs.
     * 
     * @param serverInfo
     * @param responseStream
     * @return true if processing was successful.  false otherwise (outcome contains error message).
     * @throws IOException
     * @throws XmlPullParserException
     */
    boolean readStream(ServerConnectionInfo serverInfo, InputStream responseStream) throws IOException,
        XmlPullParserException;
  }

  private static class FlushStream implements ServerXmlStreamHandler {

    FlushStream() {
    };

    @Override
    public boolean readStream(ServerConnectionInfo serverInfo, InputStream requestStream)
        throws IOException {
      // simply flush the stream -- not interested in parsing it.
      final long count = 1024L;
      while (requestStream.skip(count) == count)
        ;
      requestStream.close();
      return true;
    }

  }

  private static class ProcessXmlStream implements ServerXmlStreamHandler {

    private ServerConnectionInfo serverInfo = null;
    private boolean successful = false;
    private Document parsedDoc = null;

    ProcessXmlStream() {
    }

    @Override
    public boolean readStream(ServerConnectionInfo serverInfo, InputStream requestStream)
        throws IOException, XmlPullParserException {
      // TODO Auto-generated method stub
      this.serverInfo = serverInfo;
      // parse response
      Document doc = null;
      InputStreamReader isr = null;
      try {
        isr = new InputStreamReader(requestStream, "UTF-8");
        doc = new Document();
        KXmlParser parser = new KXmlParser();
        parser.setInput(isr);
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        doc.parse(parser);
        parsedDoc = doc;
        successful = true;
      } catch (IOException e) {
        e.printStackTrace();
        log.severe("Parsing failed with IO exception " + e.getMessage());
        throw e;
      } catch (XmlPullParserException e) {
        e.printStackTrace();
        log.severe("Parsing failed with " + e.getMessage());
        throw e;
      } finally {
        final long count = 1024L;
        while (requestStream.skip(count) == count)
          ;
        if (isr != null) {
          try {
            isr.close();
          } catch (Exception e) {
            // no-op
          }
        }
        try {
          requestStream.close();
        } catch (Exception e) {
          // no-op
        }
      }
      return true;
    }

    public ServerConnectionInfo getServerInfo() {
      return serverInfo;
    }

    public Document getParsedDoc() {
      return parsedDoc;
    }

    public boolean isSuccessful() {
      return successful;
    }
    
    protected void setIsSuccessful(boolean value) {
      successful = value;
    }
  }
  
  public static final Document retrieveAvailableFormsFromServer(ServerConnectionInfo serverInfo) {
    ProcessXmlStream actor = new ProcessXmlStream();
    fetchFormList(serverInfo, actor);
    if (actor.isSuccessful()) {
      return actor.getParsedDoc();
    } else {
      return null;
    }
  }

  public static final void testServerDownloadConnection(ServerConnectionInfo serverInfo) {
    fetchFormList(serverInfo, new FlushStream());
  }

  private static final void fetchFormList(ServerConnectionInfo serverInfo,
      ServerXmlStreamHandler handler) {
    outcome = new ServerConnectionOutcome();

    String urlString = serverInfo.getUrl();
    if (urlString.endsWith("/")) {
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
      outcome.errorMessage = "Invalid url: " + urlString + ".\nFailed with error: "
          + e.getMessage();
      log.severe(outcome.errorMessage);
      return;
    } catch (URISyntaxException e) {
      e.printStackTrace();
      outcome.errorMessage = "Invalid uri: " + urlString + ".\nFailed with error: "
          + e.getMessage();
      log.severe(outcome.errorMessage);
      return;
    }

    HttpClient httpClient = serverInfo.getHttpClient();
    if (httpClient == null) {
      httpClient = WebUtils.createHttpClient(20000);
      serverInfo.setHttpClient(httpClient);
    }

    // get shared HttpContext so that authentication and cookies are retained.
    HttpContext localContext = serverInfo.getHttpContext();
    if (localContext == null) {
      localContext = WebUtils.createHttpContext();
      serverInfo.setHttpContext(localContext);
    }

    WebUtils.clearAllCredentials(localContext);
    WebUtils.addCredentials(localContext, serverInfo.getUsername(), serverInfo.getPassword(),
        u.getHost());

    {
      // we need to issue a get request
      HttpGet httpGet = WebUtils.createOpenRosaHttpGet(u);

      // prepare response
      HttpResponse response = null;
      try {
        response = httpClient.execute(httpGet, localContext);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200) {
          Header[] openRosaVersions = response.getHeaders(WebUtils.OPEN_ROSA_VERSION_HEADER);
          if (openRosaVersions != null && openRosaVersions.length != 0) {
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
                serverInfo.setUrl(fullUrl.substring(0, idx));
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
            // stream contains form list -- pass that to stream handler.
            // That should read all bytes from the stream and close it.
            if ( !handler.readStream(serverInfo, response.getEntity().getContent()) ) {
              // errors already set...
              return;
            }
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
          outcome.errorMessage = "A network login screen may be interfering"
              + " with the transmission to the server. Status code: "
              + Integer.toString(statusCode);
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

  public static final void testServerUploadConnection(ServerConnectionInfo serverInfo) {
    testServerConnection(serverInfo, "submission");
  }

  /**
   * Send a HEAD request to the server to confirm the validity of the URL and
   * credentials.
   * 
   * @param serverInfo
   * @param actionAddr
   * @return null on failure; otherwise, the confirmed URI of this action.
   */
  public static URI testServerConnection(ServerConnectionInfo serverInfo, String actionAddr) {

    outcome = new ServerConnectionOutcome();

    String urlString = serverInfo.getUrl();
    if (urlString.endsWith("/")) {
      urlString = urlString + actionAddr;
    } else {
      urlString = urlString + "/" + actionAddr;
    }

    URI u;
    try {
      URL url = new URL(urlString);
      u = url.toURI();
    } catch (MalformedURLException e) {
      e.printStackTrace();
      outcome.errorMessage = "Invalid url: " + urlString + "for " + actionAddr
          + ".\nFailed with error: " + e.getMessage();
      log.severe(outcome.errorMessage);
      return null;
    } catch (URISyntaxException e) {
      e.printStackTrace();
      outcome.errorMessage = "Invalid uri: " + urlString + "for " + actionAddr
          + ".\nFailed with error: " + e.getMessage();
      log.severe(outcome.errorMessage);
      return null;
    }

    HttpClient httpClient = serverInfo.getHttpClient();
    if (httpClient == null) {
      httpClient = WebUtils.createHttpClient(5000);
      serverInfo.setHttpClient(httpClient);
    }

    // get shared HttpContext so that authentication and cookies are retained.
    HttpContext localContext = serverInfo.getHttpContext();
    if (localContext == null) {
      localContext = WebUtils.createHttpContext();
      serverInfo.setHttpContext(localContext);
    }

    WebUtils.clearAllCredentials(localContext);
    WebUtils.addCredentials(localContext, serverInfo.getUsername(), serverInfo.getPassword(),
        u.getHost());

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
          if (openRosaVersions != null && openRosaVersions.length != 0) {
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
                u = uNew;
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
                return u;
              } else {
                // Don't follow a redirection attempt to a different host.
                // We can't tell if this is a spoof or not.
                String msg = "Starting url: " + u.toString()
                    + " unexpected redirection attempt to a different host: " + uNew.toString();
                log.severe(msg);
                outcome.errorMessage = msg;
                return null;
              }
            } catch (Exception e) {
              e.printStackTrace();
              String msg = "Starting url: " + u.toString() + " unexpected exception: "
                  + e.getLocalizedMessage();
              log.severe(msg);
              outcome.errorMessage = msg;
              return null;
            }
          } else {
            String msg = "The url: " + u.toString()
                + " is not Aggregate 1.0 - status code on Head request: " + statusCode;
            log.severe(msg);
            outcome.errorMessage = msg;
            return null;
          }
        } else {
          // may be a server that does not handle HEAD requests
          if ( response.getEntity() != null ) {
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
          }
          String msg = "The username or password may be incorrect or the url: " + u.toString()
              + " is not Aggregate 1.0 - status code on Head request: " + statusCode;
          log.severe(msg);
          outcome.errorMessage = msg;
          return null;
        }
      } catch (ClientProtocolException e) {
        e.printStackTrace();
        String msg = "Starting url: " + u.toString() + " unexpected exception: "
            + e.getLocalizedMessage();
        log.severe(msg);
        outcome.errorMessage = msg;
        return null;
      } catch (Exception e) {
        e.printStackTrace();
        String msg = "Starting url: " + u.toString() + " unexpected exception: "
            + e.getLocalizedMessage();
        log.severe(msg);
        outcome.errorMessage = msg;
        return null;
      }
    }
  }

  public static class UploadOutcome {
    public final boolean isSuccessful;
    public final boolean notAllFilesUploaded;
    public final String instanceDir;
    public final String errorMessage;

    /**
     * Successful full upload.
     * 
     * @param instanceDir
     */
    public UploadOutcome(String instanceDir) {
      isSuccessful = true;
      notAllFilesUploaded = false;
      this.instanceDir = instanceDir;
      errorMessage = null;
    }

    /**
     * Successful, but, because the server is not an OpenRosa-compliant server,
     * we only uploaded media files and there are other non-media attachments
     * that we omitted uploading.
     * 
     * @param instanceDir
     * @param ignored
     */
    public UploadOutcome(String instanceDir, boolean ignored) {
      isSuccessful = true;
      notAllFilesUploaded = true;
      this.instanceDir = instanceDir;
      errorMessage = null;
    }

    /**
     * Error during the upload to the indicated URI. The localized mesage
     * describing the error is in the message parameter.
     * 
     * @param instanceDir
     * @param uri
     * @param message
     */
    public UploadOutcome(String instanceDir, String uri, String message) {
      isSuccessful = false;
      notAllFilesUploaded = true;
      this.instanceDir = instanceDir;
      errorMessage = message + " while sending to " + uri;
    }

    /**
     * Error during the upload to the indicated URI. The localized mesage
     * describing the error is in the message parameter.
     * 
     * @param instanceDir
     * @param uri
     * @param message
     */
    public UploadOutcome(String instanceDir, URI uri, String message) {
      isSuccessful = false;
      notAllFilesUploaded = true;
      this.instanceDir = instanceDir;
      errorMessage = message + " while sending to " + uri.toString();
    }
  }

  private static final Document uploadFilesToServer(ServerConnectionInfo serverInfo, URI u,
      String distinguishedFileTagName, File file, List<File> files, ProcessXmlStream handler) {

    Document doc = null; // the last response document from the server

    boolean first = true; // handles case where there are no media files
    int j = 0;
    while (j < files.size() || first) {
      first = false;

      HttpPost httppost = WebUtils.createOpenRosaHttpPost(u);

      long byteCount = 0L;

      // mime post
      MultipartEntity entity = new MultipartEntity();

      // add the submission file first...
      FileBody fb = new FileBody(file, "text/xml");
      entity.addPart(distinguishedFileTagName, fb);
      log.info("added " + distinguishedFileTagName + ": " + file.getName());
      byteCount += file.length();

      for (; j < files.size(); j++) {
        File f = files.get(j);
        String fileName = f.getName();
        int idx = fileName.lastIndexOf(".");
        String extension = "";
        if (idx != -1) {
          extension = fileName.substring(idx + 1);
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
        } else {
          fb = new FileBody(f, "application/octet-stream");
          entity.addPart(f.getName(), fb);
          byteCount += f.length();
          log.warning("added unrecognized file (application/octet-stream) " + f.getName());
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
        response = serverInfo.getHttpClient().execute(httppost, serverInfo.getHttpContext());
        int responseCode = response.getStatusLine().getStatusCode();
        log.info("Response code:" + responseCode);
        // verify that the response was a 201 or 202.
        // If it wasn't, the submission has failed.
        if (responseCode != 201 && responseCode != 202) {
          String msg = "POST did not return a 201 or 202 status code: " + responseCode;
          log.severe(msg);
          outcome.errorMessage = msg;
          return null;
        }
        // stream contains form list -- pass that to stream handler.
        // That should read all bytes from the stream and close it.
        if ( !handler.readStream(serverInfo, response.getEntity().getContent()) ) {
          // errors already set...
          return null;
        }
        doc = handler.getParsedDoc();
      } catch (Exception e) {
        e.printStackTrace();
        String msg = "POST unexpected exception: " + e.getMessage();
        log.severe(msg);
        outcome.errorMessage = msg;
        return null;
      }
    }

    // ok, all the parts of the submission were sent...
    // If it wasn't, the submission has failed and returned before this.
    outcome.errorMessage = "";
    outcome.isCompleteTransfer = true;
    outcome.isSuccessful = true;
    return doc;
  }

  public static final void submitInstanceToServerConnection(ServerConnectionInfo serverInfo,
      File instanceDirectory) {

    outcome = new ServerConnectionOutcome();

    URI u = testServerConnection(serverInfo, "submission");
    if (u == null)
      return;

    // We have the actual server URL in u, possibly redirected to https.
    // We know we are talking to the server because the head request
    // succeeded and had a Location header field.

    // try to send instance
    // get instance file
    File file = new File(instanceDirectory, "submission.xml");

    String submissionFile = file.getName();

    if (!file.exists()) {
      String msg = "Submission file not found: " + file.getAbsolutePath();
      log.severe(msg);
      outcome.errorMessage = msg;
      return;
    }

    // find all files in parent directory
    File[] allFiles = instanceDirectory.listFiles();

    // clean up the list, removing anything that is suspicious
    // or that we won't attempt to upload. For OpenRosa servers,
    // we'll upload just about everything...
    List<File> files = new ArrayList<File>();
    for (File f : allFiles) {
      String fileName = f.getName();
      if (fileName.startsWith(".")) {
        // potential Apple file attributes file -- ignore it
        continue;
      }
      if (fileName.equals(submissionFile)) {
        continue; // this is always added
      } else {
        files.add(f);
      }
    }

    ProcessXmlStream actor = new ProcessXmlSubmissionResponseStream(file);

    uploadFilesToServer(serverInfo, u, "xml_submission_file", file, files, actor);
    return;
  }

  public static class ProcessXmlSubmissionResponseStream extends ProcessXmlStream {

    private final File submissionFile;
    
    ProcessXmlSubmissionResponseStream(File submissionFile) {
      super();
      this.submissionFile = submissionFile;
    }
  
    @Override
    public boolean readStream(ServerConnectionInfo serverInfo, InputStream requestStream)
        throws IOException, XmlPullParserException {
      super.readStream(serverInfo, requestStream);
      
      if ( !isSuccessful() ) {
        return false;
      }
      
      Document doc = getParsedDoc();
      Element root = doc.getRootElement();
      Element metadata = root.getElement(NAMESPACE_ODK, "submissionMetadata");
      
      // and get the instanceID and submissionDate from the metadata.
      // we need to put that back into the instance file if not already present
      String instanceID = metadata.getAttributeValue("", INSTANCE_ID_ATTRIBUTE_NAME);
      String submissionDate = metadata.getAttributeValue("", SUBMISSION_DATE_ATTRIBUTE_NAME);
      
      // read the original document...
      Document originalDoc = null;
      try {
        FileInputStream fs = new FileInputStream(submissionFile);
        InputStreamReader fr = new InputStreamReader(fs, "UTF-8");
        originalDoc = new Document();
        KXmlParser parser = new KXmlParser();
        parser.setInput(fr);
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        originalDoc.parse(parser);
        fr.close();
        fs.close();
      } catch (IOException e) {
        e.printStackTrace();
        String msg = "Original submission file could not be opened " + submissionFile.getAbsolutePath();
        log.severe(msg);
        outcome.errorMessage = msg;
        setIsSuccessful(false);
        return false;
      } catch (XmlPullParserException e) {
        e.printStackTrace();
        String msg = "Original submission file could not be parsed as XML file " + submissionFile.getAbsolutePath();
        log.severe(msg);
        outcome.errorMessage = msg;
        setIsSuccessful(false);
        return false;
      }

      // determine whether it has the attributes already added.
      // if they are already there, they better match the values returned by Aggregate 1.0
      boolean hasInstanceID = false;
      boolean hasSubmissionDate = false;
      root = originalDoc.getRootElement();
      for ( int i = 0 ; i < root.getAttributeCount() ; ++i ) {
        String name = root.getAttributeName(i);
        if ( name.equals(INSTANCE_ID_ATTRIBUTE_NAME) ) {
          if ( !root.getAttributeValue(i).equals(instanceID) ) {
            String msg = "Original submission file's instanceID does not match that on server! " + submissionFile.getAbsolutePath();
            log.severe(msg);
            outcome.errorMessage = msg;
            setIsSuccessful(false);
            return false;
          } else {
            hasInstanceID = true;
          }
        }
        
        if ( name.equals(SUBMISSION_DATE_ATTRIBUTE_NAME) ) {
          if ( !root.getAttributeValue(i).equals(submissionDate) ) {
            String msg = "Original submission file's submissionDate does not match that on server! " + submissionFile.getAbsolutePath();
            log.severe(msg);
            outcome.errorMessage = msg;
            setIsSuccessful(false);
            return false;
          } else {
            hasSubmissionDate = true;
          }
        }
      }
    
      if ( hasInstanceID && hasSubmissionDate ) {
        log.info("submission already has instanceID and submissionDate attributes: " + submissionFile.getAbsolutePath());
        outcome.isSuccessful = true;
        return true;
      }
      
      if ( !hasInstanceID ) {
        root.setAttribute("", INSTANCE_ID_ATTRIBUTE_NAME, instanceID);
      }
      if ( !hasSubmissionDate ) {
        root.setAttribute("", SUBMISSION_DATE_ATTRIBUTE_NAME, submissionDate);
      }

      // write the file out...
      File revisedFile = new File( submissionFile.getParentFile(), "." + submissionFile.getName());
      try {
        FileOutputStream fos = new FileOutputStream(revisedFile, false);
        
        KXmlSerializer serializer = new KXmlSerializer();
        serializer.setOutput(fos, "UTF-8");
        originalDoc.write(serializer);
        serializer.flush();
        fos.close();
        
        // and swap files...
        boolean restoreTemp = false;
        File temp = new File( submissionFile.getParentFile(), ".back." + submissionFile.getName() );
        
        try {
          if ( temp.exists() ) {
            if ( !temp.delete() ) {
              String msg = "Unable to remove temporary submission backup file " + temp.getAbsolutePath();
              log.severe(msg);
              outcome.errorMessage = msg;
              setIsSuccessful(false);
              return false;
            }
          }
          if ( !submissionFile.renameTo(temp) ) {
            String msg = "Unable to rename submission to temporary submission backup file " + temp.getAbsolutePath();
            log.severe(msg);
            outcome.errorMessage = msg;
            setIsSuccessful(false);
            return false;
          }
          
          // recovery is possible...
          restoreTemp = true;
          
          if ( !revisedFile.renameTo(submissionFile) ) {
            String msg = "Original submission file could not be updated " + submissionFile.getAbsolutePath();
            log.severe(msg);
            outcome.errorMessage = msg;
            setIsSuccessful(false);
            return false;
          }
          
          // we're successful...
          restoreTemp = false;
        } finally {
          if ( restoreTemp ) {
            if ( !temp.renameTo(submissionFile) ) {
              String msg = "Unable to restore submission from temporary submission backup file " + temp.getAbsolutePath();
              log.severe(msg);
              outcome.errorMessage = msg;
              setIsSuccessful(false);
              return false;
            }
          }
        }
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        String msg = "Temporary submission file could not be opened " + revisedFile.getAbsolutePath();
        log.severe(msg);
        outcome.errorMessage = msg;
        setIsSuccessful(false);
        return false;
      } catch (IOException e) {
        e.printStackTrace();
        String msg = "Temporary submission file could not be written " + revisedFile.getAbsolutePath();
        log.severe(msg);
        outcome.errorMessage = msg;
        setIsSuccessful(false);
        return false;
      }
      
      return true;
    }
  }

  public static void uploadFormToServerConnection(ServerConnectionInfo serverInfo,
      File briefcaseFormDefFile, File briefcaseFormMediaDir) {
    // very similar to upload submissions...

    outcome = new ServerConnectionOutcome();

    URI u = testServerConnection(serverInfo, "formUpload");

    // We have the actual server URL in u, possibly redirected to https.
    // We know we are talking to the server because the head request
    // succeeded and had a Location header field.

    // try to send form...
    if (!briefcaseFormDefFile.exists()) {
      String msg = "Form definition file not found: " + briefcaseFormDefFile.getAbsolutePath();
      log.severe(msg);
      outcome.errorMessage = msg;
      return;
    }

    // find all files in parent directory
    File[] allFiles = briefcaseFormMediaDir.listFiles();

    // clean up the list, removing anything that is suspicious
    // or that we won't attempt to upload. For OpenRosa servers,
    // we'll upload just about everything...
    List<File> files = new ArrayList<File>();
    if ( allFiles != null ) {
      for (File f : allFiles) {
        String fileName = f.getName();
        if (fileName.startsWith(".")) {
          // potential Apple file attributes file -- ignore it
          continue;
        }
        files.add(f);
      }
    }

    ProcessXmlStream actor = new ProcessXmlStream();

    uploadFilesToServer(serverInfo, u, "form_def_file", briefcaseFormDefFile, files, actor);
  }
}
