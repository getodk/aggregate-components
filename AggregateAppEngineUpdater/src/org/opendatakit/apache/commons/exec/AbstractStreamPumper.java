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

import org.opendatakit.apache.commons.exec.util.DebugUtils;

/**
 * Modified StreamPumper that places all the read-write handling within the
 * abstract attemptProcessInputStreamBytes method.
 * 
 * Copies all data from an input stream to an output stream.
 *
 * @version $Id: StreamPumper.java 1557263 2014-01-10 21:18:09Z ggregory $
 */
public abstract class AbstractStreamPumper implements Runnable {
  
  protected final Thread executionThread;
  
  /** the input stream to pump from */
  protected final InputStream is;

  /** the output stream to pmp into */
  protected final OutputStream os;

  /** signal worker thread that it should terminate */
  protected boolean shouldClose = false;

  /** was the end of the stream reached */
  private boolean finished;

  protected abstract void attemptProcessInputStreamBytes(InputStream is, OutputStream os)
      throws IOException, InterruptedException;

  /**
   * Create a new stream pumper.
   *
   * @param is
   *          input stream to read data from
   * @param os
   *          output stream to write data to.
   * @param closeWhenExhausted
   *          if true, the output stream will be closed when the input is
   *          exhausted.
   * @param size
   *          the size of the internal buffer for copying the streams
   */
  protected AbstractStreamPumper(final InputStream is, final OutputStream os) {
    this.is = is;
    this.os = os;
    executionThread = new Thread(this, "Exec Stream Pumper");
    executionThread.setDaemon(true);
  }

  public void start() {
    executionThread.start();
  }
  
  /**
   * Copies data from the input stream to the output stream. Terminates as soon
   * as the input stream is closed or an error occurs.
   */
  public void run() {
    synchronized (this) {
      // Just in case this object is reused in the future
      finished = false;
    }
    try {
      while (!shouldClose) {
        try {
          attemptProcessInputStreamBytes(is, os);
        } catch (InterruptedException e) {
          // ignore
        }
      }
    } catch (final Exception e) {
      // nothing to do - happens quite often with watchdog
    } finally {
      try {
        os.flush();
        os.close();
      } catch (final IOException e) {
        final String msg = "Got exception while closing exhausted output stream";
        DebugUtils.handleException(msg, e);
      }
    }
    synchronized (this) {
      finished = true;
      notifyAll();
    }
  }

  public void signalShouldClose() {
    shouldClose = true;
    if ( Thread.currentThread() != executionThread ) {
      executionThread.interrupt();
    }
  }
  
  /**
   * Tells whether the end of the stream has been reached.
   * 
   * @return true is the stream has been exhausted.
   */
  public synchronized boolean isFinished() {
    return finished;
  }

  /**
   * This method blocks until the stream pumper finishes.
   *
   * @see #isFinished()
   */
  public void waitFor() {
    for (;!isFinished();) {
      synchronized(this) {
        try {
          wait();
        } catch (InterruptedException e) {
          // notified
        }
      }
    }
  }
}
