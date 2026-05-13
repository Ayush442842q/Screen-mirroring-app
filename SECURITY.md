# Security Policy

MirrorLink handles screen content, connection metadata, and room pairing flows. Security reports should be handled carefully.

## Supported Versions

Security support will begin with the first public v1 release.

| Version | Supported |
| --- | --- |
| v1.x | Planned |
| pre-v1 | Best effort |

## Reporting a Vulnerability

Please do not open public GitHub issues for security vulnerabilities.

Until a dedicated security contact is published, please report privately to the repository owner through GitHub.

Useful details:

- Affected component
- Steps to reproduce
- Impact
- Suggested fix, if known

## Security Goals

- Room codes should be short-lived.
- Receivers should not see a stream unless paired intentionally.
- Signaling messages should be scoped to the correct room.
- Production deployments should use HTTPS/WSS.
- TURN support should be documented before internet-wide use.
