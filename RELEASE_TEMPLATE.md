# Chord Release Template

## Chord v{VERSION}

### What's New
- Brief summary of changes
- New features
- Bug fixes
- Known issues

### Downloads

| Platform | File | SHA256 | Size |
|----------|------|--------|------|
| Android (debug) | `app-debug.apk` | `SHA256_HASH` | `X MB` |
| Linux (Arch/CachyOS) | `Chord-{VERSION}-linux.tar.gz` | `SHA256_HASH` | `X MB` |
| Linux (Arch) | `chord-{VERSION}-1-x86_64.pkg.tar.zst` | `SHA256_HASH` | `X MB` |
| Windows | `Chord-{VERSION}-windows.zip` | `SHA256_HASH` | `X MB` |

### Installation

#### Android
```bash
adb install app-debug.apk
```
Or download and tap to install on device. **Debug build** - not signed for Play Store.

#### Linux (Arch / CachyOS)
```bash
# From PKGBUILD
git clone https://github.com/gur0s/Chord.git
cd Chord
makepkg -si

# Or from pre-built tarball
tar -xzf Chord-{VERSION}-linux.tar.gz
cd Chord-{VERSION}
./bin/Chord
```

#### Windows
Extract `Chord-{VERSION}-windows.zip` and run `Chord.exe`.

### Verification
```bash
# Verify checksums
sha256sum -c checksums.txt
```

### Changes Since {PREVIOUS_VERSION}

#### Added
- Feature A
- Feature B

#### Changed
- Change A
- Change B

#### Fixed
- Bug fix A
- Bug fix B

### Known Issues
- Issue 1: Description
- Issue 2: Description

### Legal
- Sources: Audius (streaming), Jamendo (CC), MusicBrainz (metadata)
- License: GPL-3.0-only
- No YouTube/Spotify/private APIs used

### Checksums
```
{SHA256_HASH}  app-debug.apk
{SHA256_HASH}  Chord-{VERSION}-linux.tar.gz
{SHA256_HASH}  chord-{VERSION}-1-x86_64.pkg.tar.zst
```