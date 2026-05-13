# MirrorLink Signaling Server

The signaling server coordinates pairing and WebRTC negotiation.

Responsibilities:

- Create rooms
- Let receivers join rooms
- Relay offer/answer/ICE messages
- Expire stale rooms
- Close rooms when sessions end

The server should not handle screen media.

## Development

```bash
npm install
npm run dev
```

Default URL:

```txt
ws://localhost:8787
```

Run the smoke test while the server is running:

```bash
npm run smoke
```
