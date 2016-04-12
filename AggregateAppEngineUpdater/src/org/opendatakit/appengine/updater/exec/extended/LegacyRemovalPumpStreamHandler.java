package org.opendatakit.appengine.updater.exec.extended;

import java.io.File;
import java.io.IOException;

import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.appengine.updater.AppCfgActions;
import org.opendatakit.appengine.updater.PublishOutputEvent;

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

    if ( event.line.equals("500 Internal Server Error") ) {
      server500_error = true;
    } else if ( event.line.equals("Deleting backend: backgroundBackend 'background' has not been defined.") ) {
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
