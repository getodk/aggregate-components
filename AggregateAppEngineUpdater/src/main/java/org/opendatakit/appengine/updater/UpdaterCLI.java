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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import org.apache.commons.cli.CommandLine;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.apache.commons.exec.DefaultExecuteResultHandler;
import org.opendatakit.apache.commons.exec.StreamPumperBuilder.StreamType;
import org.opendatakit.appengine.updater.exec.extended.MonitoredPumpStreamHandler;

public class UpdaterCLI {

  private CommandLine cmd;
  private BufferedReader cliReader;

  public UpdaterCLI(CommandLine cmd) {
    super();
    AnnotationProcessor.process(this);// if not using AOP
    this.cmd = cmd;
    cliReader = new BufferedReader(new InputStreamReader(System.in, Charset.defaultCharset()));
  }

  public static EffectiveArgumentValues getArgs(CommandLine cmd) {
    // execute appCfg
    EffectiveArgumentValues args = new EffectiveArgumentValues();

    args.noGUI = !cmd.hasOption(ArgumentNameConstants.NO_UI);

    if (cmd.hasOption(ArgumentNameConstants.EMAIL)) {
      args.email = cmd.getOptionValue(ArgumentNameConstants.EMAIL);
    } else {
      args.email = null;
    }

    if (cmd.hasOption(ArgumentNameConstants.TOKEN_GRANTING_CODE)) {
      args.token_granting_code = cmd.getOptionValue(ArgumentNameConstants.TOKEN_GRANTING_CODE);
    } else {
      args.token_granting_code = null;
    }

    if (cmd.hasOption(ArgumentNameConstants.INSTALL_ROOT)) {
      File pathFile;
      String path = cmd.getOptionValue(ArgumentNameConstants.INSTALL_ROOT);
      pathFile = new File(path);
      args.install_root = pathFile;
    } else {
      // get location of this jar
      File myJarDir = UpdaterWindow.getJarDir();
      System.out.println(myJarDir.getAbsolutePath());
      String myJarDirName = myJarDir.getName();
      if (myJarDirName.equals(Preferences.ODK_AGGREGATE_INSTALLATION_DIRNAME)) {
        args.install_root = myJarDir;
      } else {
        throw new IllegalArgumentException(
            UpdaterWindow.fmt(TranslatedStrings.BAD_INSTALL_ROOT_PATH,
                ArgumentNameConstants.INSTALL_ROOT, myJarDir.getAbsolutePath()));
      }
    }

    return args;
  }

  public void run() throws InterruptedException {
    DefaultExecuteResultHandler handler;

    if (cmd.hasOption(ArgumentNameConstants.UPLOAD)) {

      EffectiveArgumentValues args = getArgs(cmd);
      handler = new DefaultExecuteResultHandler();
      AppCfgWrapper.listBackends(args, handler);
      handler.waitFor();
      if (handler.getException() != null) {
        System.out.println(((MonitoredPumpStreamHandler) handler.getExecuteStreamHandler()).getAction().name() + ": " +
            UpdaterWindow.t(TranslatedStrings.ABORTED_BY_USER_ACTION));
        System.exit(-1);
        return;
      }
      handler = new DefaultExecuteResultHandler();
      AppCfgWrapper.deleteBackendBackground(args, handler);
      handler.waitFor();
      if (handler.getException() != null) {
        System.out.println(((MonitoredPumpStreamHandler) handler.getExecuteStreamHandler()).getAction().name() + ": " +
            UpdaterWindow.t(TranslatedStrings.ABORTED_BY_USER_ACTION));
        System.exit(-1);
        return;
      }
      // TODO: This is broken until appcfg can actually delete a module version
      /*
      if ( args.hasNewRemoval() ) {
        handler = new DefaultExecuteResultHandler();
        AppCfgWrapper.deleteModuleBackground(args, handler);
        handler.waitFor();
        if ( handler.getException() != null ) {
          System.out.println(((MonitoredPumpStreamHandler) handler.getExecuteStreamHandler()).getAction().name() + ": " +
              UpdaterWindow.t(TranslatedStrings.ABORTED_BY_USER_ACTION));
          System.exit(-1);
          return;
        }
      }
      */
      handler = new DefaultExecuteResultHandler();
      AppCfgWrapper.update(args, handler);
      handler.waitFor();
      if (handler.getException() != null) {
        System.out.println(((MonitoredPumpStreamHandler) handler.getExecuteStreamHandler()).getAction().name() + ": " +
            UpdaterWindow.t(TranslatedStrings.ABORTED_BY_USER_ACTION));
        System.exit(-1);
        return;
      }

      if (args.isLegacyUpload()) {
        handler = new DefaultExecuteResultHandler();
        AppCfgWrapper.updateBackendBackground(args, handler);
        handler.waitFor();
        if (handler.getException() != null) {
          System.out.println(((MonitoredPumpStreamHandler) handler.getExecuteStreamHandler()).getAction().name() + ": " +
              UpdaterWindow.t(TranslatedStrings.ABORTED_BY_USER_ACTION));
          System.exit(-1);
          return;
        }
      }

      System.out.println(((MonitoredPumpStreamHandler) handler.getExecuteStreamHandler()).getAction().name() + ": " +
          UpdaterWindow.t(TranslatedStrings.SUCCEEDED_ACTION));
      System.exit(0);
      return;

    } else if (cmd.hasOption(ArgumentNameConstants.ROLLBACK)) {

      EffectiveArgumentValues args = getArgs(cmd);
      handler = new DefaultExecuteResultHandler();
      AppCfgWrapper.rollback(args, handler);
      handler.waitFor();
      if (handler.getException() != null) {
        System.out.println(((MonitoredPumpStreamHandler) handler.getExecuteStreamHandler()).getAction().name() + ": " +
            UpdaterWindow.t(TranslatedStrings.ABORTED_BY_USER_ACTION));
        System.exit(-1);
        return;
      } else {
        System.out.println(((MonitoredPumpStreamHandler) handler.getExecuteStreamHandler()).getAction().name() + ": " +
            UpdaterWindow.t(TranslatedStrings.SUCCEEDED_ACTION));
        System.exit(0);
        return;
      }

    } else if (cmd.hasOption(ArgumentNameConstants.CLEAR)) {

      File tokenFile = AppCfgWrapper.locateTokenFile();
      if (tokenFile.exists()) {
        tokenFile.delete();
        System.out.println(AppCfgActions.deleteToken.name() + ": " +
            UpdaterWindow.t(TranslatedStrings.SUCCEEDED_ACTION));
        System.exit(0);
        return;
      }

    } else {
      EffectiveArgumentValues args = getArgs(cmd);
      handler = new DefaultExecuteResultHandler();
      AppCfgWrapper.getToken(args, handler);
      handler.waitFor();
      if (handler.getException() != null) {
        System.out.println(((MonitoredPumpStreamHandler) handler.getExecuteStreamHandler()).getAction().name() + ": " +
            UpdaterWindow.t(TranslatedStrings.ABORTED_BY_USER_ACTION));
        System.exit(-1);
        return;
      } else {
        System.out.println(((MonitoredPumpStreamHandler) handler.getExecuteStreamHandler()).getAction().name() + ": " +
            UpdaterWindow.t(TranslatedStrings.SUCCEEDED_ACTION));
        System.exit(0);
        return;
      }

    }

  }

  @EventSubscriber(eventClass = TokenRequestEvent.class)
  public void readTokenField(TokenRequestEvent event) {
    try {
      System.out.println(UpdaterWindow.t(TranslatedStrings.ENTER_TOKEN_GRANTING_CODE_LBL));
      String token = cliReader.readLine();
      if (token != null) {
        token = token.trim();
        event.stream.emitToken(token);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @EventSubscriber(eventClass = PublishOutputEvent.class)
  public void displayOutput(PublishOutputEvent event) {

    String str = event.action.name() + ((event.type == StreamType.ERR) ? "!:  " : " :  ") + event.line;
    System.out.println(str);
  }

}
