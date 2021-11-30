#!/bin/bash
#Test master rules against test cases in EU repo

if [ "$(uname -s)" == Linux ]; then
  AIFC=./aifc-bin/aifc_linux_x86_64
else
  AIFC=./aifc-bin/aifc_osx_x86_64
fi

cd "$(dirname "$0")" || exit

for f in verification-rules/*.aifc; do
  $AIFC $f -o generated/$f.json
done

for f in display-rules/*.aifc; do
  $AIFC $f -o generated/$f.jsonyp
done

for f in ch-only-rules/*.aifc; do
  $AIFC $f -o generated/$f.json
done

cd "../../../" || exit
COVIDCERT_GENERATE_VALIDATION_RULES=1 mvn test

if [[ -d "dgc-business-rules-testdata" ]]; then
  echo "DGC rule repo already exists. Not cloning again"
else
  git clone https://github.com/eu-digital-green-certificates/dgc-business-rules-testdata.git
fi



cp -R src/main/resources/CH dgc-business-rules-testdata
cd dgc-business-rules-testdata

#delete all other countries' rules to avoid failures
find . -maxdepth 1 -regextype sed -type d -regex ".*/[A-Z][A-Z]" | grep -v CH | xargs rm -r
./build.sh
