package org.opendatakit.briefcase.ui;

import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JTextField;

public class TransformPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7169316129011796197L;
	
	private final JTextField txtBriefcaseDir;
	
	/**
	 * Create the panel.
	 */
	public TransformPanel(JTextField txtBriefcaseDir) {
		this.txtBriefcaseDir = txtBriefcaseDir;
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		Component[] com = this.getComponents();  
		for (int a = 0; a < com.length; a++) {  
		     com[a].setEnabled(enabled);  
		}
	}

}
