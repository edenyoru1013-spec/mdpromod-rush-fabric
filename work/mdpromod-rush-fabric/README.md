# MDProMod Fabric

Fabric 1.20.1 port of MDProMod 0.1.0.

The original ocgcore, WindBot, .NET duel host and presentation assets are reused.
The Forge integration layer has been replaced with Fabric API registries,
networking, lifecycle callbacks and client rendering hooks.

Build with:

```powershell
.\gradlew.bat build
```

Target environment:

- Minecraft 1.20.1
- Fabric Loader 0.16.14
- Fabric API 0.92.11 or newer for 1.20.1
- Windows x64
