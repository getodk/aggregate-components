/*
 * Copyright (C) 2012 University of Washington.
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

package org.opendatakit.dwc.client;

import java.io.Serializable;
import java.util.HashMap;

public class Configuration implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6138147273142015491L;

	public static String CLIENT_WEBSITE_HOSTNAME_KEY = "CLIENT_WEBSITE_HOSTNAME";
	public static String CLIENT_WEBSITE_PORT_KEY = "CLIENT_WEBSITE_PORT";
	public static String CLIENT_WEBSITE_CODESVR_PORT_KEY = "CLIENT_WEBSITE_CODESVR_PORT";
	public static String CLIENT_ID_KEY = "CLIENT_ID";
	public static String CLIENT_SECRET_KEY = "CLIENT_SECRET";

	HashMap<String,String> parameters = new HashMap<String,String>();
	
	public Configuration() {
	}
	
	public void put(String name, String value) {
		parameters.put(name, value);
	}
	
	public String get(String name) {
		return parameters.get(name);
	}
}
