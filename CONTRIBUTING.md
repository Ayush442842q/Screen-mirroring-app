# Contributing to MirrorLink

Thank you for your interest in contributing. MirrorLink is intended to be a professional open-source project, so contributions should be clear, focused, and easy to review.

## Development Principles

- Keep changes small and reviewable.
- Prefer simple, documented architecture over clever shortcuts.
- Do not mix unrelated refactors with feature work.
- Add or update tests when behavior changes.
- Update documentation when the user-facing workflow or setup changes.

## Project Areas

- `apps/web`: Browser sender and receiver app
- `apps/android`: Android sender app
- `server/signaling`: WebSocket signaling server
- `packages/protocol`: Shared message schema and room protocol
- `packages/signaling-client`: Shared signaling client
- `docs`: Architecture, roadmap, release, and setup documentation

## Pull Request Checklist

Before opening a pull request:

- Run relevant tests and formatters.
- Confirm the app still starts locally.
- Update docs for new behavior.
- Add screenshots for UI changes when helpful.
- Link related issues.

## Commit Style

Use concise, descriptive commit messages.

Examples:

```txt
Add room code pairing protocol
Implement browser receiver stream view
Document Android MediaProjection flow
```

## Reporting Bugs

Use the bug report issue template and include:

- Device and operating system
- Browser/app version
- Sender and receiver devices
- Network conditions if relevant
- Steps to reproduce
- Expected and actual behavior
