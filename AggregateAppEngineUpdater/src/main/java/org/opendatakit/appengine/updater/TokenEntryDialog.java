/*
 * Copyright (C) 2016 University of Washington.
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

package org.opendatakit.appengine.updater;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

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

/**
 * Modal pop-up that is displayed to the user to obtain the
 * Google Token-Granting Authorization Code.
 * <p>
 * User should paste the code from their browser into the text entry field
 * and click OK.
 *
 * @author mitchellsundt@gmail.com
 */
public class TokenEntryDialog extends JDialog {

  private static final long serialVersionUID = 6753077036860161654L;

  private String tokenValue = "";
  private boolean outcome = false;

  private JTextField txtToken;
  private JButton btnOK;

  public TokenEntryDialog(Window parentWindow) {
    super(parentWindow, UpdaterWindow.t(TranslatedStrings.TITLE_ENTER_AUTH_TOKEN_GRANTING_CODE_LBL), ModalityType.DOCUMENT_MODAL);

    JLabel lblEnterToken = new JLabel(UpdaterWindow.t(TranslatedStrings.ENTER_TOKEN_GRANTING_CODE_LBL));

    txtToken = new JTextField();
    txtToken.setColumns(60);
    txtToken.setMaximumSize(txtToken.getPreferredSize());
    txtToken.setFocusable(true);
    txtToken.setEditable(true);

    btnOK = new JButton(UpdaterWindow.t(TranslatedStrings.OK_LBL));
    btnOK.setEnabled(false);
    btnOK.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        outcome = true;
        setVisible(false);
      }
    });
    // set up listener for updating warning message
    txtToken.getDocument().addDocumentListener(new DocumentListener() {

      @Override
      public void insertUpdate(DocumentEvent e) {
        changeDisplayText();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        changeDisplayText();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        changeDisplayText();
      }

      private void changeDisplayText() {
        tokenValue = txtToken.getText().trim();
        btnOK.setEnabled(tokenValue.length() > 0);
      }
    });


    GroupLayout groupLayout = new GroupLayout(getContentPane());
    groupLayout.setHorizontalGroup(groupLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
            .addComponent(lblEnterToken)
            .addComponent(txtToken)
            .addComponent(btnOK))
        .addContainerGap());
    groupLayout.setVerticalGroup(groupLayout.createSequentialGroup()
        .addContainerGap()
        .addComponent(lblEnterToken)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(txtToken)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(btnOK)
        .addContainerGap());

    setLayout(groupLayout);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    Dimension dim = getPreferredSize();
    Insets insets = parentWindow.getInsets();
    dim.height += insets.top + insets.bottom;
    dim.width += insets.left + insets.right;
    setSize(dim);
  }

  public void dispose() {
    outcome = false;
    super.dispose();
  }

  public String getTokenValue() {
    return tokenValue;
  }

  public int showDialog() {
    this.setVisible(true);
    if (outcome) {
      return JFileChooser.APPROVE_OPTION;
    } else {
      return JFileChooser.CANCEL_OPTION;
    }
  }
}
