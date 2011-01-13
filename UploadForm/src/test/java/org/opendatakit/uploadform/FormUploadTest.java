package org.opendatakit.uploadform;
/*
 * Copyright (C) 2010 University of Washington
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


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test cases for the simpler bits of the CsvDownload class.
 * 
 * @author mitchellsundt@gmail.com
 *
 */
public class FormUploadTest {

	static File scratchBase = null;

	private static boolean deleteDirectoryTree(File path) {
	    if( path.exists() ) {
	      File[] files = path.listFiles();
	      for(File f : files) {
	         if(f.isDirectory()) {
	        	 deleteDirectoryTree(f);
	         }
	         else {
	           f.delete();
	         }
	      }
	      return( path.delete() );
	    }
	    return true;
	}

	@BeforeClass
	public static void Initialize() {
		assertNotNull( "TEMP enviornment variable not defined", System.getenv("TEMP"));
		File scratchTemp = new File(System.getenv("TEMP"));
		assertTrue("TEMP must be writable: " + scratchTemp.getPath(), scratchTemp.canWrite());
		scratchBase = new File(scratchTemp, "briefcase-unit-test");
		assertTrue("Unable to delete old scratch directory: " + scratchBase.getPath(),
					deleteDirectoryTree(scratchBase));
		assertTrue("Unable to create scratch directory: " + scratchBase.getPath(),scratchBase.mkdir());
	}
	
	@AfterClass
	public static void Cleanup() {
		// deleteDirectoryTree(scratchBase);
	}
	
	
	@Test
	public void testDofExceptionString() {
		String format = "This file: {0} is messed up!";
		File f = new File("//foof//f/ff/f");
		String unixFormat = "This file: //foof/f/ff/f is messed up!";
		String windowsFormat = unixFormat.replace("/", "\\");
		String result = FormUpload.dofExceptionString(format, f);
		assertTrue("result: "+result+" does not equal " + windowsFormat + " or " + unixFormat, (result.equals(unixFormat) || result.equals(windowsFormat)) );

		f = new File("/");
		unixFormat = "This file: / is messed up!";
		windowsFormat = "This file: C:\\ is messed up!";
		result = FormUpload.dofExceptionString(format, f);
		assertTrue("result: "+result+" does not equal " + windowsFormat + " or " + unixFormat, (result.equals(unixFormat) || result.equals(windowsFormat)) );
	}
	
//	@Test
//	public void testStartUp1() {
//		File testDir = new File(scratchBase, "testStartUp1");
//		FormUpload app = new FormUpload("http://localhost:8888", testDir.getAbsolutePath(),
//				new FormUpload.ActionListener() {
//					@Override
//					public void beforeUploadUrl(String status) {
//						// do nothing
//					}
//				});
//	}	
}

