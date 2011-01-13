package org.opendatakit.uploadsubmissions.applet;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.http.client.params.HttpClientParams;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.opendatakit.uploadsubmissions.submission.Submission;
import org.opendatakit.uploadsubmissions.submission.SubmissionResult;
import org.opendatakit.uploadsubmissions.utils.IHttpClientFactory;

public class UploadWorker {
	
	public static final String APP_VERSION = "1.0";
	public static final int CONNECTION_TIMEOUT = 30000;
	/**
	 * Notification interface to inform our creator what 
	 * submission we are currently posting data for. 
	 */
	public interface ActionListener {
		/**
		 * Called just before a post of submission data is attempted.
		 * 
		 * @param url what we are attempting to connect to.
		 * @param tries number of times we have retried connection.
		 * @param count number of posts for this submission (so far).
		 */
		public void beforePostUrl( String submissionName, int tries, int count ); 
	}

	private static final Logger _logger = Logger.getLogger(UploadWorker.class.getName());

	private final ActionListener uiNotify;
	private final URL submissionURL;
	
	public UploadWorker( URL submissionURL, ActionListener uiNotify ) {
		this.uiNotify = uiNotify;
		this.submissionURL = submissionURL;
	}
	
	public void doWork(IHttpClientFactory factory, File submissionsParentDir) throws UploadSubmissionsException {

		// get the list of submissions.  These will be the folders under the parent dir...
        File[] submissionFolders = submissionsParentDir.listFiles(new FileFilter() {
			
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});

        // configure connection
	    HttpParams params = new BasicHttpParams();
	    HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
	    HttpConnectionParams.setSoTimeout(params, CONNECTION_TIMEOUT);
	    HttpClientParams.setRedirecting(params, false);
	    
	    List<SubmissionResult> results = new ArrayList<SubmissionResult>();
	    
	    int count = 0;
	    boolean success = true;
        // loop through each, attempting to submit it...
        for (File submissionFolder : submissionFolders ) {
        	++count;
        	try {
				uiNotify.beforePostUrl("Submitting " + submissionFolder.getName(), 1, count);
				
				Submission submission = new Submission(factory, params, submissionURL, submissionFolder);
        	
        		SubmissionResult result = submission.submitAndDeleteLocalCopy();

        		results.add(result);
    			
    			if (result.isSuccess()) {
    				uiNotify.beforePostUrl(result.getFile().getName() + " Success!", 1, count);
    			} else {
    				uiNotify.beforePostUrl(result.getFile().getName() + " " + result.getFailureReason(), 1, count);
    				success = false;
    				_logger.warning("Submission failed: " + result.getFile() + ". Reason: " + result.getFailureReason());
    			}
        	} catch ( Exception e ) {
        		e.printStackTrace();
        		SubmissionResult result = new SubmissionResult(submissionFolder, submissionURL, false);
        		result.setFailureReason("Unexpected exception " + e.getMessage());
        		results.add(result);
				uiNotify.beforePostUrl(result.getFile().getName() + " " + result.getFailureReason(), 1, count);
        		success = false;
				_logger.warning("Submission failed: " + result.getFile() + ". Reason: " + result.getFailureReason());
        	}
        }
        
        if ( !success ) {
        	throw new UploadSubmissionsException(results);
        }
	}
	
	public void cleanup(boolean uploadStatus) {
	}
}
