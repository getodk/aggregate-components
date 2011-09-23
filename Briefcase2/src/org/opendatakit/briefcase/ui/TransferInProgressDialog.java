/*
 * Copyright (C) 2011 University of Washington.
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

package org.opendatakit.briefcase.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.RetrieveAvailableFormsFailedEvent;
import org.opendatakit.briefcase.model.RetrieveAvailableFormsSucceededEvent;
import org.opendatakit.briefcase.model.TransferAbortEvent;
import org.opendatakit.briefcase.model.TransferFailedEvent;
import org.opendatakit.briefcase.model.TransferSucceededEvent;

public class TransferInProgressDialog extends JDialog implements ActionListener, WindowListener {

  /**
	 * 
	 */
  private static final long serialVersionUID = 5411425417966734421L;
  private final JPanel contentPanel = new JPanel();
  private JLabel lblNewLabel;
  private JButton cancelButton;

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    try {
      TransferInProgressDialog dialog = new TransferInProgressDialog("Transfer in Progress...");
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.setVisible(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Create the dialog.
   */
  public TransferInProgressDialog(String label) {
    AnnotationProcessor.process(this);// if not using AOP

    setModalityType(ModalityType.APPLICATION_MODAL);
    setBounds(100, 100, 450, 124);
    getContentPane().setLayout(new BorderLayout());
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    {
      lblNewLabel = new JLabel(label);
    }
    GroupLayout gl_contentPanel = new GroupLayout(contentPanel);
    gl_contentPanel.setHorizontalGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING)
        .addGroup(
            gl_contentPanel.createSequentialGroup().addComponent(lblNewLabel)
                .addContainerGap(349, Short.MAX_VALUE)));
    gl_contentPanel.setVerticalGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING)
        .addGroup(
            gl_contentPanel.createSequentialGroup().addComponent(lblNewLabel)
                .addContainerGap(176, Short.MAX_VALUE)));
    contentPanel.setLayout(gl_contentPanel);
    {
      JPanel buttonPane = new JPanel();
      buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
      getContentPane().add(buttonPane, BorderLayout.SOUTH);
      {
        cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand("Cancel");
        buttonPane.add(cancelButton);
        cancelButton.addActionListener(this);
      }
    }
  }

  @EventSubscriber(eventClass = TransferFailedEvent.class)
  public void failedCompletion(TransferFailedEvent event) {
    this.setVisible(false);
  }

  @EventSubscriber(eventClass = TransferSucceededEvent.class)
  public void successfulCompletion(TransferSucceededEvent event) {
    this.setVisible(false);
  }

  @EventSubscriber(eventClass = RetrieveAvailableFormsFailedEvent.class)
  public void failedRemoteCompletion(RetrieveAvailableFormsFailedEvent event) {
    this.setVisible(false);
  }

  @EventSubscriber(eventClass = RetrieveAvailableFormsSucceededEvent.class)
  public void successfulRemoteCompletion(RetrieveAvailableFormsSucceededEvent event) {
    this.setVisible(false);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    cancelButton.setEnabled(false);
    EventBus.publish(TransferAbortEvent.class, new TransferAbortEvent());
  }

  @Override
  public void windowOpened(WindowEvent e) {
  }

  @Override
  public void windowClosing(WindowEvent e) {
    // if the user attempts to close the window,
    // warn that this will stop the transfer, and
    // if they still want to, do the same action
    // as the cancel button.
    int outcome = JOptionPane.showConfirmDialog(this, "Cancel the in-progress transfer?",
        "Close Window", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
    if (outcome == JOptionPane.OK_OPTION) {
      cancelButton.setEnabled(false);
      EventBus.publish(TransferAbortEvent.class, new TransferAbortEvent());
    }
  }

  @Override
  public void windowClosed(WindowEvent e) {
  }

  @Override
  public void windowIconified(WindowEvent e) {
  }

  @Override
  public void windowDeiconified(WindowEvent e) {
  }

  @Override
  public void windowActivated(WindowEvent e) {
  }

  @Override
  public void windowDeactivated(WindowEvent e) {
  }

}
