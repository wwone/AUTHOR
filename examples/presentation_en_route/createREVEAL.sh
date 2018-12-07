#!/bin/bash
#
# MUST USE bash, not SH
#
# DO NOT run script with redirection, as
# the redirection of stdout and stderr
# are ALREADY present!
#
# NOW must specify file with extension
#
# for this example, we hard-code the AUTHOR input filename
#
IN=en_route_example.auth
##IN=${1:?Must specify input filename WITH EXTENSION}
y=${IN%.*}
echo stripped filename $y

#
# already in this directory are the project.json and options.json
# so the following copy commands are commented out
#
#cp -v sandbox/proj_$y.json project.json
#cp -v sandbox/opts_$y.json options.json

JAVA=java

MEM=" -Xms100m -Xmx100m "
##MEM=" -Xms400m -Xmx400m "
##MEM=" -Xms200m -Xmx200m "
##MEM=

#
# the following listed JAR files must be in this
# directory along with the "author.jar" which is
# the executable for the AUTHOR system.
#
# ALSO, the Creat*json files must be present
#

# following for JSON
CP=$CP:jackson-core-2.8.8.jar
CP=$CP:jackson-databind-2.8.8.jar
CP=$CP:jackson-annotations-2.8.0.jar
CP=$CP:author.jar
CP=$CP:.

echo classpath= $CP

OBJ=BookCreate

function doit
{
echo "$JAVA" $MEM -classpath "$CP" $OBJ $IN $1 
time "$JAVA" $MEM -classpath "$CP" $OBJ $IN $1  >temp1_$1.txt 2>temp2_$1.txt 
}

# creates PDF (both types) and REVEAL web presentation
doit "fopprint" 
doit "foppdf" 
doit "reveal" 
wc tem*txt
exit 0
#
# REVEAL related:
#
# the AUTHOR system creates the file "reveal.html" as the sole output
# If you distribute it into another environment, it must be renamed
#
# to view REVEAL in a browser, the "pics" and "thumbs" directories
# must be present with the "reveal.html" file. In addition, the
# "utility.js" file is used by this webpage.
# 
# ALSO, to view REVEAL in a browser, note that the REVEAL distribution
# must be present with the "reveal.html" file. The most commonly used 
# directories are "js" and "css". REVEAL offers several other
# tools, which should be investigated for future AUTHOR development.
#
# check the release package for REVEAL

