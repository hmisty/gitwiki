#!/bin/bash
JAVA=/usr/bin/java
DIR=target
JAR=$DIR/gitwiki-0.1.0-SNAPSHOT-standalone.jar 
LOG=$DIR/gitwiki.log
PID=$DIR/gitwiki.pid

kill -0 `cat "$PID"` >> $LOG 2>&1
if [ $? -eq 0 ]; then
	echo Exit signal sent successfully.
else
	echo Exit signal failed to send.
fi
