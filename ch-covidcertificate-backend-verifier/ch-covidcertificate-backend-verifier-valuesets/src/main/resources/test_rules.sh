#!/bin/bash

#Test master rules against test cases in EU repo

cd $(dirname "$0")/../../../
if [[ -d "dgc-business-rules-testdata" ]]; then
  echo "DGC rule repo already exists. Not cloning again"
else
  git clone https://github.com/eu-digital-green-certificates/dgc-business-rules-testdata.git
fi

COVIDCERT_GENERATE_VALIDATION_RULES=1 mvn test

cp -R src/main/resources/CH dgc-business-rules-testdata
cd dgc-business-rules-testdata

#delete all other countries' rules to avoid failures
find . -maxdepth 1 -regextype sed -type d -regex ".*/[A-Z][A-Z]" | grep -v CH | xargs rm -r
./build.sh
