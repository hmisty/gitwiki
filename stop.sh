#!/bin/bash
JAVA=/usr/bin/java
DIR=target
### create the symlink by yourself first.
JAR=$DIR/gitwiki-standalone.jar 
LOG=$DIR/gitwiki.log
PID=$DIR/gitwiki.pid

kill -15 `cat "$PID"` >> $LOG 2>&1
if [ $? -eq 0 ]; then
	echo Exit signal sent successfully.
else
	echo Exit signal failed to send.
fi
