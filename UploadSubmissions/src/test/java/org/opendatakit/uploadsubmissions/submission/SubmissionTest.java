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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.uploadsubmissions.test.TestUtilities;

public class SubmissionTest 
{
	private static final String VARIABLE_NAME = "${name}";
	private static final String VARIABLE_FILENAME = "${filename}";
	private static final String VARIABLE_MIMETYPE = "${mimetype}";
	private static final String CONTENT_DISPOSITION = String.format("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"", VARIABLE_NAME, VARIABLE_FILENAME);
	private static final String CONTENT_TYPE = String.format("Content-Type: %s", VARIABLE_MIMETYPE);
	private static final String JPEG_EXTENSION = ".jpeg";
	private static final String JPG_EXTENSION = ".jpg";
	private static final String PNG_EXTENSION = ".png";
	
	private Submission _submission;
	
	@Before
	public void setup() throws URISyntaxException, MalformedURLException
	{
		_submission = new Submission(null, null, null, null);
	}
	
	@Test
	public void testCallWithNonNullResponse()
	{
		ProtocolVersion pv = HttpVersion.HTTP_1_1;
		HttpContext ctxt = new BasicHttpContext();
		DefaultHttpResponseFactory drf = new DefaultHttpResponseFactory();
		StatusLine sl = new BasicStatusLine(pv, 201, "non null response");
		HttpResponse r = drf.newHttpResponse(sl,ctxt);
		r.addHeader("Location","nowhere.com");
		
		testCall(r);
	}
	
	@Test
	public void testCallWithNullResponse()
	{
		testCall(null);
	}
	
	private void testCall(HttpResponse response)
	{
		try
		{
			HttpParams params = TestUtilities.getHttpParams();
			
			TestUtilities factory = new TestUtilities(Collections.singletonList(response));
			
			URL url = new URL("http://nowhere.com/submission");
			File submissionDir = TestUtilities.buildSingleSubmission("submission", "submission.xml", "pic1.jpg");
			Submission submission = new Submission(factory, params, url, submissionDir);
			SubmissionResult result = submission.submitAndDeleteLocalCopy();
			if (response != null)
			{
				assertTrue(result.isSuccess());
			}
			else
			{
				assertTrue(!result.isSuccess());
			}
			assertEquals(url, result.getAggregateURL());
		}
		catch(IOException e)
		{
			fail(e.toString());
		}
	}
	
	@Test
	public void testBuildSubmissionPostWithOneXML()
	{
		testBuildSubmissionPost("http://nowhere.com/submission", "submissionWithOneXML", "submission.xml");
	}
	
	@Test
	public void testBuildSubmissionPostWithOneXMLOneJpg()
	{
		testBuildSubmissionPost("http://nowhere.com/submission", "submissionWithOneXMLTwoPics", "submission.xml", "pic1.jpg");
	}
	
	@Test
	public void testBuildSubmissionPostWithOneXMLOneJpeg()
	{
		testBuildSubmissionPost("http://nowhere.com/submission", "submissionWithOneXMLTwoPics", "submission.xml", "pic1.jpeg");
	}
	
	@Test
	public void testBuildSubmissionPostWithOneXMLOnePng()
	{
		testBuildSubmissionPost("http://nowhere.com/submission", "submissionWithOneXMLTwoPics", "submission.xml", "pic1.png");
	}
	
	@Test
	public void testBuildSubmissionPostWithOneXMLOneJpgOneJpegOnePng()
	{
		testBuildSubmissionPost("http://nowhere.com/submission", "submissionWithOneXMLTwoPics", "submission.xml", "pic1.jpg", "pic2.jpeg", "pic3.png");
	}
	
	@Test
	public void testBuildSubmissionPostWithOneXMLOneMp3()
	{
		testBuildSubmissionPost("http://nowhere.com/submission", "submissionWithOneXMLOneMp3", "submission.xml", "shouldBeIgnored.mp3");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testBuildSubmissionPostWithTwoXML()
	{
		testBuildSubmissionPost("http://nowhere.com/submission", "submissionWithTwoXML", "submission1.xml", "throwsIllegalArgumentException.xml");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testBuildSubmissionPostWithNoXML()
	{
		testBuildSubmissionPost("http://nowhere.com/submission", "submissionWithNoXML", "");
	}
	
	private void testBuildSubmissionPost(String submissionURLString, String submissionName, String submissionFileName, String... otherFileNames)
	{
		ByteArrayOutputStream out = null;
		try 
		{
			URL submissionURL = new URL(submissionURLString);
			File submissionFolder = TestUtilities.buildSingleSubmission(submissionName, submissionFileName, otherFileNames);
			HttpPost post = _submission.buildSubmissionPost(submissionURL, submissionFolder);
			HttpEntity entity = post.getEntity();
			out = new ByteArrayOutputStream();
			entity.writeTo(out);
		}
		catch (MalformedURLException e) 
		{
			fail(e.getMessage());
		}
		catch (IOException e) 
		{
			fail(e.getMessage());
		} 
		catch (URISyntaxException e) 
		{
			fail(e.getMessage());
		} 
		String multipartEntity = out.toString();
		assertTrue(multipartEntity.contains(makeContentDispositionString(Submission.FORM_PART_XML_SUBMISSION_FILE, submissionFileName)));
		assertTrue(multipartEntity.contains(makeContentTypeString(Submission.XML_MIME_TYPE)));
		for (String otherFileName : otherFileNames)
		{
			if (otherFileName.endsWith(JPG_EXTENSION) || otherFileName.endsWith(JPEG_EXTENSION))
			{
				assertTrue(multipartEntity.contains(makeContentDispositionString(otherFileName, otherFileName)));
				assertTrue(multipartEntity.contains(makeContentTypeString(Submission.JPEG_MIME_TYPE)));
			}
			else if (otherFileName.endsWith(PNG_EXTENSION))
			{
				assertTrue(multipartEntity.contains(makeContentDispositionString(otherFileName, otherFileName)));
				assertTrue(multipartEntity.contains(makeContentTypeString(Submission.PNG_MIME_TYPE)));				
			}
			else
			{
				assertTrue(!multipartEntity.contains(makeContentDispositionString(otherFileName, otherFileName)));
			}
		}
	}
	
	private String makeContentDispositionString(String name, String filename)
	{
		return CONTENT_DISPOSITION.replace(VARIABLE_NAME, name).
			replace(VARIABLE_FILENAME, filename);		
	}
	
	private String makeContentTypeString(String mimetype)
	{
		return CONTENT_TYPE.replace(VARIABLE_MIMETYPE, mimetype);
	}
}
