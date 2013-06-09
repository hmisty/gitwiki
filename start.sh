#!/bin/bash
JAVA=/usr/bin/java
DIR=target
### create the symlink by yourself first.
JAR=$DIR/gitwiki-standalone.jar 
LOG=$DIR/gitwiki.log
PID=$DIR/gitwiki.pid

$JAVA -jar $JAR >> $LOG 2>&1 &
echo $! > $PID
echo Started successfully.
