#!/bin/bash

sbt -mem 2000 -Dsbt.log.format=false $@ release "electron win" "electron linux" "electron mac"
