These are the files changed to work with the special openid4java build.

Build spring-security as follows:

0. build the custom openid4java version and install it with mvn install-file
1. download 3.1.3.RELEASE version of spring security from git
2. Edit ant build.xml to set spring-security-folder location to the git tree
3. Run ant script. This copies the patches onto the 3.1.3 tree.
4. build per their instructions (gradlew build).





