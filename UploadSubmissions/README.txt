This project uses Maven 3, for more about Maven see http://maven.apache.org/

Getting it to build:

Make a keystore
===============
1) Go into a directory outside of this project where you would like your keystore to reside.
2) Run this command to generate a keystore and key pair:
	keytool -genkey -alias <alias> -keystore <keystore-name> 
		-keypass <password> -dname "cn=<some-name>" 
		-storepass <password>
3) In that same directory outside of this project, edit jarSignerDetails.txt (create it if it doesn't exist) and add:
	jarSigner.password=<password>
	jarSigner.keystore=<path-to-keystore>
	jarSigner.signAsAlias=<alias>
	jarSigner.certDir=<anything> #This is currently not used
4) In the Maven settings.xml (located in .m2/settings.xml), 
	add or modify the <profiles> and <activeProfiles> sections.
	You want to use the filepath tot he jarSignerDetails.txt file
	you created in step (3) for the <keystore.propertyfile>
	value.
	
	A sample file might be:
-----START SAMPLE settings.xml-------
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <profiles>
  	<profile>
  	  <id>basic</id>
  	  <activation><activeByDefault/></activation>
  	  <properties>
	    <keystore.propertyfile>\C:\\Users\\user\\keystore\\jarSignerDetails.txt</keystore.propertyfile>
	  </properties>
  	</profile>
  </profiles>
  <activeProfiles><activeProfile>basic</activeProfile></activeProfiles>
</settings>
-----END SAMPLE settings.xml-------
 
5) That should set up everything properly for signing the build jars. 

Build the project
=================
mvn package -- compiles the source, runs unit tests, signs jars, and packages applet into zip file

Debug the project
=================

1) Set up an ODK Aggregate instance.  To debug the applet, you will need to allow
anonymous access to the server for the APIs the applet is using.  Do this by adding
the following roles to the USER_IS_ANONYMOUS group:

Briefcase:  ROLE_ANALYST
UploadForm: ROLE_FORM_ADMIN
UploadSubmissions: ROLE_SUBMISSION_UPLOAD

2) Configure the Java Applet Run/Debug Configurations to pass a url parameter to the 
applet that identifies the path to the applet on the server:

Parameter Name | Value
-----------------------
urlUploadXFormApplet       | http://localhost:8888/UploadXFormApplet
urlUploadSubmissionsApplet | http://localhost:8888/UploadSubmissionsApplet 
urlBriefcase               | http://localhost:8888/Briefcase

3) You should now be able to run/debug in Eclipse with this configuration 	