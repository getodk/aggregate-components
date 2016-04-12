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

/**
 * The equivalent of the original PumpStreamHandler class, now using AbstractPumpStreamHandler.
 * 
 * Copies standard output and error of sub-processes to standard output and error
 * of the parent process. If output or error stream are set to null, any feedback
 * from that stream will be lost.
 *
 * @version $Id: PumpStreamHandler.java 1557263 2014-01-10 21:18:09Z ggregory $
 */
public class PumpStreamHandler extends AbstractPumpStreamHandler {

    /**
     * Construct a new <CODE>PumpStreamHandler</CODE>.
     */
    public PumpStreamHandler() {
      super();
      init(new SimplePumperBuilder());
    }

    /**
     * Construct a new <CODE>PumpStreamHandler</CODE>.
     * 
     * @param streamPumperBuilder a builder that returns the stream pumper for the out and err streams.
     */
    public PumpStreamHandler(final StreamPumperBuilder streamPumperBuilder) {
      super();
      init(streamPumperBuilder);
    }

    /**
     * consider a non-zero return value to be an error.
     */
    @Override
    public boolean isFailure(int exitValue) {
      return (exitValue != 0);
    }
}
