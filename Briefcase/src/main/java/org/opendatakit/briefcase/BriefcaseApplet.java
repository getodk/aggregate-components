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
package org.opendatakit.briefcase;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
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
import javax.swing.ButtonGroup;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.text.html.parser.ParserDelegator;

/**
 * Applet to fetch data from Aggregate and store it in csv and binary
 * files on the local machine.  This is an applet so that the user can
 * log into the Aggregate instance by manually browsing to the Aggregate 
 * instance.  The applet can then use the login credentials of the user
 * for the fetch without ever having to negotiate the login sequence
 * itself.
 * 
 * @author mitchellsundt@gmail.com
 *
 */
public class BriefcaseApplet extends JApplet implements ActionListener, CsvDownload.ActionListener {

	/** Briefcase servlet that embeds the applet in a web page ends with /Briefcase */
	private static final String BRIEFCASE_URL_ELEMENT = "Briefcase";
	public static final int SEPARATION_DISTANCE = 10;
	public static final int STATUS_TEXT_INSET = 7;

	/** serialization */
	private static final long serialVersionUID = 8523973495636927870L;

	/** logger for this applet */
	private final Logger log = Logger.getLogger(BriefcaseApplet.class.getName());

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
	private CsvDownload worker = null;

	/** states reported in the status area of the UI */
	private enum ActivityState {
		IDLE, WORKING, FETCHING, DONE
	};

	/**************************************************************
	 * Data values used to report the activities of 
	 * the worker in the background thread.
	 */
	/** most recent URL being fetched */
	private volatile String currentUrl = "";
	/** retry count */
	private volatile int tries = 1;
	/** count of number of fetches of data for this URL (cursor spans) */
	private volatile int count = 0;
	/** if there was an exception thrown by the worker this is it */
	private volatile Exception eFetchFailure = null;
	/** fetchStatus is true on exit if fetch was successful */
	private volatile boolean fetchStatus = false;
	/** track what the status of the action is */
	private volatile ActivityState activityState = ActivityState.IDLE;
	
	/************************************************************************
	 * Swing controls for the user interface
	 */
	/** status display control */
	private JTextPane statusCtrl;
	private JTextField dirPathCtrl;
	private JButton chooserCtrl;
	private JTextField formIdCtrl;
	private JTextField lastCursorCtrl;
	private JTextField lastKeyCtrl;
	private ButtonGroup binaryButtonGroup;
	private JRadioButton fetchBinaryCtrl;
	private JRadioButton convertBinaryCtrl;
	private JRadioButton asIsBinaryCtrl;
	private JCheckBox recursiveCtrl;
	private JButton executeCtrl;
	
	private final Dimension STATUS_CTRL_DIM = new Dimension(800,100);
	private CookieHandler mgr;
	/**************************************************************
	 * The user's request values.
	 */
	/** initial request URL */
	private String fullUrl = "";
	/** how to handle binary data */
	private CsvDownload.BinaryDataTreatment fetchBinaryData;
	/** whether or not to fetch nested repeated groups */
	private boolean fetchRecursively;
	/** what cursor to resume processing with */
	String lastCursor;
	/** what KEY to resume processing after */
	private String skipBeforeKey;
	
	/** background thread runs via an executor */
	private ExecutorService executor = Executors.newSingleThreadExecutor();

	/************************************************************************
	 * Handler for the background thread that is doing the fetching
	 */
	class Handler implements Runnable {

		/**
		 * Does the meat of the processing.  Runs in a background
		 * thread managed by an executor.
		 * <p>
		 * Transitions activityState from WORKING to FETCHING.
		 * Invokes CsvDownload to do the work.  On return, 
		 * saves any exception that may have been thrown, 
		 * transitions activityState to DONE, and triggers
		 * a UI update of the status, an error pop-up if an 
		 * exception was thrown, and the re-enabling of the 
		 * UI.
		 */
		@Override
		public void run() {
			// transition from WORKING to FETCHING...
			activityState = ActivityState.FETCHING;
			try {
				worker.fetchCsvRecursively(fullUrl, fetchBinaryData, fetchRecursively, skipBeforeKey );
				fetchStatus = true;
			} catch (Exception e) {
				eFetchFailure = e;
			} finally {
				// clean up -- this will not throw any exceptions
				worker.closeAllFilesAndManifest(fetchStatus);
				// transition from FETCHING to DONE
				activityState = ActivityState.DONE;
				// update UI...
				statusCtrl.setText(getStatus());
				statusCtrl.setMinimumSize(STATUS_CTRL_DIM);
				statusCtrl.setPreferredSize(STATUS_CTRL_DIM);
				EventQueue.invokeLater(new Runnable() {
					@Override
					public void run() {
						if ( eFetchFailure != null ) {
							errorDialog("error while accessing server", eFetchFailure.getMessage());
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
			text = "Idle - fill in a request and hit `Retrieve`";
			break;
		case WORKING:
			text = "Working...";
			break;
		case FETCHING:
			text = "Fetching (" + Integer.toString(count) + ":" + Integer.toString(tries) 
					+ ") - " + currentUrl;
			break;
		case DONE:
			if ( fetchStatus ) {
				text = "Outcome = SUCCESS";
			} else {
				text = "Outcome = FAILURE: " + eFetchFailure.getMessage();
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
	public synchronized void beforeFetchUrl(String currentUrl, int tries, int count) {
		this.currentUrl = currentUrl;
		this.tries = tries;
		this.count = count;
		// update UI...
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
		Font f = pane.getFont();
		FontMetrics mf = pane.getFontMetrics(f);
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

		final int VERT_SPACE = mf.getHeight();
		JLabel label;
		label = new JLabel("<html><font size=\"+2\"><b>ODK Briefcase Applet </b></font><font size=\"3\">Version " + CsvDownload.APP_VERSION + "</font></html>", JLabel.LEFT);
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
		label = new JLabel("Download directory in which to store the data files:");
		addUI(label,c);
        Box b = Box.createHorizontalBox();
		dirPathCtrl = new JTextField("C:\\dataspace\\");
		b.add(dirPathCtrl);
        b.add(Box.createHorizontalStrut(SEPARATION_DISTANCE));
		chooserCtrl = new JButton("Choose");
		chooserCtrl.addActionListener(this);
		b.add(chooserCtrl);
		addUI(b, cc);
		recursiveCtrl = new JCheckBox("Download nested repeat groups");
		addUI(recursiveCtrl,c);
		label = new JLabel("Binary data handling:");
		addUI(label,c);
		binaryButtonGroup = new ButtonGroup();
		fetchBinaryCtrl = new JRadioButton("Download binary data and replace server URL with the local filename in the csv.");
		convertBinaryCtrl = new JRadioButton("Replace server URL with the local filename in the csv.");
		asIsBinaryCtrl = new JRadioButton("Keep server URL unchanged in the csv.", true);// default
		binaryButtonGroup.add(fetchBinaryCtrl);
		addUI(fetchBinaryCtrl,c);
		binaryButtonGroup.add(convertBinaryCtrl);
		addUI(convertBinaryCtrl,c);
		binaryButtonGroup.add(asIsBinaryCtrl);
		addUI(asIsBinaryCtrl,c);
		label = new JLabel("FormId:");
		addUI(label,c);
		formIdCtrl = new JTextField("HouseholdSurvey1/HouseholdSurvey");
		addUI(formIdCtrl,c);
		addUI(new JSeparator(SwingConstants.HORIZONTAL),c);
		label = new JLabel("Parameters for resumption of a failed download attempt:");
		addUI(label,c);
		b = Box.createHorizontalBox();
		b.setAlignmentX(LEFT_ALIGNMENT);
		b.add(Box.createHorizontalStrut(3*VERT_SPACE));
		JPanel sub = new JPanel(new GridLayout(4,1));
		GridBagConstraints cs = (GridBagConstraints) c.clone();
		cs.anchor = GridBagConstraints.LINE_START;
		cs.gridx = 0;
		cs.gridy = 0;
		label = new JLabel("LastCursor-x:");
		sub.add(label,cs);
		lastCursorCtrl = new JTextField("");
		cs.gridx = 0;
		cs.gridy = 1;
		sub.add(lastCursorCtrl,cs);
		label = new JLabel("LastKEY-x:");
		cs.gridx = 0;
		cs.gridy = 2;
		sub.add(label,cs);
		lastKeyCtrl = new JTextField("");
		cs.gridx = 0;
		cs.gridy = 3;
		sub.add(lastKeyCtrl,cs);
		b.add(sub);
		addUI(b,c);
		addUI(new JSeparator(SwingConstants.HORIZONTAL),c);
		executeCtrl = new JButton("Retrieve");
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
		formIdCtrl.setEditable(isEnabled);
		fetchBinaryCtrl.setEnabled(isEnabled);
		convertBinaryCtrl.setEnabled(isEnabled);
		asIsBinaryCtrl.setEnabled(isEnabled);
		recursiveCtrl.setEnabled(isEnabled);
		lastCursorCtrl.setEditable(isEnabled);
		lastKeyCtrl.setEditable(isEnabled);
		executeCtrl.setEnabled(isEnabled);		
	}
	
	/**
	 * Action listener for the UI.
	 * <p>
	 * Handles the pressing of the "Retrieve" button.
	 * <p>
	 * This resets the processing status values, transitions
	 * the activity state to WORKING and then cleans up the user- 
	 * supplied server URL and extracts the values for the
	 * fetch request.  It then creates a new CsvWorker and fires
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
				if ( f.exists() && f.isDirectory() ) {
					fc.setSelectedFile(f);
				}
				
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

				int retval = fc.showOpenDialog(BriefcaseApplet.this);
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
				fetchStatus = false;
				eFetchFailure = null;
				activityState = ActivityState.WORKING;
				toggleEnable(false);
				
				URL url = this.getDocumentBase();
				// debugging sessions should pass an "urlBriefcase" parameter
				// that has what would otherwise be the document base... 
				if ( getParameter("url" + BRIEFCASE_URL_ELEMENT) != null ) {
					url = new URL(getParameter("url" + BRIEFCASE_URL_ELEMENT));
				}

				// The document URL always ends with a /briefcase/ element...
				// strip that off to get the base URL for the server...
				String path = url.getPath();
				log.info("path is: " + path);
				int idx = path.lastIndexOf(BRIEFCASE_URL_ELEMENT);
				if ( idx == -1 ) {
					// this will happen if Aggregate's document tree is rearranged somehow...
					log.severe("failed searching for: " + BRIEFCASE_URL_ELEMENT +
							 " in full url: " + url.toString());
					errorDialog("build error", "bad pass-through of server URL: " + url.toString());
					return;
				}
				// everything including slash before 'briefcase/'
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
				String formId = formIdCtrl.getText().trim();
				if ( formId.length() == 0 ) {
					errorDialog("bad formId", formId);
					return;
				}
				
				if ( fetchBinaryCtrl.isSelected() ) {
					fetchBinaryData = CsvDownload.BinaryDataTreatment.DOWNLOAD_BINARY_DATA;
				} else if ( convertBinaryCtrl.isSelected() ) {
					fetchBinaryData = CsvDownload.BinaryDataTreatment.REPLACE_WITH_LOCAL_FILENAME;
				} else {
					fetchBinaryData = CsvDownload.BinaryDataTreatment.RETAIN_BINARY_DATA_URL;
				}
				
				fetchRecursively = recursiveCtrl.isSelected(); 
				/**
				 * Resumption of data processing is supported through the 
				 * cursor and skipBeforeKey values.  The cursor should be 
				 * set to the LastCursor string from the failed Manifest
				 * and the skipBeforeKey should be set to the LastKey string.
				 */
				lastCursor = lastCursorCtrl.getText().trim();
				if ( lastCursor.length() == 0 ) lastCursor = null;
				skipBeforeKey = lastKeyCtrl.getText().trim();
				if ( skipBeforeKey.length() == 0 ) skipBeforeKey = null;

				worker = new CsvDownload(candidateUrl, destDir, this);
				
				Map<String, String> params = new HashMap<String, String>();
				params.put(CsvDownload.FORM_ID, formId);
				params.put(CsvDownload.NUM_ENTRIES, Integer.toString(1));
				params.put(CsvDownload.CURSOR, lastCursor);

				fullUrl = worker.createCsvFragmentLinkWithProperties(params);
				
				// launch worker...
				workerFired = true;
				try {
					executor.execute(new Handler());
				} catch ( Exception ex ) {
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
