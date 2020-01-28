#!/bin/bash

CURRENT_DIR=$PWD
BASEDIR=$(dirname "$0")

cd "$BASEDIR"/..
git remote rename origin patched
git remote add origin https://github.com/reportportal/plugin-bts-rally.git
git fetch origin
git branch master
git branch master --set-upstream-to origin
git checkout rally-transformed-to-tfs
cd "$CURRENT_DIR"