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

import org.opendatakit.apache.commons.exec.StreamPumperBuilder.StreamType;

/**
 * Moved the guts of PumpStreamHandler here.
 * 
 * Changes:
 *   - move initializer action into init() method.
 *   - pass in a StreamPumperBuilder to enable changing those implementations.
 *   
 * Copies standard output and error of sub-processes to standard output and error
 * of the parent process. 
 * 
 * @version $Id: PumpStreamHandler.java 1557263 2014-01-10 21:18:09Z ggregory $
 */
public abstract class AbstractPumpStreamHandler implements ExecuteStreamHandler {

    private OutputStream inSource;
    
    private AbstractStreamPumper outputPumper;

    private AbstractStreamPumper errorPumper;
    
    private StreamPumperBuilder streamPumperBuilder;

    /** the last exception being caught */
    private IOException caught = null;

    /**
     * Construct a new <CODE>PumpStreamHandler</CODE>.
     * 
     * @param streamPumperBuilder a builder that returns the stream pumper for the out and err streams.
     */
    public void init(final StreamPumperBuilder streamPumperBuilder) {
        this.streamPumperBuilder = streamPumperBuilder;
    }

    /**
     * Direct writing of input stream.
     * 
     * @param buffer
     * @param offset
     * @param length
     * @throws IOException
     */
    public void writeInputStream(byte[] buffer, int offset, int length) throws IOException {
      inSource.write(buffer, offset, length);
      inSource.flush();
    }
    
    /**
     * Set the <CODE>InputStream</CODE> from which to read the standard output
     * of the process.
     *
     * @param is the <CODE>InputStream</CODE>.
     */
    public void setProcessOutputStream(final InputStream is) {
      outputPumper= streamPumperBuilder.newStreamPumper(StreamType.OUT, is);
    }

    /**
     * Set the <CODE>InputStream</CODE> from which to read the standard error
     * of the process.
     *
     * @param is the <CODE>InputStream</CODE>.
     */
    public void setProcessErrorStream(final InputStream is) {
      errorPumper = streamPumperBuilder.newStreamPumper(StreamType.ERR, is);
    }

    /**
     * Set the <CODE>OutputStream</CODE> by means of which input can be sent
     * to the process.
     *
     * @param os the <CODE>OutputStream</CODE>.
     */
    public void setProcessInputStream(final OutputStream os) {
        inSource = os;
    }

    /**
     * Start the <CODE>Thread</CODE>s.
     */
    public void start() {
      outputPumper.start();
      errorPumper.start();
    }

    /**
     * Stop pumping the streams. When a timeout is specified it it is not guaranteed that the
     * pumper threads are cleanly terminated.
     */
    public void stop() throws IOException {

        inSource.close();
        outputPumper.signalShouldClose();
        errorPumper.signalShouldClose();

        if (caught != null) {
            throw caught;
        }
    }
}
