# Security Policy

## Supported versions

The latest tagged release and the default branch receive security fixes.

## Reporting a vulnerability

Please do **not** open a public issue for an undisclosed vulnerability.

Send a concise report to:

- **supportramsandesh@gmail.com**
- **sanskarin@outlook.in**

Include the affected version, reproduction conditions, impact, and a safe proof of concept when relevant. Do not include real user data or credentials.

## Security model

TempoTrack is offline-first and has no backend service. Primary security boundaries are:

- application-private local files;
- exported files explicitly created by the user;
- dependency/build supply chain;
- Android backup behavior.

The repository does not contain signing keys, API keys, tokens, or production secrets.
