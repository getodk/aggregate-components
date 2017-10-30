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
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.Formatter;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.apache.commons.exec.ExecuteException;
import org.opendatakit.apache.commons.exec.ExecuteResultHandler;
import org.opendatakit.apache.commons.exec.ExecuteStreamHandler;
import org.opendatakit.apache.commons.exec.StreamPumperBuilder.StreamType;

public class UpdaterWindow implements WindowListener {

  private static final int HorizontalSpacing = 20;
  
  private CommandLine cmd;
  private JFrame frame;
  private JTextField txtEmail;
  private JTextField txtToken;
  private JLabel lblWarning;
  private JTextPane editorArea;
  private JScrollPane editorScrollPane;
  private JButton btnDeleteToken;
  private JButton btnChoose;
  private JButton btnUpload;
  private JButton btnRollback;

  private StateExecuteResultHandler activeHandler;
  
  private static File myJarDir;
  private static ResourceBundle translations;

  public static File getJarDir() {
    return myJarDir;
  }
  
  public static String t(String id) {
    return translations.getString(id);
  }

  public static String fmt(String id, Object... args) {
    StringBuilder sb = new StringBuilder();
    Formatter formatter = null;
    try {
      formatter = new Formatter(sb, Locale.getDefault());
      formatter.format(t(id), args);
      return formatter.toString();
    } finally {
      if (formatter != null) {
        formatter.close();
      }
    }
  }

  /**
   * Launch the application.
   */
  public static void main(String[] args) {

    translations = ResourceBundle.getBundle(TranslatedStrings.class.getCanonicalName(), Locale.getDefault());
    
    Options options = addOptions();

    // get location of this jar
    String reflectedJarPath = UpdaterWindow.class.getProtectionDomain()
        .getCodeSource().getLocation().getFile();
    // remove %20 substitutions.
    reflectedJarPath = reflectedJarPath.replace("%20", " ");
    File myJar = new File(reflectedJarPath, Preferences.JAR_NAME);
    System.out.println("CodeSource Location: " + myJar.getAbsolutePath());
    File cleanJarPath = null;
    if ( myJar.exists() ) {
      try {
        cleanJarPath = myJar.getCanonicalFile();
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }
    if ( cleanJarPath == null ) {
      // try finding this within our working directory
      String dir = System.getProperty("user.dir");
      if ( dir != null ) {
        File myWD = new File(dir);
        if ( myWD.exists() ) {
          myJar = new File(myWD, Preferences.JAR_NAME);
          System.out.println("user.dir path: " + myJar.getAbsolutePath());
          if ( myJar.exists() ) {
            try {
              cleanJarPath = myJar.getCanonicalFile();
            } catch (Throwable t) {
              t.printStackTrace();
            }
          }
        }
      }
    }

    if ( cleanJarPath != null ) {
      myJarDir = cleanJarPath.getParentFile();
      System.out.println(fmt(TranslatedStrings.DIR_RUNNABLE_JAR, myJarDir.getAbsolutePath()));
    } else {
      myJarDir = null;
    }
    
    CommandLineParser parser = new DefaultParser();
    final CommandLine cmdArgs;

    try {
      cmdArgs = parser.parse(options, args);
    } catch (ParseException e1) {
      System.out.println(fmt(TranslatedStrings.LAUNCH_FAILED, e1.getMessage()));
      showHelp(options);
      System.exit(1);
      return;
    }

    if (cmdArgs.hasOption(ArgumentNameConstants.HELP)) {
      showHelp(options);
      System.exit(0);
      return;
    }

    if (cmdArgs.hasOption(ArgumentNameConstants.VERSION)) {
      showVersion();
      System.exit(0);
      return;
    }

    if ( myJarDir == null && ! cmdArgs.hasOption(ArgumentNameConstants.INSTALL_ROOT) ) {
      System.out.println(fmt(TranslatedStrings.INSTALL_ROOT_REQUIRED, 
                              Preferences.JAR_NAME, ArgumentNameConstants.INSTALL_ROOT));
      showHelp(options);
      System.exit(1);
      return;
    }

    // required for all operations
    if (cmdArgs.hasOption(ArgumentNameConstants.NO_UI) && !cmdArgs.hasOption(ArgumentNameConstants.EMAIL)) {
      System.out.println(fmt(TranslatedStrings.ARG_IS_REQUIRED, ArgumentNameConstants.EMAIL));
      showHelp(options);
      System.exit(1);
      return;
    }

    // update appEngine with the local configuration
    if (cmdArgs.hasOption(ArgumentNameConstants.UPLOAD)) {
      
      if (cmdArgs.hasOption(ArgumentNameConstants.CLEAR)) {
        System.out.println(fmt(TranslatedStrings.CONFLICTING_ARGS_CMD, 
            ArgumentNameConstants.UPLOAD, ArgumentNameConstants.CLEAR));
      }
      
      if (cmdArgs.hasOption(ArgumentNameConstants.ROLLBACK)) {
        System.out.println(fmt(TranslatedStrings.CONFLICTING_ARGS_CMD, 
            ArgumentNameConstants.UPLOAD, ArgumentNameConstants.ROLLBACK));
      }

      if (!cmdArgs.hasOption(ArgumentNameConstants.EMAIL)) {
        System.out.println(fmt(TranslatedStrings.ARG_IS_REQUIRED_CMD, ArgumentNameConstants.EMAIL));
      }
      showHelp(options);
      System.exit(1);
      return;
    }

    // rollback any stuck outstanding configuration transaction on appEngine infrastructure
    if (cmdArgs.hasOption(ArgumentNameConstants.ROLLBACK)) {
      
      if (cmdArgs.hasOption(ArgumentNameConstants.CLEAR)) {
        System.out.println(fmt(TranslatedStrings.CONFLICTING_ARGS_CMD, 
            ArgumentNameConstants.ROLLBACK, ArgumentNameConstants.CLEAR));
      }
      
      if (cmdArgs.hasOption(ArgumentNameConstants.UPLOAD)) {
        System.out.println(fmt(TranslatedStrings.CONFLICTING_ARGS_CMD, 
            ArgumentNameConstants.ROLLBACK, ArgumentNameConstants.UPLOAD));
      }

      if (!cmdArgs.hasOption(ArgumentNameConstants.EMAIL)) {
        System.out.println(fmt(TranslatedStrings.ARG_IS_REQUIRED_CMD, ArgumentNameConstants.EMAIL));
      }
      showHelp(options);
      System.exit(1);
      return;
    }

    if (cmdArgs.hasOption(ArgumentNameConstants.CLEAR)) {
      
      if (cmdArgs.hasOption(ArgumentNameConstants.ROLLBACK)) {
        System.out.println(fmt(TranslatedStrings.CONFLICTING_ARGS_CMD, 
            ArgumentNameConstants.CLEAR, ArgumentNameConstants.ROLLBACK));
      }
      
      if (cmdArgs.hasOption(ArgumentNameConstants.UPLOAD)) {
        System.out.println(fmt(TranslatedStrings.CONFLICTING_ARGS_CMD, 
            ArgumentNameConstants.CLEAR, ArgumentNameConstants.UPLOAD));
      }
      showHelp(options);
      System.exit(1);
      return;
    }

    if (!cmdArgs.hasOption(ArgumentNameConstants.NO_UI)) {

      EventQueue.invokeLater(new Runnable() {
        public void run() {
          try {
            // Set System L&F
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            UpdaterWindow window = new UpdaterWindow(cmdArgs);
            window.frame
                .setTitle(fmt(TranslatedStrings.AGG_INSTALLER_VERSION, Preferences.VERSION));
            ImageIcon icon = new ImageIcon(
                UpdaterWindow.class.getClassLoader().getResource("odkupdater.png"));
            window.frame.setIconImage(icon.getImage());
            window.frame.setVisible(true);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      });
    } else {

      try {

        UpdaterCLI aggregateInstallerCLI = new UpdaterCLI(cmdArgs);
        aggregateInstallerCLI.run();
        
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Create the application.
   */
  public UpdaterWindow(CommandLine cmd) {
    super();
    AnnotationProcessor.process(this);// if not using AOP
    this.cmd = cmd;
    frame = new JFrame();
    frame.setBounds(100, 100, 680, 595);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    frame.addWindowListener(new WindowListener() {
      @Override
      public void windowOpened(WindowEvent e) {
      }

      @Override
      public void windowClosing(WindowEvent e) {
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
    });

    JLabel lblEmail = new JLabel(t(TranslatedStrings.EMAIL_LABEL));

    txtEmail = new JTextField();
    txtEmail.setFocusable(true);
    txtEmail.setEditable(true);
    txtEmail.setColumns(60);
    txtEmail.setMaximumSize( txtEmail.getPreferredSize() );
    if ( cmd.hasOption(ArgumentNameConstants.EMAIL) ) {
      txtEmail.setText(cmd.getOptionValue(ArgumentNameConstants.EMAIL));
    }
    lblEmail.setLabelFor(txtEmail);

    JLabel lblToken = new JLabel(t(TranslatedStrings.TOKEN_GRANTING_LABEL));

    txtToken = new JTextField();
    txtToken.setColumns(60);
    txtToken.setMaximumSize( txtToken.getPreferredSize() );
    txtToken.setFocusable(false);
    txtToken.setEditable(false);
    if ( cmd.hasOption(ArgumentNameConstants.TOKEN_GRANTING_CODE) ) {
      txtToken.setText(cmd.getOptionValue(ArgumentNameConstants.TOKEN_GRANTING_CODE));
    }
    lblToken.setLabelFor(txtToken);

    // set up listener for updating warning message
    txtEmail.getDocument().addDocumentListener(new DocumentListener() {

      @Override
      public void insertUpdate(DocumentEvent e) {
        updateUI();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        updateUI();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        updateUI();
      }
      
      });

    // set up listener for updating warning message
    txtToken.getDocument().addDocumentListener(new DocumentListener() {

      @Override
      public void insertUpdate(DocumentEvent e) {
        updateUI();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        updateUI();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        updateUI();
      }
      
      });
    
    if ( (txtEmail.getText().length() > 0) && ((txtToken.getText().length() > 0) || perhapsHasToken()) ) {
      lblWarning = new JLabel(t(TranslatedStrings.WARNING_ERRANT_LABEL));
    } else {
      lblWarning = new JLabel(t(TranslatedStrings.WARNING_REDIRECT_LABEL));
    }

    JLabel outputArea = new JLabel(t(TranslatedStrings.OUTPUT_LBL));
    editorArea = new JTextPane(new DefaultStyledDocument());
    editorArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    //Put the editor pane in a scroll pane.
    editorScrollPane = new JScrollPane(editorArea);
    editorScrollPane.setVerticalScrollBarPolicy(
        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    editorScrollPane.setPreferredSize(new Dimension(400, 300));
    editorScrollPane.setMinimumSize(new Dimension(10, 10));

    outputArea.setLabelFor(editorScrollPane);
    // Create a container so that we can add a title around
    // the scroll pane. Can't add a title directly to the
    // scroll pane because its background would be white.
    // Lay out the label and scroll pane from top to bottom.
    JPanel listPane = new JPanel();
    listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
    listPane.add(outputArea);
    listPane.add(Box.createRigidArea(new Dimension(0, 5)));
    listPane.add(editorScrollPane);
    listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    
    btnDeleteToken = new JButton(t(TranslatedStrings.DELETE_TOKEN_LABEL));
    btnDeleteToken.addActionListener(new DeleteTokenActionListener());
    btnDeleteToken.setEnabled(perhapsHasToken());

    btnChoose = new JButton(t(TranslatedStrings.GET_TOKEN_LABEL));
    if ( (txtEmail.getText().length() > 0) && (txtToken.getText().length() > 0) || perhapsHasToken() ) {
      if ( perhapsHasToken() ) {
        btnChoose.setText(t(TranslatedStrings.VERIFY_TOKEN_LABEL));
      } else {
        btnChoose.setText(t(TranslatedStrings.SET_TOKEN_LABEL));
      }
    } else {
      btnChoose.setText(t(TranslatedStrings.GET_TOKEN_LABEL));
    }
    btnChoose.addActionListener(new GetTokenActionListener());
    btnChoose.setEnabled(txtEmail.getText().length() > 0);
    
    btnUpload = new JButton(t(TranslatedStrings.UPLOAD_LABEL));
    btnUpload.addActionListener(new UploadActionListener());
    btnUpload.setEnabled((txtEmail.getText().length() > 0) && perhapsHasToken());
    
    btnRollback = new JButton(t(TranslatedStrings.ROLLBACK_LABEL));
    btnRollback.addActionListener(new RollbackActionListener());
    btnRollback.setEnabled((txtEmail.getText().length() > 0) && perhapsHasToken());
    
    GroupLayout groupLayout = new GroupLayout(frame.getContentPane());
    groupLayout.setHorizontalGroup(groupLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
          .addComponent(lblEmail)
          .addComponent(txtEmail)
          .addComponent(lblToken)
          .addComponent(txtToken)
          .addComponent(lblWarning)
          .addComponent(listPane)
          .addGroup(groupLayout.createSequentialGroup()
              .addComponent(btnDeleteToken)
              .addGap(3*HorizontalSpacing)
              .addComponent(btnChoose)
              .addGap(HorizontalSpacing)
              .addComponent(btnUpload)
              .addGap(3*HorizontalSpacing, 4*HorizontalSpacing, Short.MAX_VALUE)
              .addComponent(btnRollback)))
        .addContainerGap());
    
    groupLayout.setVerticalGroup(groupLayout.createSequentialGroup()
        .addContainerGap()
        .addComponent(lblEmail)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(txtEmail)
        .addPreferredGap(ComponentPlacement.UNRELATED)
        .addComponent(lblToken)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(txtToken)
        .addPreferredGap(ComponentPlacement.UNRELATED)
        .addComponent(lblWarning)
        .addPreferredGap(ComponentPlacement.UNRELATED)
        .addComponent(listPane)
        .addPreferredGap(ComponentPlacement.UNRELATED)
        .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
          .addComponent(btnDeleteToken)
          .addComponent(btnChoose)
          .addComponent(btnUpload)
          .addComponent(btnRollback))
        .addContainerGap());
    
    frame.getContentPane().setLayout(groupLayout);

    frame.addWindowListener(this);
  }

  private boolean perhapsHasToken() {
    File tokenFile = AppCfgWrapper.locateTokenFile();
    if ( tokenFile == null ) {
      return false;
    }
    return tokenFile.exists();
  }

  private EffectiveArgumentValues getArgs() {
    // execute appCfg
    EffectiveArgumentValues args = UpdaterCLI.getArgs(cmd);

    args.noGUI = false;
    
    // override if different
    if ( txtEmail.getText() != null && txtEmail.getText().trim().length() > 0 ) {
      args.email = txtEmail.getText().trim();
    }
    
    // override if different
    if ( txtToken.getText() != null && txtToken.getText().trim().length() > 0 ) {
      args.token_granting_code = txtToken.getText().trim();
    }
    return args;
  }
  
  class DeleteTokenActionListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      
      File tokenFile = AppCfgWrapper.locateTokenFile();
      if ( tokenFile.exists() ) {
        tokenFile.delete();
      }
      txtToken.setText("");
      updateUI();
    }
  }
  
  enum StepState { GET_TOKEN, VERIFY_TOKEN, LIST_BACKENDS, DELETE_BACKENDS, DELETE_MODULE_BACKGROUND, UPDATE, UPDATE_BACKGROUND, ROLLBACK, DONE, ABORTED };
  
  protected void setActiveHandler(StateExecuteResultHandler activeHandler) {
    this.activeHandler = activeHandler;
  }
  
  protected void abortAction() {
    if ( this.activeHandler != null ) {
      this.activeHandler.errorState = StepState.ABORTED;
      this.activeHandler.successState = StepState.ABORTED;
    }
  }
  
  protected boolean hasActiveHandler() {
    return (this.activeHandler != null);
  }
  
  class StateExecuteResultHandler implements ExecuteResultHandler {
    
    ExecuteStreamHandler streamHandler;
    StepState successState;
    StepState errorState;
  
    StateExecuteResultHandler(StepState successState, StepState errorState ) {
      this.successState = successState;
      this.errorState = errorState;
    }
    
    @Override
    public void onProcessComplete(int exitValue) {
      // execute appCfg
      EffectiveArgumentValues args = getArgs();
      StateExecuteResultHandler executionHandler;
      StepState nextState;
      switch ( successState ) {
      case GET_TOKEN:
        // odd state to be in
        setActiveHandler(null);
        updateUI();
        break;
      case VERIFY_TOKEN:
        // verify the token; if ok, then list backends
        executionHandler = new StateExecuteResultHandler(StepState.LIST_BACKENDS, StepState.ABORTED);
        setActiveHandler(executionHandler);
        updateUI();
        AppCfgWrapper.getToken(args, executionHandler);
        break;
      case LIST_BACKENDS:
        // list the backends; whether or not it is ok, delete the background backend.
        executionHandler = new StateExecuteResultHandler(StepState.DELETE_BACKENDS, StepState.ABORTED);
        setActiveHandler(executionHandler);
        updateUI();
        AppCfgWrapper.listBackends(args, executionHandler);
        break;
      case DELETE_BACKENDS:
        // delete the background backend; then either remove the background module or update.
        // TODO: add this once appcfg supports deleting modules
        // nextState = (args.hasNewRemoval() ? StepState.DELETE_MODULE_BACKGROUND : StepState.UPDATE);
        executionHandler = new StateExecuteResultHandler(StepState.UPDATE, StepState.ABORTED);
        setActiveHandler(executionHandler);
        updateUI();
        AppCfgWrapper.deleteBackendBackground(args, executionHandler);
        break;
      case DELETE_MODULE_BACKGROUND:
        // delete the background backend; whether or not it is ok, update.
        executionHandler = new StateExecuteResultHandler(StepState.UPDATE, StepState.ABORTED);
        setActiveHandler(executionHandler);
        updateUI();
        AppCfgWrapper.deleteModuleBackground(args, executionHandler);
        break;
      case UPDATE:
        // update
        nextState =(args.isLegacyUpload() ? StepState.UPDATE_BACKGROUND : StepState.DONE);
        executionHandler = new StateExecuteResultHandler(nextState, StepState.ABORTED);
        setActiveHandler(executionHandler);
        updateUI();
        AppCfgWrapper.update(args, executionHandler);
        break;
      case UPDATE_BACKGROUND:
        // update
        executionHandler = new StateExecuteResultHandler(StepState.DONE, StepState.ABORTED);
        setActiveHandler(executionHandler);
        updateUI();
        AppCfgWrapper.updateBackendBackground(args, executionHandler);
        break;
      case ROLLBACK:
        // rollback
        executionHandler = new StateExecuteResultHandler(StepState.DONE, StepState.ABORTED);
        setActiveHandler(executionHandler);
        updateUI();
        AppCfgWrapper.rollback(args, executionHandler);
        break;
      case DONE:
        setActiveHandler(null);
        // we need to publish because we need these to be emitted in-order
        EventBus.publish(new PublishOutputEvent(StreamType.OUT, AppCfgActions.status, t(TranslatedStrings.SUCCEEDED_ACTION)));
        updateUI();
        break;
      case ABORTED:
        setActiveHandler(null);
        updateUI();
        break;
      }
    }

    @Override
    public void onProcessFailed(ExecuteException e) {
      setActiveHandler(null);
      // we need to publish because we need these to be emitted in-order
      EventBus.publish(new PublishOutputEvent(StreamType.OUT, AppCfgActions.status, t(TranslatedStrings.ABORTED_BY_USER_ACTION)));
      updateUI();
    }

    @Override
    public void setExecuteStreamHandler(ExecuteStreamHandler obj) {
      streamHandler = obj;
    }

    @Override
    public ExecuteStreamHandler getExecuteStreamHandler() {
      return streamHandler;
    }
  };
    
    private void updateUI() {
      boolean inProgress = hasActiveHandler();
      // upon getting a response...
      if ( (txtEmail.getText().length() > 0) && ((txtToken.getText().length() > 0) || perhapsHasToken()) ) {
        lblWarning.setText(t(TranslatedStrings.WARNING_ERRANT_LABEL));
        if ( perhapsHasToken() ) {
          btnChoose.setText(t(TranslatedStrings.VERIFY_TOKEN_LABEL));
        } else {
          btnChoose.setText(t(TranslatedStrings.SET_TOKEN_LABEL));
        }
      } else {
        lblWarning.setText(t(TranslatedStrings.WARNING_REDIRECT_LABEL));
        btnChoose.setText(t(TranslatedStrings.GET_TOKEN_LABEL));
      }
      btnDeleteToken.setEnabled(!inProgress && perhapsHasToken());
      btnChoose.setEnabled(!inProgress && txtEmail.getText().length() > 0);
      btnUpload.setEnabled(!inProgress && (txtEmail.getText().length() > 0) && perhapsHasToken());
      btnRollback.setEnabled(!inProgress && (txtEmail.getText().length() > 0) && perhapsHasToken());
    }

  class GetTokenActionListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      // execute appCfg
      EffectiveArgumentValues args = getArgs();

      StateExecuteResultHandler executionHandler = new StateExecuteResultHandler(StepState.DONE, StepState.ABORTED);
      setActiveHandler(executionHandler);
      updateUI();

      AppCfgWrapper.getToken(args, executionHandler);
    }
  }

  class UploadActionListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      // execute appCfg
      EffectiveArgumentValues args = getArgs();

      StateExecuteResultHandler executionHandler = new StateExecuteResultHandler(StepState.DELETE_BACKENDS, StepState.ABORTED);
      setActiveHandler(executionHandler);
      updateUI();

      AppCfgWrapper.listBackends(args, executionHandler);
    }
  }

  class RollbackActionListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      // execute appCfg
      EffectiveArgumentValues args = getArgs();

      StateExecuteResultHandler executionHandler = new StateExecuteResultHandler(StepState.DONE, StepState.ABORTED);
      setActiveHandler(executionHandler);
      updateUI();

      AppCfgWrapper.rollback(args, executionHandler);
    }
  }

  @Override
  public void windowOpened(WindowEvent e) {

  }

  @Override
  public void windowClosing(WindowEvent e) {

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

  @EventSubscriber(eventClass = TokenRequestEvent.class)
  public void displayTokenInputField(TokenRequestEvent event) {
    TokenEntryDialog ted = new TokenEntryDialog((JFrame) SwingUtilities.getRoot(txtToken));
    int retVal = ted.showDialog();
    if (retVal == JFileChooser.APPROVE_OPTION) {
      String token = ted.getTokenValue();
      txtToken.setText(token);
      try {
        event.stream.emitToken(token);
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      abortAction();
      try {
        event.stream.emitToken("\u0003" + System.lineSeparator());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  @EventSubscriber(eventClass = PublishOutputEvent.class)
  public void displayOutput(PublishOutputEvent event) {
    displayOutput(event.type, event.action.name(), event.line);
  }
  
  public synchronized void displayOutput(StreamType type, String actionName, String line) {
    Document doc = editorArea.getDocument();

    String str = actionName + ((type == StreamType.ERR) ? "!:  " : " :  ") + line + "\r\n";
    
    try {
      doc.insertString(doc.getLength(), str, null);
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
  }
  
  static void showHelp(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("java -jar " + Preferences.JAR_NAME, options);
  }

  static void showVersion() {
    System.out.println(fmt(TranslatedStrings.VERSION_INFO, Preferences.VERSION));
  }

  /**
   * Setting up options for Command Line Interface
   * 
   * @return
   */
  static Options addOptions() {
    Options options = new Options();

    Option email = Option.builder().argName("email").hasArg().longOpt(ArgumentNameConstants.EMAIL)
        .desc(t(TranslatedStrings.EMAIL_ARG_DESC))
        .build();

    Option token = Option.builder().argName("code").hasArg().longOpt(ArgumentNameConstants.TOKEN_GRANTING_CODE)
        .desc(t(TranslatedStrings.TOKEN_GRANTING_CODE_ARG_DESC)).build();

    Option clear = Option.builder().longOpt(ArgumentNameConstants.CLEAR)
        .desc(t(TranslatedStrings.CLEAR_ARG_DESC)).build();

    Option upload = Option.builder().longOpt(ArgumentNameConstants.UPLOAD)
        .desc(t(TranslatedStrings.UPLOAD_ARG_DESC)).build();

    Option rollback = Option.builder().longOpt(ArgumentNameConstants.ROLLBACK)
        .desc(t(TranslatedStrings.ROLLBACK_ARG_DESC)).build();

    Option help = Option.builder().longOpt(ArgumentNameConstants.HELP)
        .desc(t(TranslatedStrings.HELP_ARG_DESC)).build();

    Option version = Option.builder().longOpt(ArgumentNameConstants.VERSION)
        .desc(t(TranslatedStrings.VERSION_ARG_DESC)).build();

    Option install_root = Option.builder().hasArg().argName("path").longOpt(ArgumentNameConstants.INSTALL_ROOT)
        .desc(t(TranslatedStrings.INSTALL_ROOT_ARG_DESC)).build();

    Option no_ui = Option.builder().longOpt(ArgumentNameConstants.NO_UI)
        .desc(t(TranslatedStrings.NO_UI_ARG_DESC)).build();

    options.addOption(email);
    options.addOption(token);
    options.addOption(clear);
    options.addOption(upload);
    options.addOption(rollback);
    options.addOption(help);
    options.addOption(version);
    options.addOption(install_root);
    options.addOption(no_ui);

    return options;
  }

}
