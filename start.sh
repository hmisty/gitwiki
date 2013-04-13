#!/bin/bash
JAVA=/usr/bin/java
DIR=target
JAR=$DIR/gitwiki-0.1.1-SNAPSHOT-standalone.jar 
LOG=$DIR/gitwiki.log
PID=$DIR/gitwiki.pid

$JAVA -jar $JAR >> $LOG 2>&1 &
echo $! > $PID
echo Started successfully.
