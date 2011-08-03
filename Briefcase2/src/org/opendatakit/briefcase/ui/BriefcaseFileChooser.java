package org.opendatakit.briefcase.ui;

import java.awt.Container;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.opendatakit.briefcase.util.FormFileUtils;

class BriefcaseFileChooser extends JFileChooser {

	/**
	 * 
	 */
	private final Container parentWindow;
	/**
	 * 
	 */
	private static final long serialVersionUID = 7687033156045655297L;
	
	/**
	 * 
	 * @param f
	 * @param parentWindow
	 * @return true if directory is a valid briefcase directory.
	 */
	public static final boolean testAndMessageBadBriefcaseFolder(File f, Container parentWindow) {
		if ( !FormFileUtils.isValidBriefcaseFolder(f) ) {
			JOptionPane.showMessageDialog(parentWindow, 
					"Not a Briefcase directory (does not exist, not empty or did not find " +
					File.separator + "forms and/or " + File.separator + "scratch directories)", 
					"Invalid Briefcase Directory", JOptionPane.ERROR_MESSAGE);
			return false;
		} else if (  FormFileUtils.isUnderODKFolder(f) || FormFileUtils.isUnderBriefcaseFolder(f)){
			JOptionPane.showMessageDialog(parentWindow, 
				"Directory appears to be nested within an enclosing Briefcase or ODK Device directory",
				"Invalid Briefcase Directory", JOptionPane.ERROR_MESSAGE);
			return false;
		} else {
			return true;
		}
	}
	
	@Override
	public void approveSelection() {
		File f = this.getSelectedFile();
		if ( testAndMessageBadBriefcaseFolder(f, parentWindow) ) {
			super.approveSelection();
		}
	}

	BriefcaseFileChooser(Container parentWindow) {
		super();
		this.parentWindow = parentWindow;
		setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	}
}