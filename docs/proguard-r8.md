# ProGuard and R8

Both SDK modules provide consumer rules. No host rules are currently required for their public APIs. Release builds should still verify measurement collection, Room persistence, export URIs, and WorkManager worker creation after shrinking.

The upload and Firestore modules also provide consumer rules. Hosts should verify their Firebase configuration and an upload flow in minified release builds.

The calls module declares its service, receivers, and FileProvider in its manifest. Verify foreground sampling and call uploads after shrinking.
