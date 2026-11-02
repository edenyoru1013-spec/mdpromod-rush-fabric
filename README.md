# MDProMod Rush Duel Fabric

Fabric 1.20.1 port of MDProMod with a Rush Duel rules runtime, physical duel
field, deck editor, local duel host, WindBot integration, and multiplayer
seat routing.

## Repository Layout

| Path | Purpose |
|---|---|
| `work/mdpromod-rush-fabric/` | Fabric mod source, resources, tests, and Gradle build |
| `work/mdpro-repo/tools/duel_host/` | .NET duel host used by the mod |
| `work/mdpro-repo/tools/windbot_standalone/` | WindBot sources used by the host |
| `work/rush-invest/` | Rush deck fixtures and investigation tools |
| `work/probe_*.py`, `work/test_rush_host.py` | Headless protocol and duel tests |
| `PROJECT_STATUS.md` | Detailed project handoff, test results, and known issues |

## Build The Mod

Prerequisites:

- JDK 17 or newer
- Gradle 8.12.1 wrapper
- A D-drive Gradle cache is recommended:

```powershell
$env:GRADLE_USER_HOME='D:\gradle-home'
$env:TEMP="$env:GRADLE_USER_HOME\.tmp"
$env:TMP=$env:TEMP
.\gradlew.bat clean build
```

The development JAR is written to `build/libs/`.

## Build The Duel Host

```powershell
cd work\mdpro-repo\tools\duel_host
dotnet build -c Release
```

The host output is written to `bin\Release\net8.0\`.

## Notes

- GitHub stores source only. Packaged JARs, the reusable resource bundle,
  downloaded SDKs, backups, screenshots, and build caches are intentionally
  excluded.
- Full release packaging uses `work/mdpromod-rush-fabric/slim_bundle.py` and
  requires the external prebuilt bundle/base JAR used by the original local
  build pipeline.
- `PROJECT_STATUS.md` is the authoritative record of current behavior,
  validation results, and known limitations.
