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

import org.opendatakit.apache.commons.exec.StreamPumperBuilder.StreamType;

/**
 * Event that is published to trigger processing of a line of
 * stdout or stderr from the appCfg process.  Processing can
 * be either emitting it to a screen or file, or analyzing it
 * to refine the success/failure status of the command.
 *
 * @author mitchellsundt@gmail.com
 */
public class PublishOutputEvent {
  public final StreamType type;
  public final AppCfgActions action;
  public final String line;

  public PublishOutputEvent(StreamType type, AppCfgActions action, String line) {
    this.type = type;
    this.action = action;
    this.line = line;
  }
}
