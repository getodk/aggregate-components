/*
 * Copyright (C) 2010 University of Washington.
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
package org.opendatakit.uploadsubmissions.submission;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.junit.Test;
import org.opendatakit.uploadsubmissions.applet.UploadSubmissionsException;
import org.opendatakit.uploadsubmissions.applet.UploadWorker;
import org.opendatakit.uploadsubmissions.applet.UploadWorker.ActionListener;
import org.opendatakit.uploadsubmissions.test.TestUtilities;

public class SubmissionUploaderTest implements ActionListener {

	private static final Random _rand = new Random();
	private static final Logger _logger =
			Logger.getLogger(SubmissionUploaderTest.class.getName());

	@Override
	public void beforePostUrl(String submissionName, int tries, int count) {
		_logger.info(submissionName);
	}
	
	// Is this a bad test because it relies on the Submission class?
	@Test
	public void testUploadSubmissionWithTwoGoodSubmissions() throws MalformedURLException, InterruptedException, ExecutionException
	{
		testUploadSubmissions(1, 2, "non_null_submission.xml", true);
	}
	
	@Test
	public void testUploadSubmissionWithTwoBadSubmissions()
	{
		testUploadSubmissions(1, 2, "", false);
	}
	
	public void testUploadSubmissions(int submissionCountStart, int submissionCountEnd, String submissionXMLName, boolean shouldSucceed)
	{
		try
		{
	        String submissionPrefix = "submission_";
	        String pic = "pic.jpg";
	        File submissionsParentDir = TestUtilities.buildSubmissionSet(submissionCountStart, submissionCountEnd, submissionPrefix, submissionXMLName, pic);

	        URL server = new URL("http://nowhere.com/submission");
	        List<HttpResponse> responses = new ArrayList<HttpResponse>();
	        for ( int i = 0 ; i < submissionCountEnd - submissionCountStart + 1 ; ++i ) {
	        	responses.add(TestUtilities.buildSubmissionResponse(server, 201, 
																	"non null response body"));
	        }
	        
			TestUtilities factory = new TestUtilities(responses);
			UploadWorker uploader = new UploadWorker(server, this);
			
			uploader.doWork(factory, submissionsParentDir);

			// after a successful upload, the directory should be empty!
			String[] files = submissionsParentDir.list();
			assertEquals(files.length, 0);
			assertTrue(shouldSucceed);
		}
		catch (UploadSubmissionsException e)
		{
			assertTrue(!shouldSucceed);
			List<SubmissionResult> results = e.getDetailedResults();
			for ( SubmissionResult r : results ) {
				assertTrue(!r.isSuccess());
			}
		}
		catch (Exception e)
		{
			fail(e.toString());
		}
	}
	
	@Test
	public void testCheckSubmissionResultsAndDeleteSubmissionsWithAllSuccessfulSubmissions()
	{
		testCheckSubmissionResultsAndDeleteSubmissions(100);
	}
	
	@Test
	public void testCheckSubmissionResultsAndDeleteSubmissionsWithNoSuccessfulSubmissions()
	{
		testCheckSubmissionResultsAndDeleteSubmissions(0);
	}
	
	@Test
	public void testCheckSubmissionResultsAndDeleteSubmissionsWithHalfSuccessfulSubmissions()
	{
		testCheckSubmissionResultsAndDeleteSubmissions(50);
	}
	
	private void testCheckSubmissionResultsAndDeleteSubmissions(int successProb)
	{
		URL serverUrl = null;
		try {
			serverUrl = new URL("http://nowhere.com:5421/fido");
		} catch (MalformedURLException e1) {
			fail(e1.getMessage());
		}
		
		String submissionPrefix = "submission";
		String submissionXML = "submission.xml";
		String[] otherFiles = new String[]{"pic1.jpg", "pic2.png"};
		File submissionsParentDir = TestUtilities.buildSubmissionSet(1, 10, submissionPrefix, submissionXML, otherFiles);
		List<HttpResponse> responses = buildSubmissionResultSet(serverUrl, submissionsParentDir, successProb);

		TestUtilities factory = new TestUtilities(responses);
		UploadWorker uploader = new UploadWorker(serverUrl, this);

		try {
			uploader.doWork(factory, submissionsParentDir);
			
			assertEquals(successProb, 100);
		} catch ( UploadSubmissionsException e ) {
			assertTrue(successProb != 100);
			List<SubmissionResult> results = e.getDetailedResults();
			for (SubmissionResult result : results)
			{
				boolean success = result.isSuccess();
				if (successProb == 100)
					assertTrue(success);
				else if (successProb == 0)
					assertTrue(!success);
				
				if (success)
					assertTrue(!result.getFile().exists());
				else
					assertTrue(result.getFile().exists());
			}
		} catch ( Exception e ) {
			fail(e.getMessage());
		}
		
	}

	/**
	 * 
	 * @param submissionsParentDir the parent dir containing all of the submission directories under it.
	 * @param probOfSuccess percentage probability of the SubmissionResult.isSuccess() returning true (out of 100%).
	 * @return
	 */
	private List<HttpResponse> buildSubmissionResultSet(URL serverUrl, File submissionsParentDir, int probOfSuccess)
	{
		List<HttpResponse> outcomes = new ArrayList<HttpResponse>();
		File[] submissionFolders = submissionsParentDir.listFiles(new FileFilter() {
				
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
       	for (@SuppressWarnings("unused") File submissionFolder : submissionFolders)
       	{
       		int newInt = _rand.nextInt(100);
       		boolean success = (newInt <= (probOfSuccess - 1));
       		HttpResponse response;
       		if ( success ) {
       			response = TestUtilities.buildSubmissionResponse(serverUrl, 201, "success body");
       		} else {
       			response = TestUtilities.buildSubmissionResponse(serverUrl, 503, "failure body");
       		}
       		outcomes.add(response);
       	}
		return outcomes;
	}
}
