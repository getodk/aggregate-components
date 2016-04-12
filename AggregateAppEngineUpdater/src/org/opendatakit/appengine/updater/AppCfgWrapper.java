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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.lang.SystemUtils;
import org.opendatakit.apache.commons.exec.CommandLine;
import org.opendatakit.apache.commons.exec.DefaultExecutor;
import org.opendatakit.apache.commons.exec.ExecuteException;
import org.opendatakit.apache.commons.exec.ExecuteResultHandler;
import org.opendatakit.appengine.updater.exec.extended.LegacyRemovalPumpStreamHandler;
import org.opendatakit.appengine.updater.exec.extended.MonitoredPumpStreamHandler;

public class AppCfgWrapper {
  
  /**
   * Attempt to return a File object that holds the 
   * oauth2 tokens.
   *  
   * @return null if a suitable home directory can't be found.
   */
  public static File locateTokenFile() {
    String userhome = System.getProperty("user.home");
    String althome = null;
    if ( SystemUtils.IS_OS_WINDOWS ) {
      String username = System.getProperty("user.name");
      althome = "C:\\Users\\" + username;
    }
    File dirHome = new File(userhome);
    if ( !dirHome.exists() ) {
      if ( althome == null ) {
        return null;
      } else {
        dirHome = new File(althome);
        if ( !dirHome.exists() ) {
          return null;
        }
      }
    }
    File tokenFile = new File(dirHome, Preferences.APP_ENGINE_OAUTH2_JAVA_FILENAME);
    return tokenFile;
  }

  /**
   * When invoking Java, or the tools, everything is entirely broken w.r.t. spaces appearing
   * in the path and file names. The underlying implementation uses ProcessBuilder to 
   * construct the process. But even though we pass an array of arguments, these appear to 
   * be appended before invoking the command, causing everything to break if these have spaces
   * regardless of what escaping or quoting we do for Linux. 
   * 
   * Therefore, we force the current working directory to be the directory containing this jar
   * and then reference everything as a relative path from this jar location. This eliminates 
   * the need for spaces in any of the arguments.
   * 
   * @param args
   * @return
   */
  private static InvokationObject buildAppCfgInvokation(EffectiveArgumentValues args, int millisecondTimeout) {
    File app;
    // all the scripts are crap. 
    // directly launch java
    // we already know it is Java 7.
    // we already know where it is.
    
    InvokationObject iObj = new InvokationObject();

    System.out.println(args.install_root.getAbsolutePath());

    // point environment to the android sdk
    iObj.envMap = new HashMap<String,String>(System.getenv());
//    File sdk_root = new File(args.install_root, APP_ENGINE_SDK_DIRNAME);
//    iObj.envMap.put("ANDROID_HOME", sdk_root.getAbsolutePath());

    // substitution map
    iObj.substitutionMap = new HashMap<String,Object>();
    iObj.substitutionMap.put("email",  args.email);
    if ( args.token != null ) {
      iObj.substitutionMap.put("token", args.token);
    }
    iObj.substitutionMap.put("sdk_root", "." + File.separator + Preferences.APP_ENGINE_SDK_DIRNAME);
    iObj.substitutionMap.put("legacy_install", "." + File.separator + Preferences.LEGACY_REMOVAL_DIRNAME);
    iObj.substitutionMap.put("modules_install", "." + File.separator + Preferences.ODK_AGGREGATE_EAR_DIRNAME);

    // declare the executor with a working directory of the sdk root
    iObj.executor = new DefaultExecutor();
    iObj.executor.setWorkingDirectory(args.install_root);

    // find java and declare it as the executable
    String javahome = System.getProperty("java.home");
    if ( javahome == null ) {
      throw new IllegalStateException("unable to find java.home property");
    }
    System.out.println("java.home path: " + javahome);
    app = new File( new File( new File(javahome), "bin"), "java");
    System.out.println("java.home java path: " + app.getAbsolutePath());
    iObj.cmdLine = new CommandLine(app);
    iObj.cmdLine.setSubstitutionMap(iObj.substitutionMap);

    // from run_java.sh script
    iObj.cmdLine.addArgument("-Xmx1100m");
    // NOTE: working directory is set to the android sdk directory
    // use relative paths without spaces to ensure that jar is visible.
    // if there are any spaces in the classpath, then nothing works.
    // need to declare a classpath because there is no Main entry in the
    // appengine-tools-api.jar manifest.
    iObj.cmdLine.addArgument("-cp");
    iObj.cmdLine.addArgument("." + File.separator + Preferences.APP_ENGINE_SDK_DIRNAME + "/lib/appengine-tools-api.jar");
    iObj.cmdLine.addArgument("com.google.appengine.tools.admin.AppCfg");
    // iObj.cmdLine.addArgument("--no_cookies");
    iObj.cmdLine.addArgument("--oauth2");
    iObj.cmdLine.addArgument("--noisy");

    return iObj;
  }
  
  public static void getToken(EffectiveArgumentValues args, ExecuteResultHandler executionHandler) {
    AppCfgActions action = AppCfgActions.getToken;
    InvokationObject iObj = buildAppCfgInvokation(args, 60000);
    
    if ( args.token != null ) {
      action = AppCfgActions.verifyToken;
    }

    iObj.cmdLine.addArgument("--email=${email}");
    iObj.cmdLine.addArgument("resource_limits_info");
    iObj.cmdLine.addArgument("${legacy_install}");
    
    File outlog = new File(args.install_root, action.name() + ".stdout.log");
    File errlog = new File(args.install_root, action.name() + ".stderr.log");
    try {
      MonitoredPumpStreamHandler handler = new MonitoredPumpStreamHandler(args.token, action, outlog, errlog);
      executionHandler.setExecuteStreamHandler(handler);
      iObj.executor.setStreamHandler(handler);
      iObj.executor.execute(iObj.cmdLine, iObj.envMap, executionHandler);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      executionHandler.onProcessFailed(new ExecuteException(UpdaterWindow.t(TranslatedStrings.FILE_NOT_FOUND_EXCEPTION), -1, e));
    } catch (ExecuteException e) {
      e.printStackTrace();
      executionHandler.onProcessFailed(e);
    } catch (IOException e) {
      e.printStackTrace();
      executionHandler.onProcessFailed(new ExecuteException(UpdaterWindow.t(TranslatedStrings.IO_EXCEPTION), -1, e));
    }
  }


  public static void listBackends(EffectiveArgumentValues args, ExecuteResultHandler executionHandler) {
    AppCfgActions action = AppCfgActions.listBackends;
    InvokationObject iObj = buildAppCfgInvokation(args, 60000);

    iObj.cmdLine.addArgument("--sdk_root=${sdk_root}");
    iObj.cmdLine.addArgument("--email=${email}");
    iObj.cmdLine.addArgument("backends");
    iObj.cmdLine.addArgument("${legacy_install}");
    iObj.cmdLine.addArgument("list");

    File outlog = new File(args.install_root, action.name() + ".stdout.log");
    File errlog = new File(args.install_root, action.name() + ".stderr.log");
    try {
      LegacyRemovalPumpStreamHandler handler = new LegacyRemovalPumpStreamHandler(args.token, action, outlog, errlog);
      executionHandler.setExecuteStreamHandler(handler);
      iObj.executor.setStreamHandler(handler);
      iObj.executor.execute(iObj.cmdLine, iObj.envMap, executionHandler);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      executionHandler.onProcessFailed(new ExecuteException(UpdaterWindow.t(TranslatedStrings.FILE_NOT_FOUND_EXCEPTION), -1, e));
    } catch (ExecuteException e) {
      e.printStackTrace();
      executionHandler.onProcessFailed(e);
    } catch (IOException e) {
      e.printStackTrace();
      executionHandler.onProcessFailed(new ExecuteException(UpdaterWindow.t(TranslatedStrings.IO_EXCEPTION), -1, e));
    }
  }


  public static void deleteBackendBackground(EffectiveArgumentValues args, ExecuteResultHandler executionHandler) {
    AppCfgActions action = AppCfgActions.deleteBackendBackground;
    InvokationObject iObj = buildAppCfgInvokation(args, 60000);

    iObj.cmdLine.addArgument("--sdk_root=${sdk_root}");
    iObj.cmdLine.addArgument("--email=${email}");
    iObj.cmdLine.addArgument("backends");
    iObj.cmdLine.addArgument("${legacy_install}");
    iObj.cmdLine.addArgument("delete");
    iObj.cmdLine.addArgument("background");
    
    File outlog = new File(args.install_root, action.name() + ".stdout.log");
    File errlog = new File(args.install_root, action.name() + ".stderr.log");
    try {
      LegacyRemovalPumpStreamHandler handler = new LegacyRemovalPumpStreamHandler(args.token, action, outlog, errlog);
      executionHandler.setExecuteStreamHandler(handler);
      iObj.executor.setStreamHandler(handler);
      iObj.executor.execute(iObj.cmdLine, iObj.envMap, executionHandler);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      executionHandler.onProcessFailed(new ExecuteException(UpdaterWindow.t(TranslatedStrings.FILE_NOT_FOUND_EXCEPTION), -1, e));
    } catch (ExecuteException e) {
      e.printStackTrace();
      executionHandler.onProcessFailed(e);
    } catch (IOException e) {
      e.printStackTrace();
      executionHandler.onProcessFailed(new ExecuteException(UpdaterWindow.t(TranslatedStrings.IO_EXCEPTION), -1, e));
    }
  }


  public static void update(EffectiveArgumentValues args, ExecuteResultHandler executionHandler) {
    AppCfgActions action = AppCfgActions.update;
    InvokationObject iObj = buildAppCfgInvokation(args, 60000);

    iObj.cmdLine.addArgument("--sdk_root=${sdk_root}");
    iObj.cmdLine.addArgument("--email=${email}");
    iObj.cmdLine.addArgument("update");
    iObj.cmdLine.addArgument("${modules_install}");
    
    File outlog = new File(args.install_root, action.name() + ".stdout.log");
    File errlog = new File(args.install_root, action.name() + ".stderr.log");
    try {
      MonitoredPumpStreamHandler handler = new MonitoredPumpStreamHandler(args.token, action, outlog, errlog);
      executionHandler.setExecuteStreamHandler(handler);
      iObj.executor.setStreamHandler(handler);
      iObj.executor.execute(iObj.cmdLine, iObj.envMap, executionHandler);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      executionHandler.onProcessFailed(new ExecuteException(UpdaterWindow.t(TranslatedStrings.FILE_NOT_FOUND_EXCEPTION), -1, e));
    } catch (ExecuteException e) {
      e.printStackTrace();
      executionHandler.onProcessFailed(e);
    } catch (IOException e) {
      e.printStackTrace();
      executionHandler.onProcessFailed(new ExecuteException(UpdaterWindow.t(TranslatedStrings.IO_EXCEPTION), -1, e));
    }
  }


  public static void rollback(EffectiveArgumentValues args, ExecuteResultHandler executionHandler) {
    AppCfgActions action = AppCfgActions.rollback;
    InvokationObject iObj = buildAppCfgInvokation(args, 60000);

    iObj.cmdLine.addArgument("--sdk_root=${sdk_root}");
    iObj.cmdLine.addArgument("--email=${email}");
    iObj.cmdLine.addArgument("rollback");
    iObj.cmdLine.addArgument("${modules_install}");
    
    File outlog = new File(args.install_root, action.name() + ".stdout.log");
    File errlog = new File(args.install_root, action.name() + ".stderr.log");
    try {
      MonitoredPumpStreamHandler handler = new MonitoredPumpStreamHandler(args.token, action, outlog, errlog);
      executionHandler.setExecuteStreamHandler(handler);
      iObj.executor.setStreamHandler(handler);
      iObj.executor.execute(iObj.cmdLine, iObj.envMap, executionHandler);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      executionHandler.onProcessFailed(new ExecuteException(UpdaterWindow.t(TranslatedStrings.FILE_NOT_FOUND_EXCEPTION), -1, e));
    } catch (ExecuteException e) {
      e.printStackTrace();
      executionHandler.onProcessFailed(e);
    } catch (IOException e) {
      e.printStackTrace();
      executionHandler.onProcessFailed(new ExecuteException(UpdaterWindow.t(TranslatedStrings.IO_EXCEPTION), -1, e));
    }
  }
}
