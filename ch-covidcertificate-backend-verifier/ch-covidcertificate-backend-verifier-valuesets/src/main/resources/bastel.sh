#!/bin/bash
VALUESETS=$(curl https://www.cc-d.bit.admin.ch/trust/v2/verificationRules | jq ".valueSets")
cat ch-covidcertificate-backend-verifier/ch-covidcertificate-backend-verifier-valuesets/src/main/resources/verificationRulesV2.json | jq "{validDuration: .validDuration, rules: .rules, displayRules: .displayRules, valueSets: $VALUESETS, modeRules: .modeRules}" > nationalrules.json
git clone git@github.com:admin-ch/CovidCertificate-SDK-Kotlin.git
cd CovidCertificate-SDK-Kotlin
git reset --hard origin/main
cp ../nationalrules.json src/test/resources/
./gradlew test --tests ch.admin.bag.covidcertificate.sdk.core.verifier.*
