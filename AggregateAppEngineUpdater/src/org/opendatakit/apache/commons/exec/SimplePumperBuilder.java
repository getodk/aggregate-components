package org.opendatakit.apache.commons.exec;

import java.io.InputStream;

/**
 * Builder that mimics the behavior of the original PumpStreamHandler class.
 * 
 * @author mitchellsundt@gmail.com
 */
public class SimplePumperBuilder implements StreamPumperBuilder {

  @Override
  public AbstractStreamPumper newStreamPumper(StreamType type, InputStream is) {
    return new StreamPumper(is, System.err);
  }

}
