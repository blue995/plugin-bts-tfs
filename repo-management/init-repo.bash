#!/bin/bash

git remote add main https://github.com/reportportal/plugin-bts-rally.git
git fetch main
git checkout -t main/master
git checkout rally-transformed-to-tfs