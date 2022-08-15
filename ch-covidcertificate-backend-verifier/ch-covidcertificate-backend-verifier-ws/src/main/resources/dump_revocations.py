import requests
import json
import io
import time 

isFinished = False
since = "0"
certs = []
validDuration = 0
    
while isFinished == False:
    try:
        response = requests.get(
            url="https://www.cc.bit.admin.ch/trust/v2/revocationList",
            params= {"since": since} if since != "0" else {} ,
            headers={
                "Accept": "application/json",
                "Authorization": "Bearer 0795dc8b-d8d0-4313-abf2-510b12d50939",
            },
        )
        jsonResponse = response.json()
        certs.extend(jsonResponse["revokedCerts"])
        validDuration = jsonResponse["validDuration"]
        since = response.headers["X-Next-Since"]
        isFinished = response.headers["Up-To-Date"] == "true"
    except requests.exceptions.RequestException:
        print('HTTP Request failed')

with open('revocations.csv', 'w') as file:
    file.write('\n'.join(certs))


data = {
    "validDuration": validDuration,
    "lastDownload": round(time.time() * 1000),
    "nextSince": since
}

with open('revocation_metadata.json', 'w') as f:
    json.dump(data, f)
