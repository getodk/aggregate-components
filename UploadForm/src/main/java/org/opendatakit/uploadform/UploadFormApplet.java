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



import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
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
import javax.swing.Box;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.html.parser.ParserDelegator;

/**
 * Applet to upload a multimedia form into Aggregate 1.0.  This is an applet 
 * so that the user can log into the Aggregate instance by manually browsing 
 * to the Aggregate instance.  The applet can then use the login credentials 
 * of the user for the upload without ever having to negotiate the login 
 * sequence itself.
 * 
 * @author mitchellsundt@gmail.com
 *
 */
public class UploadFormApplet extends JApplet implements ActionListener, FormUpload.ActionListener {

	/** Upload servlet that embeds the applet in a web page ends with /UploadXFormApplet */
	private static final String FORM_UPLOAD_URL_ELEMENT = "UploadXFormApplet";
	public static final String UPLOAD_COMMAND = "Upload Form Definition";
	public static final int SEPARATION_DISTANCE = 10;
	public static final int STATUS_TEXT_INSET = 7;

	/** serialization */
	private static final long serialVersionUID = 8523973495636927870L;

	/** logger for this applet */
	private final Logger log = Logger.getLogger(UploadFormApplet.class.getName());

	// So this runs first before JVM loads an L&F
	static
	{
		try 
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} 
		catch (Exception e) 
		{
			// Oh well, we will live with whatever L&F Java decides to use
		}
	}
	
	/** does all the actual work... */
	private FormUpload worker = null;

	/** states reported in the status area of the UI */
	private enum ActivityState {
		IDLE, WORKING, UPLOADING, DONE
	};

	/**************************************************************
	 * Data values used to report the activities of 
	 * the worker in the background thread.
	 */
	/** summary of action being undertaken */
	private volatile String summary = "in progress";
	/** if there was an exception thrown by the worker this is it */
	private volatile Exception eUploadFormFailure = null;
	/** uploadStatus is true on exit if upload of form was successful */
	private volatile boolean uploadStatus = false;
	/** track what the status of the action is */
	private volatile ActivityState activityState = ActivityState.IDLE;
	
	/************************************************************************
	 * Swing controls for the user interface
	 */
	/** status display control */
	private JTextPane statusCtrl;
	private JTextField dirPathCtrl;
	private JButton chooserCtrl;
	private JButton executeCtrl;
	private final Dimension STATUS_CTRL_DIM = new Dimension(800,400);
	
	private CookieHandler mgr;
	/**************************************************************
	/** background thread runs via an executor */
	private ExecutorService executor = Executors.newSingleThreadExecutor();

	/************************************************************************
	 * Handler for the background thread that is doing the form uploading
	 */
	class Handler implements Runnable {

		/**
		 * Does the meat of the processing.  Runs in a background
		 * thread managed by an executor.
		 * <p>
		 * Transitions activityState from WORKING to UPLOADING.
		 * Invokes CsvDownload to do the work.  On return, 
		 * saves any exception that may have been thrown, 
		 * transitions activityState to DONE, and triggers
		 * a UI update of the status, an error pop-up if an 
		 * exception was thrown, and the re-enabling of the 
		 * UI.
		 */
		@Override
		public void run() {
			// transition from WORKING to UPLOADING...
			activityState = ActivityState.UPLOADING;
			try {
				worker.uploadXForm();
				uploadStatus = true;
			} catch (Exception e) {
				eUploadFormFailure = e;
			} finally {
				// transition from UPLOADING to DONE
				activityState = ActivityState.DONE;
				// update UI...
				statusCtrl.setText(getStatus());
				statusCtrl.setMinimumSize(STATUS_CTRL_DIM);
			    statusCtrl.setPreferredSize(STATUS_CTRL_DIM);
			    EventQueue.invokeLater(new Runnable() {
					@Override
					public void run() {
						if ( eUploadFormFailure != null ) {
							errorDialog("error while accessing server", eUploadFormFailure.getMessage());
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
			text = "Idle - select a form definition file and hit `" + 
						UPLOAD_COMMAND + "`";
			break;
		case WORKING:
			text = "Working...";
			break;
		case UPLOADING:
			text = "Uploading..." + summary;
			break;
		case DONE:
			if ( uploadStatus ) {
				text = "Outcome = SUCCESS";
			} else {
				text = "Outcome = FAILURE: " + eUploadFormFailure.getMessage();
			}
			break;
		default:
			text = "Bad State - please close all browser windows.";
		}
		return "<html><span style=\"font-family: arial; font-size: 120%; color: blue;\">" +
				text + "</span></html>";
	}

	/**
	 * The action listener callback for CsvDownload.
	 * Notifies the UI of each URL access done during the download process.
	 */
	@Override
	public synchronized void beforeUploadUrl(String summary) {
		// update UI...
		this.summary = summary;
		statusCtrl.setText(getStatus());
	}

	private void addUI(Component ui, GridBagConstraints c) {
		Container p = getContentPane();
		GridBagLayout gb = (GridBagLayout) p.getLayout();
		p.add(ui);
		gb.setConstraints(ui, c);
	}
	/**
	 * Called during the initialization of the applet frame.  
	 * Lays out the controls.  Sets up the (sole) action 
	 * listener for the "Retrieve" button.
	 */
	public void init() {
		mgr = CookieHandler.getDefault();
		if ( mgr == null ) {
			log.severe("No default CookieManager -- creating our own!");
			mgr = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
			CookieHandler.setDefault(mgr);
		} else {
			log.info("Found a default CookieManager -- using it!");
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

		GridBagConstraints asNeeded = new GridBagConstraints();
		asNeeded.gridx = GridBagConstraints.LINE_START;
		asNeeded.fill = GridBagConstraints.NONE;
		asNeeded.anchor = GridBagConstraints.LINE_START;
		asNeeded.insets = new Insets(SEPARATION_DISTANCE, SEPARATION_DISTANCE, 
								SEPARATION_DISTANCE, SEPARATION_DISTANCE);
		asNeeded.weightx = 1.0;

		JLabel label;
		label = new JLabel("<html><font size=\"+2\"><b>ODK UploadXFormApplet </b></font><font size=\"3\">Version " + FormUpload.APP_VERSION + "</font></html>", JLabel.LEFT);
		addUI(label,c);
		statusCtrl = new JTextPane();
		statusCtrl.setContentType("text/html"); // lets Java know it will be HTML                  
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
		label = new JLabel("Form definition (.xml):");
		addUI(label,c);
        Box b = Box.createHorizontalBox();
		dirPathCtrl = new JTextField("",20);
		b.add(dirPathCtrl);
        b.add(Box.createHorizontalStrut(SEPARATION_DISTANCE));
		chooserCtrl = new JButton("Choose");
		chooserCtrl.addActionListener(this);
		b.add(chooserCtrl);
		addUI(b,cc);
		addUI(new JSeparator(SwingConstants.HORIZONTAL),c);
		executeCtrl = new JButton(UPLOAD_COMMAND);
		executeCtrl.addActionListener(this);
		addUI(executeCtrl,asNeeded);
	}

	/**
	 * Toggles the UI controls so that they can be disabled during 
	 * processing then re-enabled once the worker has completed 
	 * the download or failed.
	 * 
	 * @param isEnabled
	 */
	public void toggleEnable(boolean isEnabled) {
		dirPathCtrl.setEditable(isEnabled);
		chooserCtrl.setEnabled(isEnabled);
		executeCtrl.setEnabled(isEnabled);		
	}
	
	/**
	 * Action listener for the UI.
	 * <p>
	 * Handles the pressing of the "Upload" button.
	 * <p>
	 * This resets the processing status values, transitions
	 * the activity state to WORKING and then cleans up the user- 
	 * supplied server URL and extracts the values for the
	 * upload request.  It then creates a new CsvWorker and fires
	 * off a background thread to do the actual work.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == chooserCtrl) {
			try {
				toggleEnable(false);
				JFileChooser fc = new JFileChooser();
				String filename = dirPathCtrl.getText();
				File f = new File(filename);
				if ( f.exists() ) {
					fc.setSelectedFile(f);
				}
				fc.setFileFilter(new FileFilter() {

					@Override
					public boolean accept(File f) {
						if ( f.isDirectory() ) return true; // navigate the file system
						return f.getName().endsWith(".xml");
					}

					@Override
					public String getDescription() {
						return ".xml files";
					}} );
				
				int retval = fc.showOpenDialog(UploadFormApplet.this);
				if ( retval == JFileChooser.APPROVE_OPTION ) {
					File file = fc.getSelectedFile();
					if ( file != null ) {
						dirPathCtrl.setText(file.getCanonicalPath());
					}
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			} finally {
				toggleEnable(true);
			}
		} else 
		if (e.getSource() == executeCtrl) {
			boolean workerFired = false;
			String candidateUrl = this.getDocumentBase().toString();
			try {
				/** reset the status outcome variables */
				uploadStatus = false;
				eUploadFormFailure = null;
				activityState = ActivityState.WORKING;
				toggleEnable(false);
				
				URL url = this.getDocumentBase();
				// debugging sessions should pass an "urlUploadXFormApplet" parameter
				// that has what would otherwise be the document base... 
				if ( getParameter("url" + FORM_UPLOAD_URL_ELEMENT) != null ) {
					url = new URL(getParameter("url" + FORM_UPLOAD_URL_ELEMENT));
				}

				// The document URL always ends with a FORM_UPLOAD_URL_ELEMENT...
				// strip that off to get the base URL for the server...
				String path = url.getPath();
				log.info("path is: " + path);
				int idx = path.lastIndexOf(FORM_UPLOAD_URL_ELEMENT);
				if ( idx == -1 ) {
					// this will happen if Aggregate's document tree is rearranged somehow...
					log.severe("failed searching for: " + FORM_UPLOAD_URL_ELEMENT +
							 " in full url: " + url.toString());
					errorDialog("build error", "bad pass-through of server URL: " + url.toString());
				}
				// everything excluding slash before 'UploadXFormApplet'
				path = path.substring(0, idx);
				
				URL serverUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), path);
				candidateUrl = serverUrl.toString();
				log.info("base Url: " + candidateUrl);

				log.info("trying to figure out cookies");
				boolean found = false;
				try {
					Map<String,List<String>> rh = new HashMap<String,List<String>> ();
					Map<String,List<String>> cookieStrings = mgr.get(serverUrl.toURI(), rh);
					List<String> cookies = cookieStrings.get("Cookie");
					for ( String c : cookies ) {
						found = true; 
						log.info("found cookie: " + c );
					}
				} catch ( Exception eIgnore) {
				}

				if ( !found ) {
					log.severe("no authentication cookie!");
				}
				
				String destDir = dirPathCtrl.getText().trim();
				if ( destDir.length() == 0 ) {
					errorDialog("bad directory path", destDir);
					return;
				}

				worker = new FormUpload(candidateUrl, mgr, destDir, this);
				
				// launch worker...
				workerFired = true;
				try {
					executor.execute(new Handler());
				} catch (Exception ex) {
					workerFired = false;
					throw ex;
				}
			} catch (MalformedURLException eIgnore) {
				errorDialog("bad server url", candidateUrl);
			} catch (Exception eFail) {
				errorDialog("bad settings", eFail.getClass().getName() + ":" + eFail.getMessage());
			} finally {
				toggleEnable(!workerFired);
				if (!workerFired) {
					activityState = ActivityState.DONE;
				}
			}
		}
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
}
