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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.bushe.swing.event.EventBus;
import org.opendatakit.apache.commons.exec.AbstractLineBufferingCharacterStreamPumper;
import org.opendatakit.apache.commons.exec.AbstractPumpStreamHandler;
import org.opendatakit.apache.commons.exec.AbstractStreamPumper;
import org.opendatakit.apache.commons.exec.StreamPumperBuilder;
import org.opendatakit.apache.commons.exec.StreamPumperBuilder.StreamType;
import org.opendatakit.appengine.updater.AppCfgActions;
import org.opendatakit.appengine.updater.PublishOutputEvent;
import org.opendatakit.appengine.updater.TokenRequestEvent;

/**
 * Copied from PumpStreamHandler, then extensively modified to use
 * TriggeringStreamPumpers that look for certain strings in the output and/or
 * error streams and fire events.
 * 
 * The PumpStreamHandler class didn't have the necessary hooks to preclude
 * wholesale copying of its implementation.
 * 
 * Copies standard output and error of sub-processes to standard output and
 * error of the parent process. If output or error stream are set to null, any
 * feedback from that stream will be lost.
 *
 * @version $Id: PumpStreamHandler.java 1557263 2014-01-10 21:18:09Z ggregory $
 */
public class MonitoredPumpStreamHandler extends AbstractPumpStreamHandler {

  private AppCfgActions action;
  private String startingToken;
  private boolean tokenSent = false;
  private FileOutputStream outs;
  private FileOutputStream errs;
  /**
   * Buffers the input into lines and copies all data from the input stream to the 
   * output stream. Allows output to be modified and additional actions to be taken.
   *
   * @version $Id: StreamPumper.java 1557263 2014-01-10 21:18:09Z ggregory $
   */
  public class TriggeringStreamPumper extends AbstractLineBufferingCharacterStreamPumper {
    
    StreamType type;
    
    /**
     * Create a new stream pumper.
     * 
     * @param is input stream to read data from
     * @param os output stream to write data to.
     */
    public TriggeringStreamPumper(StreamType type, final InputStream is, final OutputStream os) {
      super(is, os);
      this.type = type;
    }

    @Override
    protected String processLineSoFar(String line, boolean hasNewLine) throws IOException {

      if ( !hasNewLine && line.equals("Please enter code: ") ) {
        if ( tokenSent || startingToken == null) {
          // TODO: communicate with user to get token
          EventBus.publish(new TokenRequestEvent(MonitoredPumpStreamHandler.this));
        } else {
          tokenSent = true;
          emitToken(startingToken);
        }
      }
      
      if ( hasNewLine ) {
        EventBus.publish(new PublishOutputEvent(type, action, line));
      }

      return line;
    }
  }

  public void emitToken(String token) throws IOException {
    try {
      token = token + System.lineSeparator() + System.lineSeparator() + "\u0003" + System.lineSeparator();
      byte[] output = token.getBytes();
      this.writeInputStream(output, 0, output.length);
    } catch ( IOException e) {
      // ignore
    }
  }
  
  /**
   * Construct a new <CODE>PumpStreamHandler</CODE>.
   * @throws IOException 
   */

  public MonitoredPumpStreamHandler(String startingToken, AppCfgActions action, File outLogFile, File errLogFile)
      throws IOException {
    super();
    
    this.startingToken = startingToken;
    this.action = action;

    outs = new FileOutputStream(outLogFile, false);
    errs = new FileOutputStream(errLogFile, false);

    init(new StreamPumperBuilder() {

      @Override
      public AbstractStreamPumper newStreamPumper(StreamType type, InputStream is) {
        switch (type) {
        default:
        case OUT:
          return new TriggeringStreamPumper(type, is, outs);
        case ERR:
          return new TriggeringStreamPumper(type, is, errs);
        }
      }});
  }
  
  public AppCfgActions getAction() {
    return action;
  }

  /**
   * consider a non-zero return value to be an error.
   */
  @Override
  public boolean isFailure(int exitValue) {
    return (exitValue != 0);
  }
}
