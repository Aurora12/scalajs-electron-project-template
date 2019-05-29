#!/bin/bash

sbt -mem 2000 -Dsbt.log.format=false $@ dev
