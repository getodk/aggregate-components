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

/**
 * Argument values that are derived, perhaps modified in the UI, or supplied as command line arguments.
 *
 * @author mitchellsundt@gmail.com
 */
public class EffectiveArgumentValues {

  public File install_root;
  public String email;
  public String token_granting_code;

  public boolean noGUI;

  /**
   * @return true if the user has created a NewRemoval directory, indicating that modules should be removed.
   */
  // TODO: This method has to do with the UpdaterCLI.run() method which has a commented block that would use it
  public boolean hasNewRemoval() {
    File newRemoval = new File(install_root, "NewRemoval");
    return (newRemoval.exists());
  }

}
