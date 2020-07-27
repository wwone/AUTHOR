The "lib" folder contains JAR files, and other executables needed for the AUTHOR system.

The "author.jar" archive was first uploaded 12/8/2018, and updated
12/28/2018. For trying examples, a user does not
need to compile the Java code, in order to run AUTHOR. Be aware, that only
the CLASS files are in this JAR, that is only the compiled Java code.

When executing AUTHOR, there are additional JSON files required for
common resource information. These files must be present in the
execution directory. They are named "Creat*.json". (The reason for
this is to allow on-the-fly formatting changes to the final e-document
output. If these files were embedded in the JAR, there would be much
processing required to make small format-related changes.)


I have included the Vanilla CSS file in this directory as it is needed when
you deploy HTML output in the Vanilla (default) mode.

I have included the Javascript file "utility_v.js", which is needed
when you deploy HTML output in the Vanilla (default) mode, and have
local images. Local image display allows for a "thumbnail" on the
HTML page, with an option to pop up a full-size image in its own
window. The Javascript performs the popup formatting, and window
control.
