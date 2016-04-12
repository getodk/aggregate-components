package org.opendatakit.apache.commons.exec;

import java.io.InputStream;

/**
 * Builder that returns (or creates) the appropriate AbstractStreamPumper for the 
 * indicated stream type.
 * 
 * @author mitchellsundt@gmail.com
 *
 */
public interface StreamPumperBuilder {
  public enum StreamType { OUT, ERR };
  
  /**
   * Implementation is expected to terminate the OUT and ERR streams.
   *  
   * @param type type of stream this is (relative to command being executed -- output or error stream).
   * @param is InputStream portion of this linkage.
   * @return the appropriate AbstractStreamPumper for the indicated stream type.
   */
  public AbstractStreamPumper newStreamPumper(StreamType type, InputStream is);
}
