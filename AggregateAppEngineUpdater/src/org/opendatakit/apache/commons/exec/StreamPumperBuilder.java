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
