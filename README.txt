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

It works specifically with httpClient 4.1.2 libraries and 
supports variable deadlines (timeouts), redirects, certificate
validation, and expect-continue semantics (provided Google's 
UrlFetchService supports those semantics).

(3) TomcatUtils

A wrapper to the Javax ImageIO library for resizing the images 
on Aggregate to provide thumbnail images on Tomcat deployments.
On Google AppEngine, this functionality is provided through a 
different set of APIs.

NOTE: The Javax ImageIO library is not redistributable. Included in 
the /lib directory of this project is a subset of the library that
is redistributable.
