#!/bin/bash
#
# MUST USE bash, not SH
#
# Execute the AUTHOR system to create the
# e-documents from the text source
#
# DO NOT run script with redirection, as
# the redirection of stdout and stderr
# are ALREADY present!
#
# This version of the script is not generalized,
# it is SPECIFIC to the Lisbon article
IN=lisbon.auth
##IN=${1:?Must specify input filename WITH EXTENSION}
y=${IN%.*}
echo stripped filename $y

# the following project-related files must be present in this directory:
#
# project.json
# options.json

# note from below that certain JAR files must be present in this
# directory, including the "author.jar" created when the AUTHOR
# Java code is compiled. NOTE that "author.jar" is available
# in this GitHub repository, in the "lib" folder.
#
# ALSO, if the AUTHOR source is not present in this directory, you
# must also have all of the Create*.json files (from "source"
# folder) present


JAVA=java

MEM=" -Xms100m -Xmx100m "

# following for JSON
CP=$CP:jackson-core-2.8.8.jar
CP=$CP:jackson-databind-2.8.8.jar
CP=$CP:jackson-annotations-2.8.0.jar
# following can be the "author.jar" file from the "lib" folder on GitHub
CP=$CP:author.jar
CP=$CP:.

echo classpath= $CP

OBJ=BookCreate

#
# the following code performs:
#
# READ AUTHOR input from this directory
# create "temp1xxxx.txt" file to contain standard out of the AUTHOR execution
# create "temp2xxxx.txt" file to contain standard err of the AUTHOR execution

function doit
{
echo "$JAVA" $MEM -classpath "$CP" $OBJ $IN $1 
time "$JAVA" $MEM -classpath "$CP" $OBJ $IN $1  >temp1_$1.txt 2>temp2_$1.txt 
}

# single-file HTML
doit "htmlsingle" 
# interactive FOP PDF
doit "foppdf" 
# print FOP PDF
doit "fopprint"
# Kindle
doit "kindle" 
# provide general idea of any errors or other output of the program
wc tem*txt
exit 0

# at this point, a number of files will have been created, containing
# the article content, ready for actual file creation. The HTML
# output is ready now
#
# see readme for how post-processing is performed
#
