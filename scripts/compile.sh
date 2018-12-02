#
# compile all of the Java code for AUTHOR
#
# this script assumes that you have a Java compiler
#
# most of the objects are compiled by dependency. However, the
# CreateSpecialOutput..." and "xxxxSink.java" objects are dynamically loaded at runtime,
# so we explicitly compile them here.
#
# since JSON is read in Java, we need the jar's for "Jackson"

JAVAC=javac
echo "- - - - - - -"
echo "- - - - - - -"
echo "- - - - - - -"
echo "- - - - - - -"

# following for JSON
CP=$CP:jackson-core-2.8.8.jar
CP=$CP:jackson-databind-2.8.8.jar
CP=$CP:.

OBJ=BookCreate


rm  *.class

#
# compile main program and dependencies
#
echo "$JAVAC" -classpath "$CP" $OBJ.java
"$JAVAC" -classpath "$CP" $OBJ.java

#
# compile Createxxxx.java objects (dynamic)
#

echo "$JAVAC" -classpath "$CP" Create*.java
"$JAVAC" -classpath "$CP" Create*.java

#
# compile "xxxSink.java" objects (dynamic)
#
echo "$JAVAC" -classpath "$CP" *Sink.java
"$JAVAC" -classpath "$CP" *Sink.java

