The "lib" folder contains JAR files, and other executables needed for the AUTHOR system.

Currently, 12/8/2018, the "author.jar" has been uploaded. Thus, a user does not
need to compile the Java code, in order to run AUTHOR. Be aware, that only
the CLASS files are in this JAR, that is only the compiled Java code.

When executing AUTHOR, there are additional JSON files required for
common resource information. These files must be present in the
execution directory. They are named "Creat*.json". (The reason for
this is to allow on-the-fly formatting changes to the final e-document
output. If these files were embedded in the JAR, there would be much
processing required to make small format-related changes.)
