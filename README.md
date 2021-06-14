# CovidCertificate-App-Verifier-Service

[![License: MPL 2.0](https://img.shields.io/badge/License-MPL%202.0-brightgreen.svg)](https://github.com/admin-ch/CovidCertificate-App-Verifier-Service/blob/main/LICENSE)

This project is released by the the [Federal Office of Information Technology, Systems and Telecommunication FOITT](https://www.bit.admin.ch/)
on behalf of the [Federal Office of Public Health FOPH](https://www.bag.admin.ch/).
The app design, UX and implementation was done by [Ubique](https://www.ubique.ch?app=github).

## Services
This backend serves various parameters used for covid certificate verification to the app and syncs Digital Signature Certificates (DSC) used to sign covid certificates from and to the european hub.

### Webservice
Serves various parameters used for verifying the validaty of covid certificates. Currently this includes:

* DSCs
* revoked covid certificates
* value sets with test and vaccine parameters
* additional (national) verification rules


### Sync

TODO write a few words

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
