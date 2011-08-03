package org.opendatakit.briefcase.ui;

import java.awt.Container;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.opendatakit.briefcase.util.FormFileUtils;

final class ODKCollectFileChooser extends JFileChooser {
	
	/**
	 * 
	 */
	private final Container parentWindow;
	/**
	 * 
	 */
	private static final long serialVersionUID = 8457430037255302153L;
	
	@Override
	public void approveSelection() {
		File f = this.getSelectedFile();
		if ( !FormFileUtils.isValidODKFolder(f) ) {
			JOptionPane.showMessageDialog(this.parentWindow, 
					"Not an ODK Collect device (did not find an " +
					File.separator + "odk" + File.separator + "forms  and/or an " +
					File.separator + "odk" + File.separator + "instances directory)", 
					"Invalid ODK Directory", JOptionPane.ERROR_MESSAGE);
		} else if ( FormFileUtils.isUnderODKFolder(f) || FormFileUtils.isUnderBriefcaseFolder(f)) {
			JOptionPane.showMessageDialog(parentWindow, 
					"Directory appears to be nested within an enclosing Briefcase or ODK Device directory",
					"Invalid Briefcase Directory", JOptionPane.ERROR_MESSAGE);
		} else {
			super.approveSelection();
		}
	}

	ODKCollectFileChooser(Container parentWindow) {
		super(FormFileUtils.getMountPoint());
		this.parentWindow = parentWindow;
		setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	}
}