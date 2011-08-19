package org.opendatakit.briefcase.ui;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;

public class MainWindow {

	private JFrame frame;
	private JTextField txtBriefcaseDir;
	private JButton btnChoose;
	private TransferPanel transferPanel;
	private TransformPanel transformPanel;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
				    // Set System L&F
			        UIManager.setLookAndFeel(
			            UIManager.getSystemLookAndFeelClassName());

					MainWindow window = new MainWindow();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	class FolderActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
	    	// briefcase...
	    	BriefcaseFileChooser fc = new BriefcaseFileChooser(MainWindow.this.frame, true);
	    	int retVal = fc.showDialog(MainWindow.this.frame, null);
	    	if ( retVal == JFileChooser.APPROVE_OPTION ) {
	    		txtBriefcaseDir.setText(fc.getSelectedFile().getAbsolutePath());
				transformPanel.setEnabled(true);
				transferPanel.setEnabled(true);
	    	}
		}
		
	}

	/**
	 * Create the application.
	 */
	public MainWindow() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 680, 595);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JLabel lblBriefcaseDirectory = new JLabel("Briefcase Directory");
		
		txtBriefcaseDir = new JTextField();
		txtBriefcaseDir.setColumns(10);
		txtBriefcaseDir.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {
				// don't care...
			}

			@Override
			public void focusLost(FocusEvent e) {
				String briefcaseDir = txtBriefcaseDir.getText();
				if ( briefcaseDir != null && briefcaseDir.length() != 0 ) {
					File f = new File(briefcaseDir);
					if (BriefcaseFileChooser.testAndMessageBadBriefcaseFolder(f, frame)) {
						transformPanel.setEnabled(true);
						transferPanel.setEnabled(true);
					} else {
						transformPanel.setEnabled(false);
						transferPanel.setEnabled(false);
					}
				} else {
					transformPanel.setEnabled(false);
					transferPanel.setEnabled(false);
				}
			}
		});
		
		btnChoose = new JButton("Choose...");
		btnChoose.addActionListener(new FolderActionListener());
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		GroupLayout groupLayout = new GroupLayout(frame.getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(tabbedPane, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 628, Short.MAX_VALUE)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(lblBriefcaseDirectory)
							.addGap(18)
							.addComponent(txtBriefcaseDir, GroupLayout.DEFAULT_SIZE, 362, Short.MAX_VALUE)
							.addGap(18)
							.addComponent(btnChoose)))
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(txtBriefcaseDir, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(btnChoose)
						.addComponent(lblBriefcaseDirectory))
					.addGap(33)
					.addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, 446, Short.MAX_VALUE)
					.addContainerGap())
		);
		
		transferPanel = new TransferPanel(txtBriefcaseDir);
		tabbedPane.addTab("Transfer", null, transferPanel, null);
		
		transformPanel = new TransformPanel(txtBriefcaseDir);
		tabbedPane.addTab("Transform", null, transformPanel, null);
		frame.getContentPane().setLayout(groupLayout);

		transformPanel.setEnabled(false);
		transferPanel.setEnabled(false);
	}
}
