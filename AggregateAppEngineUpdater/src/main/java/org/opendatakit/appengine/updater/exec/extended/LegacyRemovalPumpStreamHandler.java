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

package org.opendatakit.appengine.updater.exec.extended;

import java.io.File;
import java.io.IOException;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.appengine.updater.AppCfgActions;
import org.opendatakit.appengine.updater.PublishOutputEvent;

/**
 * Look for strings on stdout that would suggest that this appEngine applicationId
 * does not use legacy backends or have the 'background' legacy backend.
 * <p>
 * Treat failure outcomes of the process as successes when we find any such indications.
 *
 * @author mitchellsundt@gmail.com
 */
public class LegacyRemovalPumpStreamHandler extends MonitoredPumpStreamHandler {

  boolean server500_error = false;
  boolean background_not_found = false;

  public LegacyRemovalPumpStreamHandler(String startingToken, AppCfgActions action, File outLogFile,
                                        File errLogFile) throws IOException {
    super(startingToken, action, outLogFile, errLogFile);
    AnnotationProcessor.process(this);// if not using AOP
  }

  @EventSubscriber(eventClass = PublishOutputEvent.class)
  public synchronized void displayOutput(PublishOutputEvent event) {

    if (event.line.equals("500 Internal Server Error")) {
      server500_error = true;
    } else if (event.line.equals("Deleting backend: backgroundBackend 'background' has not been defined.")) {
      background_not_found = true;
    }
  }

  /**
   * consider a non-zero return value to be an error.
   */
  @Override
  public boolean isFailure(int exitValue) {
    return (exitValue != 0) && !(server500_error || background_not_found);
  }

}
