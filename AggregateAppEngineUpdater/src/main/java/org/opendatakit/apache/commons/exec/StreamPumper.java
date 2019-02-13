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

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.opendatakit.apache.commons.exec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Restructured to move most functionality into AbstractStreamPumper class, leaving
 * the guts of the read-write loop to be implemented here.
 * <p>
 * Copies all data from an input stream to an output stream.
 *
 * @version $Id: StreamPumper.java 1557263 2014-01-10 21:18:09Z ggregory $
 */
public class StreamPumper extends AbstractStreamPumper {

  /**
   * Create a new stream pumper.
   *
   * @param is input stream to read data from
   * @param os output stream to write data to.
   */
  public StreamPumper(final InputStream is, final OutputStream os) {
    super(is, os);
  }

  protected void attemptProcessInputStreamBytes(InputStream is, OutputStream os) throws IOException, InterruptedException {

    int fwdRead;
    for (; ; ) {
      fwdRead = is.available();
      if (fwdRead < 0) {
        // confirming close (may need this to propagate up chain)
        int ch = is.read();
        if (ch == -1) {
          signalShouldClose();
          break;
        }
      } else if (fwdRead > 0) {
        int ch = is.read();
        if (ch == -1) {
          signalShouldClose();
          break;
        }
        os.write(ch);
        os.flush();
      }
      Thread.sleep(10);
    }
  }
}
