# MirrorLink v1 Architecture

MirrorLink v1 is designed around a simple rule: receivers should work in the browser, while sender support uses the best available platform API.

## v1 Scope

Supported sender paths:

- Laptop browser to browser receiver
- Android app to browser receiver

Supported receiver paths:

- Mobile browser
- Tablet browser
- Laptop browser
- TV browser where WebRTC playback is supported

Out of scope for v1:

- iPhone/iPad sender app
- Native TV app
- Internet relay guarantees
- Accounts
- Recording
- Remote control
- Multi-receiver broadcast

## Components

### Web App

Location: `apps/web`

Responsibilities:

- Browser receiver experience
- Laptop browser sender experience
- Room creation and joining
- QR code pairing
- Screen capture with `getDisplayMedia` on supported desktop browsers
- WebRTC peer connection setup
- Stream playback

Initial implementation status: started.

### Android App

Location: `apps/android`

Responsibilities:

- Android screen capture with MediaProjection
- Sender room creation
- WebSocket signaling
- WebRTC streaming to browser receiver
- Start, stop, and connection state UI

### Signaling Server

Location: `server/signaling`

Responsibilities:

- Create short-lived rooms
- Join receivers to rooms
- Relay signaling messages between sender and receiver
- Track room lifecycle
- Expire stale rooms

The signaling server does not receive screen media. Media should flow over WebRTC peer connections.

Initial implementation status: started.

### Protocol Package

Location: `packages/protocol`

Responsibilities:

- Shared room message types
- Error codes
- Versioned protocol schema
- Compatibility helpers

### Signaling Client Package

Location: `packages/signaling-client`

Responsibilities:

- WebSocket connection lifecycle
- Reconnect behavior
- Typed message send/receive helpers
- Shared WebRTC signaling helpers

## Room Lifecycle

1. Sender requests a room.
2. Server creates a room with a short room code.
3. Sender displays room code and QR code.
4. Receiver joins the room.
5. Server notifies sender and receiver.
6. Sender creates a WebRTC offer.
7. Receiver replies with an answer.
8. Both sides exchange ICE candidates.
9. Media starts flowing through WebRTC.
10. Room closes when sender stops or both clients disconnect.

## Message Types

Initial protocol messages:

```txt
room:create
room:created
room:join
room:joined
peer:ready
webrtc:offer
webrtc:answer
webrtc:ice-candidate
room:error
room:closed
heartbeat
```

The final implementation should keep these messages typed in `packages/protocol`.

## Security Model

v1 is designed for trusted local or self-hosted use.

Required safeguards:

- HTTPS/WSS in deployed environments
- Short-lived room codes
- Room code entropy suitable for temporary pairing
- Server-side room expiration
- One active receiver per v1 room unless multi-receiver support is explicitly added later
- Clear sender stop-sharing control

Deferred safeguards:

- PIN-protected rooms
- End-to-end signaling validation
- Authenticated accounts
- Abuse controls for hosted public infrastructure

## WebRTC Notes

For local network testing, STUN may be enough. For reliable internet use, production deployments will need TURN.

v1 should expose configuration for:

- STUN servers
- TURN servers
- Signaling server URL

## Compatibility Strategy

Browser receiver should be the default compatibility layer. Native apps should be added only where the platform does not expose enough browser capability.

This means:

- Laptop sender can start in the browser.
- Android sender should be native because mobile browser screen capture is not reliable enough.
- iPhone/iPad sender should be native with ReplayKit in v3.
- TV receiver should start with browser support, then Android TV native support in v4.
