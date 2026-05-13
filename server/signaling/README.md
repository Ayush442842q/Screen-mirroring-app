# MirrorLink Signaling Server

The signaling server coordinates pairing and WebRTC negotiation.

Responsibilities:

- Create rooms
- Let receivers join rooms
- Relay offer/answer/ICE messages
- Expire stale rooms
- Close rooms when sessions end

The server should not handle screen media.
