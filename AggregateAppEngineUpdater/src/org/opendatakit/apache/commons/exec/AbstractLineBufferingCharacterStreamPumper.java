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

package org.opendatakit.apache.commons.exec;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Convert the input stream into a line-at-a-time buffered character stream.
 * Assumes the default Charset of the system for the input and output streams.
 * 
 * Calls processLineSoFar(line, hasNewLine) to process the incoming 
 * line before sending it to the output stream.
 * 
 * @author mitchellsundt@gmail.com
 *
 */
public abstract class AbstractLineBufferingCharacterStreamPumper extends AbstractStreamPumper {

  InputStreamReader isr;
  StringBuilder buf = new StringBuilder();
  
  /**
   * Called whenever there is output. We try to call this infrequently (e.g., only when we get 
   * a newline). The output line is buffered and extended until we get a hasNewLine value equal
   * to true.  Then we pay attention to the return value (which modifies what we will display.
   * 
   * @param line
   * @param hasNewLine
   * @return the line that should be output. Null if output should be suppressed.
   * @throws IOException 
   */
  protected abstract String processLineSoFar(String line, boolean hasNewLine) throws IOException;
  
  protected void attemptProcessInputStreamBytes(InputStream is, OutputStream os) throws IOException {

    boolean firstTime = true;
    boolean foundEof = false;
    boolean foundEol = false;
    boolean foundOneChar = false;
    
    for(;!foundEof;) {
      firstTime = true;
      while ( isr.ready() || firstTime ) {
        firstTime = false;
        char cbuf[] = new char[1];
        if ( isr.read(cbuf, 0, 1) == -1 ) {
          foundEof = true;
          signalShouldClose();
          break;
        }
        foundOneChar = true;
        if ( cbuf[0] == '\n' ) {
          foundEol = true;
          break;
        } else {
          buf.append(cbuf[0]);
        }
      }
    
      if ( foundOneChar ) {
        // got at least one character -- notify observer.
        if ( foundEol ) {
          String line;
          if (buf.length() > 0 && buf.charAt(buf.length()-1) == '\r') {
            line = buf.substring(0,  buf.length()-1);
          } else {
            line = buf.toString();
          }
          buf.setLength(0);
          line = processLineSoFar(line, true);
          if ( line != null ) {
            line = line + System.lineSeparator();
            byte[] outbuf = line.getBytes(); 
            os.write(outbuf);
            os.flush();
          }
        } else {
          String line = buf.toString();
          processLineSoFar(line, false);
        }
      }
    }
  }

  /**
   * Create a new stream pumper.
   * 
   * @param is input stream to read data from
   * @param os output stream to write data to.
   */
  public AbstractLineBufferingCharacterStreamPumper(final InputStream is, final OutputStream os) {
    super(is, os);
    isr = new InputStreamReader(is);
  }
}
