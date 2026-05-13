# Detailed Roadmap

MirrorLink will be developed in milestones that each produce something testable and releasable.

## MVP v1: Core Mirroring

Goal: prove reliable screen mirroring from laptop browser and Android mobile sender to browser receivers.

### Features

- Browser receiver page
- Laptop browser sender page
- Android sender app
- Pairing by room code
- Pairing by QR code
- WebRTC stream setup
- WebSocket signaling server
- Sender start/stop controls
- Receiver fullscreen control
- Basic reconnect and error states
- GitHub Release with Android APK

### Supported Paths

| Sender | Receiver | v1 Status |
| --- | --- | --- |
| Laptop browser | Mobile browser | Required |
| Laptop browser | Tablet browser | Required |
| Laptop browser | Laptop browser | Required |
| Android app | Mobile browser | Required |
| Android app | Tablet browser | Required |
| Android app | Laptop browser | Required |
| Laptop browser | TV browser | Best effort |

### Exit Criteria

- A user can open the web app on a laptop and mirror to a phone browser.
- A user can install the Android APK and mirror to another phone browser.
- Room code pairing works.
- QR pairing works.
- Docs explain how to run locally and install the APK.
- GitHub Release contains usable artifacts.

## MVP v2: Reliability and Daily Use

Goal: make v1 stable enough for repeated real use.

### Features

- Automatic reconnect
- Network quality indicator
- Manual quality selector
- FPS selector
- Audio support where platform allows
- Receiver device names
- Sender device names
- Recent rooms/devices
- PIN-protected rooms
- Better TV browser layout
- Improved accessibility

### Exit Criteria

- Common disconnects recover automatically.
- User can choose quality when bandwidth is limited.
- Receiver UI works comfortably on mobile, tablet, laptop, and TV browser sizes.

## MVP v3: iPhone and iPad Sender

Goal: add Apple mobile devices as senders.

### Features

- iOS sender app
- iPadOS sender support
- ReplayKit Broadcast Extension
- iOS sender setup guide
- TestFlight or release build documentation

### Exit Criteria

- iPhone can mirror to browser receiver.
- iPad can mirror to browser receiver.
- Setup steps are documented clearly.

## MVP v4: TV Support

Goal: make TV receiving and Android TV usage first-class.

### Features

- TV browser receiver mode
- Android TV receiver app
- Remote-friendly navigation
- Large room code display
- Always-on receiver lobby
- Fullscreen active stream mode
- Android TV sender feasibility investigation

### Exit Criteria

- TV can reliably act as a receiver.
- Android TV app is published as a release artifact.
- Remote navigation works without touch or mouse.

## MVP v5: Production and Self-Hosting

Goal: make MirrorLink realistic for public hosted use and polished self-hosting.

### Features

- TURN relay documentation
- Docker Compose setup
- Hosted signaling deployment guide
- Optional accounts
- Room permissions
- Stronger security controls
- Public demo deployment
- Release automation

### Exit Criteria

- Users can self-host the complete system.
- Releases include clear compatibility notes.
- Production deployment has documented security expectations.
