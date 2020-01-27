#!/bin/bash

CURRENT_DIR=.
BASEDIR=$(dirname "$0")

cd "$BASEDIR"/..
git remote add main https://github.com/reportportal/plugin-bts-rally.git
git fetch main
git checkout -t main/master
git checkout rally-transformed-to-tfs
cd "$CURRENT_DIR"