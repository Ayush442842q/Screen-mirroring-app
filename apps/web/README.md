# MirrorLink Web App

The web app will provide:

- Browser receiver mode
- Laptop browser sender mode
- Room code pairing
- QR code pairing
- WebRTC stream playback

Stack: React + Vite + TypeScript.

## Development

```bash
npm install
npm run dev
```

The app defaults to `ws://<current-host>:8787` for signaling.

Laptop sender support uses `navigator.mediaDevices.getDisplayMedia`, which requires a secure context or localhost.
