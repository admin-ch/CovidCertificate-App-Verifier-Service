# CovidCertificate-App-Verifier-Service

[![License: MPL 2.0](https://img.shields.io/badge/License-MPL%202.0-brightgreen.svg)](https://github.com/admin-ch/CovidCertificate-App-Verifier-Service/blob/main/LICENSE)

This project is released by the the [Federal Office of Information Technology, Systems and Telecommunication FOITT](https://www.bit.admin.ch/)
on behalf of the [Federal Office of Public Health FOPH](https://www.bag.admin.ch/).
The app design, UX and implementation was done by [Ubique](https://www.ubique.ch?app=github).

## Services
This service provides an API, which is consumed by the CovidCertificate-SDKs used by the COVID Certificate Apps of Switzerland. It publishes all the necessary data that is needed to verify a Digital Covid Certificate (DCC) in the client apps (offline). It also regularly syncs the DSCs with the EU-Gateway (`dgc-gateway`). The service itself does neither receive nor verify a DCC. 

### Webservice
Serves various data used for verifying the validaty of Digital Covid Certificates. Currently this includes:

* Public keys of Document Signer Certificates (DSCs)
* List of revoked UVCIs of Digital Covid Certificate (DCCs)
* National rules
* Value sets with test and vaccine mappings

## DGC Sync

The `ch-covidcertificate-backend-verifier-sync` module implements `DGCSync`, which updates the local database to match the list provided by the [DGC gateway](https://github.com/eu-digital-green-certificates/dgc-gateway). 

Every 30 minutes, an mTLS connection is set up and a GET request is sent to the gateway's `/trustList` endpoint, which responds with a list of CSCA and DSC certificates. Next, the certificates are validated: Expired certificates and DSCs without matching CSCA certificates are filtered out. Finally, the database is  updated to match the filtered list exactly.

## Usage
It is recommended to use the SDK (for iOS or Android) to verify the validity of Digital Covid Certificates. The SDK then interacts with this service. This service expects a bearer token to be passed in as `Authorization` header.

```
Authorization: Bearer <app-token>
```

## Contribution Guide

This project is truly open-source and we welcome any feedback on the code regarding both the implementation and security aspects.

Bugs or potential problems should be reported using Github issues.
We welcome all pull requests that improve the quality of the source code.
Please note that the app will be available with approved translations in English, German, French, Italian.

## Repositories

* Android App: [CovidCertificate-App-Android](https://github.com/admin-ch/CovidCertificate-App-Android)
* Android SDK: [CovidCertificate-SDK-Android](https://github.com/admin-ch/CovidCertificate-SDK-Android)
* iOS App: [CovidCertificate-App-iOS](https://github.com/admin-ch/CovidCertificate-App-iOS)
* iOS SDK: [CovidCertificate-SDK-iOS](https://github.com/admin-ch/CovidCertificate-SDK-iOS)
* Config Service: [CovidCertificate-App-Config-Service](https://github.com/admin-ch/CovidCertificate-App-Config-Service)
* Verifier Service: [CovidCertificate-App-Verifier-Service](https://github.com/admin-ch/CovidCertificate-App-Verifier-Service)

## License

This project is licensed under the terms of the MPL 2 license. See the [LICENSE](LICENSE) file for details.
