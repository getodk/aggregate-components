This code is a more generic implementation of the functionality first described in 
the blog posts:
http://peterkenji.blogspot.com/2009/08/using-apache-httpclient-4-with-google.html
and
http://esxx.blogspot.com/2009/06/using-apaches-httpclient-on-google-app.html

It has been updated to use httpClient 4.2.2 libraries and supports variable
deadlines (timeouts), redirects, certificate validation, and expect-continue
semantics (provided Google's UrlFetchService supports those semantics).
