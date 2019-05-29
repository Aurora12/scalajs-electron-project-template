#!/bin/bash

sbt -mem 2000 -Dsbt.log.format=false $@ dev "electron win" "electron linux" "electron mac"
