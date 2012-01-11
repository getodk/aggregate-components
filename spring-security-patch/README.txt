These are the files changed to work with the special openid4java build.

Building is tricky:

0. build the custom openid4java version and install it with mvn install-file
1. download 3.1.0.RELEASE version of spring security from git
2. build per their instructions (gradlew build).
3. Confirm that gradle 1.0-milestone-3 was being used.
4. copy patches into the spring-security tree.
5. manually delete all build directories EXCEPT buildSrc\build
6. run gradlew build again.

This process first builds the buildSrc directory, which requires
the old libraries to function.  Then it applies the patches and 
makes use of a dependency-determination bug in gradle milestone-3
that prevents it from detecting changes; this necessitates deleting
the /build directories so that everything is rebuilt appropriately.

The buildSrc directory will not build after the patches are applied.




