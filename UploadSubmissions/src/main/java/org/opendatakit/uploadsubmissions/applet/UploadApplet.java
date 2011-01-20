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
package org.opendatakit.uploadsubmissions.applet;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JApplet;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.text.html.parser.ParserDelegator;

import org.opendatakit.uploadsubmissions.applet.UploadWorker.ActionListener;
import org.opendatakit.uploadsubmissions.ui.SubmissionUploaderPanel;
import org.opendatakit.uploadsubmissions.utils.HttpClientFactory;

public class UploadApplet extends JApplet implements
		SubmissionUploaderPanel.ActionListener, ActionListener {

	public static final String ODK_INSTANCES_DIR = "/odk/instances";
	public static final String AGGREGATE_SUBMISSION_SERVLET = "submission";
	public static final String SUBMISSION_UPLOAD_URL_ELEMENT = "UploadSubmissionsApplet";
	public static final int SEPARATION_DISTANCE = 10;
	public static final int STATUS_TEXT_INSET = 7;
	public static final String HTML_OPEN = "<html>";
	public static final String HTML_CLOSE = "</html>";
	public static final String SPAN_OPEN = "<span style=\"font-family: arial; font-size: 120%; color: blue;\">";
	public static final String SPAN_CLOSE = "</span>";

	private static final long serialVersionUID = 1067499473847917508L;

	// So this runs first before JVM loads an L&F
	static {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			// Oh well, we will live with whatever L&F Java decides to use
		}
	}

	/** does all the actual work... */
	private UploadWorker worker = null;

	/** states reported in the status area of the UI */
	private enum ActivityState {
		IDLE, WORKING, POSTING, DONE
	};
	
	/**************************************************************
	 * Data values used to report the activities of 
	 * the worker in the background thread.
	 */
	/** most recent submission being posted */
	private volatile String currentSubmission = "";
	/** retry count */
	private volatile int tries = 1;
	/** count of number of uploads of data for this URL (cursor spans) */
	private volatile int count = 0;
	/** if there was an exception thrown by the worker this is it */
	private volatile Exception eUploadFailure = null;
	/** uploadStatus is true on exit if upload was successful */
	private volatile boolean uploadStatus = false;
	/** track what the status of the action is */
	private volatile ActivityState activityState = ActivityState.IDLE;

	/************************************************************************
	 * Swing controls for the user interface
	 */
	/** status display control */
	private JTextPane statusCtrl;
	private SubmissionUploaderPanel _panel;
	private final Dimension STATUS_CTRL_DIM = new Dimension(800,400);
	
	private CookieHandler mgr;
	/**************************************************************
	 * The user's request values.
	 */

	private final Logger _logger = Logger.getLogger(UploadApplet.class.getName());
	private URL _submissionURL;
	private File _submissionsParentDir;
	
	/** background thread runs via an executor */
	private ExecutorService executor = Executors.newSingleThreadExecutor();

	/************************************************************************
	 * Handler for the background thread that is doing the uploading
	 */
	class Handler implements Runnable {

		/**
		 * Does the meat of the processing.  Runs in a background
		 * thread managed by an executor.
		 * <p>
		 * Transitions activityState from WORKING to POSTING.
		 * Invokes UploadWorker to do the work.  On return, 
		 * saves any exception that may have been thrown, 
		 * transitions activityState to DONE, and triggers
		 * a UI update of the status, an error pop-up if an 
		 * exception was thrown, and the re-enabling of the 
		 * UI.
		 */
		@Override
		public void run() {
			// transition from WORKING to POSTING...
			activityState = ActivityState.POSTING;
			try {
			    HttpClientFactory factory = new HttpClientFactory();
				worker.doWork(factory, _submissionsParentDir);
				uploadStatus = true;
			} catch (Exception e) {
				eUploadFailure = e;
			} finally {
				// clean up -- this will not throw any exceptions
				worker.cleanup(uploadStatus);
				// transition from FETCHING to DONE
				activityState = ActivityState.DONE;
				// update UI...
				statusCtrl.setText(getStatus());
				statusCtrl.setMinimumSize(STATUS_CTRL_DIM);
				statusCtrl.setPreferredSize(STATUS_CTRL_DIM);
				EventQueue.invokeLater(new Runnable() {
					@Override
					public void run() {
						if ( eUploadFailure != null ) {
							errorDialog("error while accessing server", eUploadFailure.getMessage());
						}
						toggleEnable(true);
					}
				});

			}
		}
	}

	/**
	 * Compute the status string.
	 * Synchronized to give a hint to the compiler that we are accessing 
	 * concurrently-updated data values (which are also marked volatile
	 * for an extra dose of compiler hinting).
	 * 
	 * @return string summarizing the current processing status.
	 */
	public synchronized String getStatus() {
		String text = null;
		switch (activityState) {
		case IDLE:
			text = "Idle - select a location and hit `" + 
						SubmissionUploaderPanel.COMMAND_SELECT + "`";
			break;
		case WORKING:
			text = "Working...";
			break;
		case POSTING:
			text = "Posting (" + Integer.toString(count) + ":" +
								Integer.toString(tries)	+ ") - " +
								currentSubmission;
			break;
		case DONE:
			if ( uploadStatus ) {
				text = "Outcome = SUCCESS";
			} else {
				text = "Outcome = FAILURE: " + eUploadFailure.getMessage();
			}
			break;
		default:
			text = "Bad State - please close all browser windows.";
		}
		return HTML_OPEN + SPAN_OPEN + text + SPAN_CLOSE + HTML_CLOSE;
	}

	private void toggleEnable(boolean isEnabled) {
		_panel.toggleEnable(isEnabled);
	}
	
	private void addUI(Component ui, GridBagConstraints c) {
		Container p = getContentPane();
		GridBagLayout gb = (GridBagLayout) p.getLayout();
		p.add(ui);
		gb.setConstraints(ui, c);
	}

	@Override
	public void init() {
		mgr = CookieHandler.getDefault();
		if ( mgr == null ) {
			_logger.severe("No default CookieManager -- creating our own!");
			mgr = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
			CookieHandler.setDefault(mgr);
		} else {
			_logger.info("Found a default CookieManager -- using it!");
		}
		
		Container pane = getContentPane();
		pane.setLayout(new GridBagLayout());

		// This line of code is needed to avoid a NullPointerException
		// http://kr.forums.oracle.com/forums/thread.jspa?threadID=1997861
		new ParserDelegator() ;
		 
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = new Insets(SEPARATION_DISTANCE, SEPARATION_DISTANCE, 
								SEPARATION_DISTANCE, SEPARATION_DISTANCE);
		c.weightx = 1.0;

		JLabel label;
		label = new JLabel("<html><font size=\"+2\"><b>ODK UploadSubmissions Applet </b></font><font size=\"3\">Version " + UploadWorker.APP_VERSION + "</font></html>", JLabel.LEFT);
		addUI(label,c);
		statusCtrl = new JTextPane();
		statusCtrl.setContentType("text/html");
		statusCtrl.setEditable(false);
		statusCtrl.setText(getStatus());
		statusCtrl.setMinimumSize(STATUS_CTRL_DIM);
		statusCtrl.setPreferredSize(STATUS_CTRL_DIM);
		GridBagConstraints cc = (GridBagConstraints) c.clone();
		cc.fill = GridBagConstraints.BOTH;
		JScrollPane scrollable = new JScrollPane(statusCtrl);
	    Border border = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
	    scrollable.setBorder(border);
	    scrollable.setMinimumSize(STATUS_CTRL_DIM);
		addUI(scrollable,cc);

		_panel = new SubmissionUploaderPanel(ODK_INSTANCES_DIR, this);
		_panel.setOpaque(true);
		addUI(_panel,cc);
	}

	/**
	 * Simplistic pop-up error dialog.
	 * 
	 * @param error
	 * @param value
	 */
	private void errorDialog(String error, String value) {
		String msgError = "<html>" + error.substring(0,1).toUpperCase() + error.substring(1) + ". " + value + "</html>";

		JTextPane msgElement = new JTextPane();
		msgElement.setContentType("text/html"); // lets Java know it will be HTML                  
		msgElement.setEditable(false);
		msgElement.setText(msgError);		

		JOptionPane.showMessageDialog(this, msgElement, error,
										JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * Called when the applet container (browser) is being shut down.
	 */
	public void stop() {
		executor.shutdownNow();
		this.getContentPane().removeAll();
		super.stop();
	}

	/**
	 * Uploads the submissions, checks the results, and shows the results to the
	 * user.
	 * 
	 * global: _submissionURL the URL to post the submissions to
	 * 
	 * @param submissionsParentDir
	 *            the directory containing multiple submissions (each submission
	 *            is a directory of its own)
	 */
	@Override
	public boolean doUploadSubmissions(File submissionsParentDir) {
		boolean workerFired = false;

		// Get submission URL
		try {
			/** reset the status outcome variables */
			uploadStatus = false;
			eUploadFailure = null;
			activityState = ActivityState.WORKING;

			_submissionsParentDir = submissionsParentDir;
			// debugging sessions should pass an "urlUploadSubmissionsApplet"
			// parameter
			// that has what would otherwise be the document base...
			URL url = this.getDocumentBase();
			if (getParameter("url" + SUBMISSION_UPLOAD_URL_ELEMENT) != null) {
				url = new URL(getParameter("url"
						+ SUBMISSION_UPLOAD_URL_ELEMENT));
			}

			// The document URL always ends with a
			// SUBMISSION_UPLOAD_URL_ELEMENT...
			// strip that off to get the base URL for the server...
			String path = url.getPath();
			_logger.info("path is: " + path);
			int idx = path.lastIndexOf(SUBMISSION_UPLOAD_URL_ELEMENT);
			if (idx == -1) {
				// this will happen if Aggregate's document tree is rearranged
				// somehow...
				_logger.severe("failed searching for: "
						+ SUBMISSION_UPLOAD_URL_ELEMENT + " in full url: "
						+ url.toString());
				errorDialog("build error", "Bad pass-through of server URL: " + url.toString());
				return false;
			}
			// everything including slash before 'UploadXFormApplet'
			path = path.substring(0, idx);

			// Get submission URL
			path += AGGREGATE_SUBMISSION_SERVLET;
			URL submissionURL = new URL(url.getProtocol(), url.getHost(), url
					.getPort(), path);
			_submissionURL = submissionURL;
			_logger.info("base Url: " + _submissionURL.toString());

			_logger.info("trying to figure out cookies");
			boolean found = false;
			try {
				Map<String,List<String>> rh = new HashMap<String,List<String>> ();
				Map<String,List<String>> cookieStrings = mgr.get(_submissionURL.toURI(), rh);
				List<String> cookies = cookieStrings.get("Cookie");
				for ( String c : cookies ) {
					found = true; 
					_logger.info("found cookie: " + c );
				}
			} catch ( Exception eIgnore) {
			}

			if ( !found ) {
				_logger.severe("no authentication cookie!");
			}
			
			worker = new UploadWorker(_submissionURL, mgr, this);
			
			// launch worker...
			workerFired = true;
			try {
				executor.execute(new Handler());
			} catch ( Exception e ) {
				workerFired = false;
				throw e;
			}
			return workerFired;
		} catch (MalformedURLException e) {
			errorDialog("bad server url", _submissionURL.toString());
			return false;
		} catch (Exception eFail) {
			errorDialog("bad settings", eFail.getClass().getName() + ":" + eFail.getMessage());
			return false;
		} finally {
			toggleEnable(!workerFired);
			if (!workerFired) {
				activityState = ActivityState.DONE;
			}
		}
	}

	/**
	 * The action listener callback for UploadWorker.
	 * Notifies the UI of each submission posted during the upload process.
	 */
	@Override
	public synchronized void beforePostUrl(String currentSubmission, int tries, int count) {
		this.currentSubmission = currentSubmission;
		this.tries = tries;
		this.count = count;
		// update UI...
		statusCtrl.setText(getStatus());
	}
}
