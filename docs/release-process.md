# Release Process

MirrorLink releases should be predictable, documented, and easy for users to install.

## Versioning

Use semantic versioning after the first stable release:

```txt
MAJOR.MINOR.PATCH
```

Before v1, use alpha tags:

```txt
v0.1.0-alpha.1
v0.2.0-alpha.1
v1.0.0
```

## Release Artifacts

Each relevant release should include:

- Web app build notes
- Signaling server build notes
- Android APK
- Docker instructions when server packaging is available
- Changelog
- Compatibility table
- Known issues

## Release Checklist

- Update `CHANGELOG.md`.
- Confirm docs match the release behavior.
- Run web checks.
- Run server checks.
- Build Android APK when applicable.
- Smoke test laptop-to-mobile.
- Smoke test Android-to-mobile when applicable.
- Create a GitHub Release with artifacts.
- Include known limitations.

## Publishing APKs

APK files should be attached to GitHub Releases, not committed into the repository.

The release workflow runs on tags matching:

```txt
v*
```

Example:

```bash
git tag v0.1.0-alpha.1
git push origin v0.1.0-alpha.1
```

The workflow builds the Android debug APK and attaches it to the release. Later, this should be upgraded to signed release APKs/AABs once signing keys are configured.

## Compatibility Table Template

| Sender | Receiver | Status | Notes |
| --- | --- | --- | --- |
| Laptop Chrome | Android Chrome | Supported | v1 target |
| Laptop Chrome | iOS Safari | Planned test | Receiver only |
| Android app | Android Chrome | Supported | v1 target |
| Android app | Laptop Chrome | Supported | v1 target |
| Laptop Chrome | TV browser | Best effort | Depends on TV browser |
