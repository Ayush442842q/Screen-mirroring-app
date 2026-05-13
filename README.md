# MirrorLink

MirrorLink is an open-source screen mirroring project for sharing a screen from one device to another with low-latency WebRTC streaming.

The project starts with a practical v1 target:

- Laptop browser to mobile browser
- Laptop browser to tablet browser
- Laptop browser to laptop browser
- Android mobile app to mobile/tablet/laptop browser
- Pairing by room code or QR code
- Browser receiver experience on phones, tablets, laptops, and TV browsers

Future releases will add iPhone/iPad sender support, Android TV support, production TURN infrastructure, and stronger security controls.

## Repository Status

This repository is being built as a professional open-source project. The first milestone is the v1 foundation and prototype.

Current phase:

- [x] Open-source repository structure
- [x] Professional documentation baseline
- [x] v1 architecture plan
- [x] Detailed roadmap
- [ ] UI prototype
- [ ] WebRTC laptop-to-mobile implementation
- [ ] Android sender implementation

## Monorepo Layout

```txt
apps/
  web/          Browser sender and receiver app
  android/      Android sender app
  ios/          Future iPhone/iPad sender app
  tv-android/   Future Android TV app

packages/
  protocol/           Shared signaling message types
  signaling-client/   Shared WebSocket/WebRTC signaling client
  ui/                 Shared UI tokens and components

server/
  signaling/    WebSocket signaling server

docs/
  architecture-v1.md
  roadmap.md
  release-process.md
  ui-prototype.md
```

## Planned v1 Architecture

MirrorLink v1 uses WebRTC for media streaming and a lightweight WebSocket signaling server for room pairing.

High-level flow:

1. A sender creates a room.
2. The signaling server assigns a room code.
3. A receiver joins by room code or QR code.
4. Sender and receiver exchange WebRTC offer/answer/ICE messages through the signaling server.
5. Media flows peer-to-peer when possible.

See [docs/architecture-v1.md](docs/architecture-v1.md) for the complete v1 design.

## Roadmap

See [docs/roadmap.md](docs/roadmap.md).

## Releases

Public builds will be published through GitHub Releases.

Planned release artifacts:

- Web app build
- Signaling server source/package
- Docker image instructions
- Android APK for v1
- Changelog and compatibility notes

See [docs/release-process.md](docs/release-process.md).

## Contributing

Contributions are welcome once the first implementation lands. Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening issues or pull requests.

## Security

Please do not report security issues in public GitHub issues. See [SECURITY.md](SECURITY.md).

## License

MirrorLink is released under the MIT License. See [LICENSE](LICENSE).
