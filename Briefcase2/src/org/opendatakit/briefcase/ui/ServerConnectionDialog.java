package org.opendatakit.briefcase.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.EmptyBorder;

import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.util.Aggregate10Utils;
import org.opendatakit.briefcase.util.Aggregate10Utils.ServerConnectionOutcome;

public class ServerConnectionDialog extends JDialog implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6224121510693483027L;
	
	private static final String PASSWORD_LABEL = "Password:";
	private static final String USERNAME_LABEL = "Username:";
	private static final String URL_LABEL = "URL:";
	private static final String CONNECT = "Connect";
	private static final String CANCEL = "Cancel";
	private final JPanel contentPanel = new JPanel();
	private JTextField textUrlField;
	private JTextField textUsernameField;
	private JTextField textPasswordField;
	private ServerConnectionInfo serverInfo = null;
	private final boolean asTarget;
	private boolean isSuccessful = false;

	/**
	 * Create the dialog.
	 */
	public ServerConnectionDialog(ServerConnectionInfo oldInfo, boolean asTarget) {
		serverInfo = oldInfo;
		this.asTarget = asTarget;
		setModalityType(ModalityType.DOCUMENT_MODAL);
		setBounds(100, 100, 450, 222);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		JLabel lblUrl = new JLabel(URL_LABEL);
		
		textUrlField = new JTextField();
		textUrlField.setColumns(10);
		
		JLabel lblUsername = new JLabel(USERNAME_LABEL);
		
		textUsernameField = new JTextField();
		textUsernameField.setColumns(10);
		
		JLabel lblPassword = new JLabel(PASSWORD_LABEL);
		
		textPasswordField = new JTextField();
		textPasswordField.setColumns(10);
		GroupLayout gl_contentPanel = new GroupLayout(contentPanel);
		gl_contentPanel.setHorizontalGroup(
			gl_contentPanel.createSequentialGroup()
				.addContainerGap()
				.addGroup(gl_contentPanel.createParallelGroup(Alignment.TRAILING)
							.addComponent(lblUrl)
							.addComponent(lblUsername)
							.addComponent(lblPassword))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING)
						.addComponent(textUrlField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(textUsernameField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(textPasswordField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						)
				.addContainerGap());
		
		gl_contentPanel.setVerticalGroup(
				gl_contentPanel.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_contentPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblUrl)
						.addComponent(textUrlField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_contentPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblUsername)
						.addComponent(textUsernameField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_contentPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblPassword)
						.addComponent(textPasswordField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addContainerGap());
		contentPanel.setLayout(gl_contentPanel);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton(CONNECT);
				okButton.setActionCommand(CONNECT);
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
				okButton.addActionListener(this);
			}
			{
				JButton cancelButton = new JButton(CANCEL);
				cancelButton.setActionCommand(CANCEL);
				buttonPane.add(cancelButton);
				cancelButton.addActionListener(this);
			}
		}
		
		if ( serverInfo != null ) {
			textUrlField.setText(serverInfo.getUrl());
			textUsernameField.setText(serverInfo.getUsername());
			textPasswordField.setText(serverInfo.getPassword());
		}
	}

	public ServerConnectionInfo getServerInfo() {
		return serverInfo;
	}

	public boolean isSuccessful() {
		return isSuccessful;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if ( CONNECT.equals(e.getActionCommand()) ) {
			// do action...
			ServerConnectionInfo info = new ServerConnectionInfo(
					textUrlField.getText(),
					textUsernameField.getText(),
					textPasswordField.getText());
			
			// TODO: check that we can connect to the server...
			if ( asTarget ) {
				Aggregate10Utils.testServerUploadConnection(info);
			} else {
				Aggregate10Utils.testServerDownloadConnection(info);
			}
			ServerConnectionOutcome outcome = Aggregate10Utils.getOutcome();
			if ( outcome.isSuccessful() ) {
				serverInfo = info;
				isSuccessful = true;
				this.setVisible(false);
			} else {
				JOptionPane.showMessageDialog(this, 
						outcome.getErrorMessage(), 
						"Invalid Server URL", JOptionPane.ERROR_MESSAGE);
				isSuccessful = false;
			}
		} else {
			// cancel...
			this.setVisible(false);
		}
	}
}
