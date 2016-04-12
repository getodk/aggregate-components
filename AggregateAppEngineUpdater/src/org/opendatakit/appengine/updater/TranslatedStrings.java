package org.opendatakit.appengine.updater;

import java.util.ListResourceBundle;

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

public class TranslatedStrings extends ListResourceBundle {

  public static final String LAUNCH_FAILED = "a2";
  public static final String DIR_RUNNABLE_JAR = "a2a";
  public static final String INSTALL_ROOT_REQUIRED = "a2b";
  public static final String EMAIL_LABEL = "a3";
  public static final String TOKEN_GRANTING_LABEL = "a3a";
  public static final String GET_TOKEN_LABEL = "a4";
  public static final String SET_TOKEN_LABEL = "a4a";
  public static final String VERIFY_TOKEN_LABEL = "a4b";
  public static final String DELETE_TOKEN_LABEL = "a4b1";
  public static final String UPLOAD_LABEL = "a4c";
  public static final String ROLLBACK_LABEL = "a4d";
  public static final String VERSION_INFO = "a5";
  
  public static final String ARG_IS_REQUIRED = "a5a";
  public static final String ARG_IS_REQUIRED_CMD = "a5b";
  public static final String CONFLICTING_ARGS_CMD = "a5b1";

  public static final String BAD_INSTALL_ROOT_PATH = "a5c";
  
  public static final String WARNING_ERRANT_LABEL = "a6";
  public static final String WARNING_REDIRECT_LABEL = "a7";

  public static final String EMAIL_ARG_DESC = "a7a";
  public static final String TOKEN_GRANTING_CODE_ARG_DESC = "a7b";
  public static final String SDK_ROOT_ARG_DESC = "a7c";
  public static final String INSTALL_ROOT_ARG_DESC = "a7d";
  public static final String HELP_ARG_DESC = "a7e";
  public static final String VERSION_ARG_DESC = "a7f";
  public static final String NO_UI_ARG_DESC = "a7g";
  public static final String UPLOAD_ARG_DESC = "a7h";
  public static final String ROLLBACK_ARG_DESC = "a7i";
  public static final String CLEAR_ARG_DESC = "a7j";

  public static final String STATUS_LBL = "a8";
  public static final String SUCCEEDED_ACTION = "a8a";
  public static final String ABORTED_BY_USER_ACTION = "a8b";
  public static final String FAILED_BUT_OK_ACTION = "a8c";
  public static final String OK_LBL = "a8d";
  public static final String TITLE_ENTER_AUTH_TOKEN_GRANTING_CODE_LBL = "a8e";
  public static final String ENTER_TOKEN_GRANTING_CODE_LBL = "a8f";
  public static final String OUTPUT_LBL = "a8g";
  public static final String FILE_NOT_FOUND_EXCEPTION = "a8h";
  public static final String IO_EXCEPTION = "a8i";
  
  public static final String AGG_INSTALLER_VERSION = "a1";
  
  @Override
  protected Object[][] getContents() {
    return new Object[][] {
    // LOCALIZE THIS
        {LAUNCH_FAILED, "Launch Failed: %1$s"},
        {DIR_RUNNABLE_JAR, "Directory of this runnable Jar: %1$s"},
        {INSTALL_ROOT_REQUIRED, "Unable to determine path to %1$s. Relaunch supplying %2$s argument value."},
        {EMAIL_LABEL, "Email:"},
        {TOKEN_GRANTING_LABEL, "Google Token-Granting Authorization Code:"},
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
        
        {WARNING_ERRANT_LABEL, "<html>A token was detected on your system. If the token is invalid, you may be redirected to a browser to obtain an token-granting code. Copy that into the pop-up window and click OK.</html>"},
        {WARNING_REDIRECT_LABEL, "<html>You will be redirected to a browser to obtain an token-granting code. Copy that into the pop-up window and click OK.</html>"},

        {EMAIL_ARG_DESC, "Google E-mail account that has ownership privileges for AppEngine Cloud project"},
        {TOKEN_GRANTING_CODE_ARG_DESC, "Token-granting-code from Google Authentication screens"},
        {SDK_ROOT_ARG_DESC, "Specify path to appengine sdk"},
        {INSTALL_ROOT_ARG_DESC, "Specify path to ODK Aggregate directory produced by installer"},
        {HELP_ARG_DESC, "Print help information (this screen)"},
        {VERSION_ARG_DESC, "Print version information"},
        {NO_UI_ARG_DESC, "Run without a GUI (as a command line program)"},
        {CLEAR_ARG_DESC, "Delete the Oauth2 Credentials file (clears all tokens)"},
        {UPLOAD_ARG_DESC, "Upload ODK Aggregate to AppEngine"},
        {ROLLBACK_ARG_DESC, "Rollback any failed/stuck upload of ODK Aggregate to AppEngine"},

        {STATUS_LBL, "status"},
        {SUCCEEDED_ACTION, "Action Succeeded!"},
        {ABORTED_BY_USER_ACTION, "Action failed or aborted by user"},
        {FAILED_BUT_OK_ACTION, "Action failed (this is expected)"},
        {OK_LBL, "OK"},
        {TITLE_ENTER_AUTH_TOKEN_GRANTING_CODE_LBL, "Enter Google Token-Granting Authorization Code"},
        {ENTER_TOKEN_GRANTING_CODE_LBL, "Enter Token-Granting Authorization Code: (from browser page)"},
        {OUTPUT_LBL, "Output:"},
        {FILE_NOT_FOUND_EXCEPTION, "File Not Found Exception"},
        {IO_EXCEPTION, "IO Exception"},

        {AGG_INSTALLER_VERSION, "ODK Aggregate AppEngine Updater - Version %1$s"}
    // END OF MATERIAL TO LOCALIZE
    };
  }

}
