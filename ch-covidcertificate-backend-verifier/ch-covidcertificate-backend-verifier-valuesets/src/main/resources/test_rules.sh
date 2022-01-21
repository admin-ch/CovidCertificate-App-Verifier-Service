#!/bin/bash
#Test master rules against test cases in EU repo
set -eux
if [ "$(uname -s)" == Linux ]; then
  AIFC=./aifc-bin/aifc_linux_x86_64
elif [ "$(uname -s)" == Darwin ]; then
  if [ "$(uname -p)" == arm ]; then
    AIFC=./aifc-bin/aifc_osx_aarch64
  else
    AIFC=./aifc-bin/aifc_osx_x86_64
  fi 
fi

cd "$(dirname "$0")" || exit
RESOURCES_DIR=$(pwd)

rm -r generated/*
mkdir -p generated/verification-rules
mkdir -p generated/display-rules
mkdir -p generated/ch-only-rules
mkdir -p generated/mode-rules

for f in verification-rules/*.aifc; do
  $AIFC $f -o generated/$f.json
done

for f in display-rules/*.aifc; do
  $AIFC $f -o generated/$f.json
done

for f in ch-only-rules/*.aifc; do
  $AIFC $f -o generated/$f.json
done

for f in mode-rules/*.aifc; do
  $AIFC $f -o generated/$f.json
done

cd "../../../" || exit
COVIDCERT_GENERATE_VALIDATION_RULES=1 mvn test

cd "../.."
mkdir -p external-tests
cd "external-tests"

if [[ -d "dgc-business-rules-testdata" ]]; then
  echo "DGC rule repo already exists. Not cloning again"
else
  git clone https://github.com/eu-digital-green-certificates/dgc-business-rules-testdata.git
fi

cp -R "$RESOURCES_DIR/CH" dgc-business-rules-testdata
cd dgc-business-rules-testdata

#delete all other countries' rules to avoid failures
find . -maxdepth 1 -regextype sed -type d -regex ".*/[A-Z][A-Z]" | grep -v CH | xargs -r rm -rf
./build.sh

cd ..

if [[ -d "CovidCertificate-SDK-Kotlin" ]]; then
  echo "SDK repo already exists. Not cloning again"
else
  git clone git@github.com:admin-ch/CovidCertificate-SDK-Kotlin.git
fi

VALUESETS=$(curl https://www.cc-d.bit.admin.ch/trust/v2/verificationRules | jq ".valueSets")
cd CovidCertificate-SDK-Kotlin
git reset --hard origin/main
jq "{validDuration: .validDuration, rules: .rules, displayRules: .displayRules, valueSets: $VALUESETS, modeRules: .modeRules}" > nationalrules.json < "$RESOURCES_DIR/verificationRulesV2.json"
./gradlew test --tests ch.admin.bag.covidcertificate.sdk.core.verifier.*
