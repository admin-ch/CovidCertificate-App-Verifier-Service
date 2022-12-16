#!/bin/bash
set -xeu
cd "$(dirname "$0")" || exit
python3 ch-covidcertificate-backend-verifier/ch-covidcertificate-backend-verifier-ws/src/main/resources/dump_revocations.py
sqlite3 revocations.sqlite "CREATE TABLE revocations ( uvci text NOT NULL );" ".mode csv" ".import revocations.csv revocations"
sqlite3 revocations.sqlite "CREATE TABLE \"metadata\" (\"validDuration\" integer NOT NULL DEFAULT '0',\"lastDownload\" integer NOT NULL DEFAULT '0',\"nextSince\" text);"
sqlite3 revocations.sqlite "INSERT INTO  \"metadata\" (validDuration, lastDownload, nextSince) VALUES ($(cat revocation_metadata.json | jq .validDuration), $(cat revocation_metadata.json | jq .lastDownload), $(cat revocation_metadata.json | jq .nextSince));"

sqlite3 revocations-android.sqlite "CREATE TABLE revocations ( uvci text NOT NULL ,PRIMARY KEY (uvci));" ".mode csv" ".import revocations.csv revocations"
sqlite3 revocations-android.sqlite "CREATE TABLE \"metadata\" (\"validDuration\" integer NOT NULL DEFAULT '0',\"lastDownload\" integer NOT NULL DEFAULT '0',\"nextSince\" text NOT NULL DEFAULT '' , PRIMARY KEY (nextSince, validDuration, lastDownload));"
sqlite3 revocations-android.sqlite "INSERT INTO \"metadata\" (validDuration, lastDownload, nextSince) VALUES ($(cat revocation_metadata.json | jq .validDuration), $(cat revocation_metadata.json | jq .lastDownload), $(cat revocation_metadata.json | jq .nextSince));"

rm revocations.csv
mv revocations.sqlite ch-covidcertificate-backend-verifier/ch-covidcertificate-backend-verifier-ws/src/main/resources/
mv revocations-android.sqlite ch-covidcertificate-backend-verifier/ch-covidcertificate-backend-verifier-ws/src/main/resources/
mv revocation_metadata.json ch-covidcertificate-backend-verifier/ch-covidcertificate-backend-verifier-ws/src/main/resources/

./ch-covidcertificate-backend-verifier/ch-covidcertificate-backend-verifier-valuesets/src/main/resources/test_rules.sh