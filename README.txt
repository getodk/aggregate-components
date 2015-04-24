This directory contains these projects:

(1) CleanDatastore

A utility used when unit testing to wipe a Google AppEngine datastore. 
No usage documentation.

(2) GaeHttpClient

Apache HttpClient is not supported on Google AppEngine. 
This code is a more generic implementation of the 
functionality first described in the blog posts:

http://peterkenji.blogspot.com/2009/08/using-apache-httpclient-4-with-google.html
and
http://esxx.blogspot.com/2009/06/using-apaches-httpclient-on-google-app.html

It works specifically with httpClient 4.2.2 libraries and 
supports variable deadlines (timeouts), redirects, certificate
validation, and expect-continue semantics (provided Google's 
UrlFetchService supports those semantics).

Version was updated to 1.1.2 with Java 1.7 compiler and AppEngine 1.9.18

(3) TomcatUtils

A wrapper to the Javax ImageIO library for resizing the images 
on Aggregate to provide thumbnail images on Tomcat deployments.
On Google AppEngine, this functionality is provided through a 
different set of APIs.

NOTE: The Javax ImageIO library is not redistributable. Included in 
the /lib directory of this project is a subset of the library that
is redistributable.

This was updated to 1.0.1 with the change to use the ImageIO library
version 1.3.0 here:
		<dependency>
			<groupId>com.github.jai-imageio</groupId>
			<artifactId>jai-imageio-core</artifactId>
			<version>1.3.0</version>
		</dependency>

(4) OpenID4Java

This is no longer maintained and has been deleted. 

(5) spring-security-patch

This is no longer maintained and has been deleted. 

(6) UsableServerDiagnosticApp

This is the GWT sample app with an additional Server Information page.
If a user has trouble running ODK Aggregate on a hosting service, 
use this application to verify that the service properly handles 
GWT applications and is Tomcat 6.

(7) gwt-visualization-1.1.2

This is formerly a zip found at:
  https://code.google.com/p/gwt-google-apis/
With the decommissioning of Googlecode, the
zip was copied and exploded into this project.

(8) gwt-google-maps-v3

The gwt-google-maps-v3-1.0.1.jar is built from sources that
have been updated to work with GWT 2.7.0.

The gwt-google-maps-v3-snapshot.jar was built from code formerly at:
  http://code.google.com/p/gwt-google-maps-v3/
Unfortunately, that has since been removed, and, even then, the 
uploaded jars on that site do not include both the source and the
compiled classes, so they failed during GWT compiles.

To build, do the following:
(1) compile the project -- should compile with 11 warnings
(7) Export... / Java / JAR File
(8) Choose: Export generated class files and resources
(9) Choose: Export Java source files and resources
(10) Enter JAR file (gwt-google-maps-v3-snapshot.jar) and choose to compress contents.
(11) Finish
(12) You'll get a warning that it exported with warnings (the original 11).
