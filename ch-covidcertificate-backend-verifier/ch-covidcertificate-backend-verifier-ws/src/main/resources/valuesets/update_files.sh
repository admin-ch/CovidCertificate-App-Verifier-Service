#!/bin/bash

CURRENT_DIR="$(pwd)"
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd $SCRIPT_DIR

echo "downloading 'test-type.json'"
curl -s -o test-type.json https://raw.githubusercontent.com/ehn-dcc-development/ehn-dcc-schema/main/valuesets/test-type.json
echo "downloading 'test-manf.json'"
curl -s -o test-manf.json https://raw.githubusercontent.com/ehn-dcc-development/ehn-dcc-schema/main/valuesets/test-manf.json
echo "downloading 'vaccine-mah-manf.json'"
curl -s -o vaccine-mah-manf.json https://raw.githubusercontent.com/ehn-dcc-development/ehn-dcc-schema/main/valuesets/vaccine-mah-manf.json
echo "downloading 'vaccine-medicinal-product.json'"
curl -s -o vaccine-medicinal-product.json https://raw.githubusercontent.com/ehn-dcc-development/ehn-dcc-schema/main/valuesets/vaccine-medicinal-product.json
echo "downloading 'vaccine-prophylaxis.json'"
curl -s -o vaccine-prophylaxis.json https://raw.githubusercontent.com/ehn-dcc-development/ehn-dcc-schema/main/valuesets/vaccine-prophylaxis.json
echo "value set files updated"

cd $CURRENT_DIR