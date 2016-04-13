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

import java.util.ListResourceBundle;

/**
 * Strings that we could translate if we want to internationalize this 
 * applicaiton.
 * 
 * @author mitchellsundt@gmail.com
 *
 */
public class TranslatedStrings extends ListResourceBundle {

  public static final String LAUNCH_FAILED = "a";
  public static final String DIR_RUNNABLE_JAR = "b";
  public static final String INSTALL_ROOT_REQUIRED = "c";
  public static final String EMAIL_LABEL = "d";
  public static final String TOKEN_GRANTING_LABEL = "e";
  public static final String GET_TOKEN_LABEL = "f";
  public static final String SET_TOKEN_LABEL = "g";
  public static final String VERIFY_TOKEN_LABEL = "h";
  public static final String DELETE_TOKEN_LABEL = "i";
  public static final String UPLOAD_LABEL = "j";
  public static final String ROLLBACK_LABEL = "k";
  public static final String VERSION_INFO = "l";
  
  public static final String ARG_IS_REQUIRED = "m";
  public static final String ARG_IS_REQUIRED_CMD = "n";
  public static final String CONFLICTING_ARGS_CMD = "o";

  public static final String BAD_INSTALL_ROOT_PATH = "p";
  
  public static final String WARNING_ERRANT_LABEL = "q";
  public static final String WARNING_REDIRECT_LABEL = "r";

  public static final String EMAIL_ARG_DESC = "s";
  public static final String TOKEN_GRANTING_CODE_ARG_DESC = "t";
  public static final String SDK_ROOT_ARG_DESC = "u";
  public static final String INSTALL_ROOT_ARG_DESC = "v";
  public static final String HELP_ARG_DESC = "w";
  public static final String VERSION_ARG_DESC = "x";
  public static final String NO_UI_ARG_DESC = "y";
  public static final String UPLOAD_ARG_DESC = "z";
  public static final String ROLLBACK_ARG_DESC = "aa";
  public static final String CLEAR_ARG_DESC = "bb";

  public static final String STATUS_LBL = "cc";
  public static final String SUCCEEDED_ACTION = "dd";
  public static final String ABORTED_BY_USER_ACTION = "ee";
  public static final String FAILED_BUT_OK_ACTION = "ff";
  public static final String OK_LBL = "gg";
  public static final String TITLE_ENTER_AUTH_TOKEN_GRANTING_CODE_LBL = "hh";
  public static final String ENTER_TOKEN_GRANTING_CODE_LBL = "ii";
  public static final String OUTPUT_LBL = "jj";
  public static final String FILE_NOT_FOUND_EXCEPTION = "kk";
  public static final String IO_EXCEPTION = "ll";
  
  public static final String AGG_INSTALLER_VERSION = "zz";
  
  @Override
  protected Object[][] getContents() {
    return new Object[][] {
    // LOCALIZE THIS
        {LAUNCH_FAILED, "Launch Failed: %1$s"},
        {DIR_RUNNABLE_JAR, "Directory of this runnable Jar: %1$s"},
        {INSTALL_ROOT_REQUIRED, "Unable to determine path to %1$s. Relaunch supplying %2$s argument value."},
        {EMAIL_LABEL, "Email of Google Cloud Platform account (the owner of your App Engine):"},
        {TOKEN_GRANTING_LABEL, "Code from Google's account authorization web page (granting access for Google App Engine appcfg):"},
        {GET_TOKEN_LABEL, "Get Token"},
        {SET_TOKEN_LABEL, "Set Token"},
        {VERIFY_TOKEN_LABEL, "Verify Token"},
        {DELETE_TOKEN_LABEL, "Delete Token"},
        {UPLOAD_LABEL, "Upload"},
        {ROLLBACK_LABEL, "Rollback"},
        {VERSION_INFO, "version %1$s"},
        
        {ARG_IS_REQUIRED, "%1$s is required"},
        {ARG_IS_REQUIRED_CMD, "%1$s is required when executing a command"},
        {CONFLICTING_ARGS_CMD, "Conflicting arguments. Only one of %1$s or %2$s can be specified."},

        {BAD_INSTALL_ROOT_PATH, "Bad %1$s path: %2$s"},
        
        {WARNING_ERRANT_LABEL, "<html>A token was detected on your system. Click on \"Upload\" to upload ODK Aggregate to your App Engine instance. If the token is invalid, you may be redirected to a browser to obtain a code from Google's account authorization web page. If that occurs, copy that code into the pop-up window that will appear and click OK to proceed.</html>"},
        {WARNING_REDIRECT_LABEL, "<html>After entering the Email address, Click on \"Get Token.\" <br>You will be redirected to a browser to obtain a code from Google's account authorization web page. <br>Copy that code into the pop-up window that will appear and click OK to proceed.</html>"},

        {EMAIL_ARG_DESC, "Email of Google Cloud Platform account (the owner of your App Engine)"},
        {TOKEN_GRANTING_CODE_ARG_DESC, "Code from Google's account authorization web page (granting access for Google App Engine appcfg)"},
        {SDK_ROOT_ARG_DESC, "Specify path to app engine sdk"},
        {INSTALL_ROOT_ARG_DESC, "Specify path to ODK Aggregate directory produced by installer"},
        {HELP_ARG_DESC, "Print help information (this screen)"},
        {VERSION_ARG_DESC, "Print version information"},
        {NO_UI_ARG_DESC, "Run without a GUI (as a command line program)"},
        {CLEAR_ARG_DESC, "Clear all tokens (delete the Oauth2 Credentials file)"},
        {UPLOAD_ARG_DESC, "Upload ODK Aggregate to App Engine"},
        {ROLLBACK_ARG_DESC, "Rollback any failed/stuck upload of ODK Aggregate to App Engine"},

        {STATUS_LBL, "status"},
        {SUCCEEDED_ACTION, "Action Succeeded!"},
        {ABORTED_BY_USER_ACTION, "Action failed or aborted by user"},
        {FAILED_BUT_OK_ACTION, "Action failed (this is expected)"},
        {OK_LBL, "OK"},
        {TITLE_ENTER_AUTH_TOKEN_GRANTING_CODE_LBL, "Enter Code from Google's Account Authorization Web Page"},
        {ENTER_TOKEN_GRANTING_CODE_LBL, "Enter Code from Google's Account Authorization web page"},
        {OUTPUT_LBL, "Output:"},
        {FILE_NOT_FOUND_EXCEPTION, "File Not Found Exception"},
        {IO_EXCEPTION, "IO Exception"},

        {AGG_INSTALLER_VERSION, "ODK Aggregate App Engine Updater - Version %1$s"}
    // END OF MATERIAL TO LOCALIZE
    };
  }

}
