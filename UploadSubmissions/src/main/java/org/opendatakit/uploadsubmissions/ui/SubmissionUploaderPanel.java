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
package org.opendatakit.uploadsubmissions.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.opendatakit.uploadsubmissions.applet.UploadApplet;
import org.opendatakit.uploadsubmissions.utils.FindDirectoryStructure;

public class SubmissionUploaderPanel extends JPanel implements ActionListener{	
	
	/**
	 * Notification interface to inform our creator to launch the 
	 * upload submissions activity. 
	 */
	public interface ActionListener {
		/**
		 * Called when the start submissions upload button is pressed.
		 * 
		 * @param directory containing all the submissions directories.
		 * @return true if the UI should remain disabled (we're working!)
		 */
		public boolean doUploadSubmissions( File submissionsParentDir ); 
	}

	private static final String LABEL_INSTRUCTIONS = "Select the location containing the submissions:";
	private static final String LABEL_FOLDER = "Folder:";
	private static final String COMMAND_REFRESH = "Refresh";
	public static final String COMMAND_SELECT = "Upload Submissions";
	private static final String COMMAND_CHOOSE = "Choose";
	private static final String TRUE_DRIVE_CMD_PREFIX = "@";
	private static final String MANUAL_DRIVE_CMD = "manual";
	
	private static final long serialVersionUID = 6753077036860161654L;

	private final ActionListener _callback;
	
	private Logger _logger = Logger.getLogger(SubmissionUploaderPanel.class.getName());
	private String _directoryStructureToSearchFor;
	
	// GUI components
	private JPanel _panel;
	
	private String _lastActiveButtonCommandValue = null;
	ButtonGroup _driveButtons;
	private JRadioButton _manualDriveRadioButton;
	private JTextField _manualDriveLocationField;
	private JButton _manualDriveSelectButton;
	private String _manualDrivePathValueText = null;
	
	private JButton _doActionButton;
	private JButton _driveRefreshButton;
	
	public SubmissionUploaderPanel(String directoryStructureToSearchFor, ActionListener callback)
	{
        super(new BorderLayout());
        _callback = callback;
		
		_directoryStructureToSearchFor = FindDirectoryStructure.normalizePathString(directoryStructureToSearchFor);

		BorderLayout gb = new BorderLayout();
		this.setLayout(gb);
		
		rebuildPanel();
	}
	
	private void rebuildPanel() {
		if ( _panel != null ) {
			_manualDrivePathValueText = _manualDriveLocationField.getText().trim();
		}
		if ( _manualDrivePathValueText == null ) {
			_manualDrivePathValueText = "";
		}
		JPanel panel = buildFreshPanel();
		if ( _panel != null ) {
			this.remove(_panel);
		}
		_panel = panel;
        add(_panel, BorderLayout.CENTER);
        this.validate();
        this.invalidate();
        Container c = this.getParent();
        if ( c != null ) {
        	c.validate();
        	c.invalidate();
        }
	}
	
	private JPanel buildFreshPanel() {
		JPanel panel = new JPanel(new GridBagLayout());

		_lastActiveButtonCommandValue = MANUAL_DRIVE_CMD;
		
		GridBagConstraints c = new GridBagConstraints();
		// Set common constraint settings
		c.anchor = GridBagConstraints.LINE_END;
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(UploadApplet.SEPARATION_DISTANCE, UploadApplet.SEPARATION_DISTANCE, 
								UploadApplet.SEPARATION_DISTANCE, UploadApplet.SEPARATION_DISTANCE);
		
		// Get mounted drives that have the needed directory structures...
        List<File> submissionParentDirs = FindDirectoryStructure.searchMountedDrives(_directoryStructureToSearchFor);
        int yOffset = 1;
        
        if ( submissionParentDirs.isEmpty() ) {
        	_logger.info("No mounted drives match -- writing refresh message");
        	GridBagConstraints cc = (GridBagConstraints) c.clone();
        	cc.insets = new Insets(0,UploadApplet.SEPARATION_DISTANCE,0,0);
        	cc.anchor = GridBagConstraints.LINE_START;
        	
	    	cc.gridy = yOffset++;
	    	panel.add(new JLabel("No " + _directoryStructureToSearchFor + 
								" directory structures found on any mounted drives."), cc);
	    	cc.gridy = yOffset++;
			panel.add(new JLabel("Is your device connected and have you enabled " +
								 "USB Storage for it?"),cc);
	    	cc.gridy = yOffset++;
	    	cc.insets =  new Insets(2*UploadApplet.SEPARATION_DISTANCE, UploadApplet.SEPARATION_DISTANCE, 
					UploadApplet.SEPARATION_DISTANCE, UploadApplet.SEPARATION_DISTANCE);
			panel.add(new JLabel("Click `Refresh` to re-scan for mounted drives."),cc);
        }

    	JLabel label = new JLabel(LABEL_INSTRUCTIONS);
    	label.setOpaque(true);
    	c.anchor = GridBagConstraints.LINE_START;
    	c.gridx = 0;
    	c.gridy = yOffset++;
    	panel.add(label, c);
		c.anchor = GridBagConstraints.LINE_END;
    	
        _driveButtons = new ButtonGroup();
    	boolean first = true;
        for (File submissionPDir : submissionParentDirs)
        {
        	String name = submissionPDir.getAbsolutePath(); // name = "/media/disk/odk/instances"
        	int index = name.indexOf(_directoryStructureToSearchFor);
        	if (index == -1)
        	{
        		// this is a logic error or a drive disconnect error...
        		String error = String.format("Could not find %s in %s when it is in drives list.",
        										_directoryStructureToSearchFor, name);
        		_logger.severe(error);
        		continue;
        	}
        	else
        	{
        		name = name.substring(0, index); // name = "/media/disk"
        	}

        	JRadioButton submissionPDirButton = new JRadioButton(name, first);
        	submissionPDirButton.setMnemonic(name.charAt(0));
        	submissionPDirButton.setActionCommand(TRUE_DRIVE_CMD_PREFIX + name);
        	if (first) {
        		_lastActiveButtonCommandValue = TRUE_DRIVE_CMD_PREFIX + name;
        	}
        	submissionPDirButton.addActionListener(this);
        	_driveButtons.add(submissionPDirButton);
        	c.gridx = 0;
        	c.gridy = yOffset++;
        	panel.add(submissionPDirButton, c);
        	first = false;
        } 

        Box b = Box.createHorizontalBox();
        _manualDriveRadioButton = new JRadioButton(LABEL_FOLDER);
        b.add(_manualDriveRadioButton);
        _manualDriveLocationField = new JTextField(_manualDrivePathValueText, 20);
        _manualDriveLocationField.setEnabled(false);
        b.add(Box.createHorizontalStrut(UploadApplet.SEPARATION_DISTANCE));
        b.add(_manualDriveLocationField, c);
        _manualDriveSelectButton = new JButton(COMMAND_CHOOSE);
        _manualDriveSelectButton.setEnabled(false);
        _manualDriveSelectButton.addActionListener(this);
        b.add(Box.createHorizontalStrut(UploadApplet.SEPARATION_DISTANCE));
        b.add(_manualDriveSelectButton);
        
        _manualDriveRadioButton.setActionCommand(MANUAL_DRIVE_CMD);
        _manualDriveRadioButton.addActionListener(this);
        _driveButtons.add(_manualDriveRadioButton);
        c.gridx = 0;
        c.gridy = yOffset++;
        c.fill = GridBagConstraints.BOTH;
        panel.add(b, c);
        
        GridBagConstraints cl = new GridBagConstraints();
        cl.anchor = GridBagConstraints.LINE_START;
        cl.fill = GridBagConstraints.HORIZONTAL;
        cl.gridx = 0;
        cl.gridy = yOffset++;
        cl.weightx = 1.0;
		cl.insets = new Insets(UploadApplet.SEPARATION_DISTANCE, UploadApplet.SEPARATION_DISTANCE, 
				UploadApplet.SEPARATION_DISTANCE, UploadApplet.SEPARATION_DISTANCE);
		panel.add(new JSeparator(SwingConstants.HORIZONTAL),cl);

        _doActionButton = new JButton(COMMAND_SELECT);
        _doActionButton.addActionListener(this);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        c.gridy = yOffset;
        panel.add(_doActionButton, c);
        
        _driveRefreshButton = new JButton(COMMAND_REFRESH);
        _driveRefreshButton.addActionListener(this);
        c.anchor = GridBagConstraints.LINE_END;
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        c.gridy = yOffset++;
        panel.add(_driveRefreshButton, c);

        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        c.gridy = yOffset;
        panel.add(new JLabel("NOTE: upon successful upload, submissions will be deleted."), c);

        return panel;
	}

	public void toggleEnable(boolean isEnabled) {
		
		// enable/disable the radio buttons and find the selected one
		JRadioButton selected = null;
		Enumeration<AbstractButton> eb = _driveButtons.getElements();
		while ( eb.hasMoreElements() ) {
			JRadioButton b = (JRadioButton)eb.nextElement();
			if ( b.isSelected() ) {
				selected = b;
			}
			b.setEnabled(isEnabled);
		}
		
		// if we are enabling and manual radio button is selected,
		// we should enable the text field and chooser button
		boolean manualSelected = isEnabled &&
			  ( selected != null &&
				selected.equals(_manualDriveRadioButton));
		_manualDriveLocationField.setEnabled(manualSelected);
		_manualDriveSelectButton.setEnabled(manualSelected);

		// and then there are the start and refresh buttons...
		_doActionButton.setEnabled(isEnabled);
		_driveRefreshButton.setEnabled(isEnabled);
	}
	
	@Override
	public void actionPerformed(ActionEvent event) {
		if ( event.getSource().equals(_driveRefreshButton) ) {
			try {
				toggleEnable(false);
				rebuildPanel();
			} finally {
				toggleEnable(true);
			}
		}
		else if ( event.getSource().equals(_manualDriveSelectButton) ) {
			try {
				toggleEnable(false);
				JFileChooser fc = new JFileChooser();
				String filename = _manualDriveLocationField.getText().trim();
				File f = new File(filename);
				if ( f.exists() ) {
					fc.setSelectedFile(f);
				}
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				
				int retval = fc.showOpenDialog(this);
				if ( retval == JFileChooser.APPROVE_OPTION ) {
					File file = fc.getSelectedFile();
					if ( file != null ) {
						_manualDriveLocationField.setText(file.getCanonicalPath());
					}
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			} finally {
				toggleEnable(true);
			}
		}
		else if ( event.getSource().equals(_doActionButton) ) {
			boolean working = false;
			try {
				toggleEnable(false);
				String submissionsParentDirString = null;
				String cmd = _lastActiveButtonCommandValue;
				if ( cmd.startsWith(TRUE_DRIVE_CMD_PREFIX) ) {
					submissionsParentDirString = cmd.substring(TRUE_DRIVE_CMD_PREFIX.length()) +
							_directoryStructureToSearchFor;
				} else if ( cmd.equals(MANUAL_DRIVE_CMD) ) {
					submissionsParentDirString = _manualDriveLocationField.getText().trim();
				}
				
				if ( submissionsParentDirString == null ) {
					submissionsParentDirString = "";
				}
				working = sendSubmissions(submissionsParentDirString);
			} finally {
				toggleEnable(!working);
			}
		}
		else if ( event.getSource().equals(_manualDriveRadioButton)) {
			_lastActiveButtonCommandValue = event.getActionCommand();
			_manualDriveLocationField.setEnabled(true);
			_manualDriveSelectButton.setEnabled(true);
		}
		else {
			_lastActiveButtonCommandValue = event.getActionCommand();
			_manualDriveLocationField.setEnabled(false);
			_manualDriveSelectButton.setEnabled(false);
		}
	}

	/**
	 * 
	 * @param submissionsParentDirString directory where submissions directories are located
	 * @return true if the UI should remain disabled (we're working)
	 */
	private boolean sendSubmissions(String submissionsParentDirString) {
		File submissionsParentDir = null;
		ArrayList<String> errors = new ArrayList<String>();

		// Add error message on invalid drive
		if (submissionsParentDirString == null || submissionsParentDirString.equals(""))
		{
			_logger.warning("No location selected or folder is blank.");
			errors.add("No location selected or folder is blank.");
		}
		// Valid drive, proceed processing
		else
		{
			submissionsParentDir = new File(submissionsParentDirString);
			if (!submissionsParentDir.exists())
			{
				_logger.warning("Directory does not exist: " + submissionsParentDir);
				errors.add("Directory does not exist: " + submissionsParentDir);
			}
			else if(!submissionsParentDir.canRead())
			{
				_logger.warning("Can't read " + submissionsParentDir);
				errors.add("Don't have permission to read " + submissionsParentDir);
			}
			else if (submissionsParentDir.list().length == 0)
			{
				_logger.warning("Submissions parent directory is empty -- nothing to submit!");
				errors.add(submissionsParentDir + " is empty - nothing to submit!");
			}
		}
		
		if (submissionsParentDir != null && errors.isEmpty())
		{
			return _callback.doUploadSubmissions(submissionsParentDir);
		}
		else if (!errors.isEmpty())
		{
			errors.add(0, "Please resolve the following problems:");
			JOptionPane.showMessageDialog(this, errors.toArray(), "Errors", JOptionPane.WARNING_MESSAGE);
		}
		return false;
	}
}
