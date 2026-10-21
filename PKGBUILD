# Maintainer: gur0s <gur0s@chord.app>
pkgname=chord
pkgver=1.0.0
pkgrel=1
pkgdesc="A cross-platform music player with Audius, Jamendo, and MusicBrainz integration"
arch=('x86_64' 'aarch64')
url="https://github.com/gur0s/Chord"
license=('GPL-3.0-only')
depends=(
    'java-runtime>=21'
    'alsa-lib'
    'libx11'
    'libxext'
    'libxi'
    'libxrender'
    'libxtst'
    'freetype2'
    'fontconfig'
)
makedepends=(
    'java-environment>=21'
    'gradle'
    'git'
)
optdepends=(
    'pulseaudio: PulseAudio support'
    'pipewire: PipeWire support'
)
provides=('chord')
conflicts=('chord-git')
source=("Chord-$pkgver.tar.gz::https://github.com/gur0s/Chord/releases/download/v$pkgver/Chord-$pkgver.tar.gz")
sha256sums=('SKIP')

build() {
    cd "Chord-$pkgver"
    
    # Build with Gradle
    ./gradlew :composeApp:packageTarGz --no-daemon
}

package() {
    cd "Chord-$pkgver"
    
    # Find the built distribution
    local dist_dir=$(find composeApp/build/distributions -name "Chord-*.tar.gz" | head -1)
    if [[ -z "$dist_dir" ]]; then
        echo "Error: Distribution not found"
        return 1
    fi
    
    # Extract and install
    tar -xzf "$dist_dir" -C "$pkgdir"
    
    # Move to proper locations
    local extracted_dir=$(basename "$dist_dir" .tar.gz)
    
    # Install binary
    install -dm755 "$pkgdir/usr/bin"
    ln -sf "/opt/chord/bin/Chord" "$pkgdir/usr/bin/chord"
    
    # Install application files
    install -dm755 "$pkgdir/opt/chord"
    cp -r "$extracted_dir"/* "$pkgdir/opt/chord/"
    
    # Desktop entry
    install -dm755 "$pkgdir/usr/share/applications"
    cat > "$pkgdir/usr/share/applications/chord.desktop" << EOF
[Desktop Entry]
Name=Chord
Comment=A cross-platform music player with Audius, Jamendo, and MusicBrainz integration
Exec=chord
Icon=chord
Terminal=false
Type=Application
Categories=Audio;Music;Player;
StartupNotify=true
EOF
    
    # Icon (if available)
    if [[ -f "$extracted_dir/lib/app-icon.png" ]]; then
        install -dm755 "$pkgdir/usr/share/icons/hicolor/256x256/apps"
        cp "$extracted_dir/lib/app-icon.png" "$pkgdir/usr/share/icons/hicolor/256x256/apps/chord.png"
    fi
    
    # License
    install -dm755 "$pkgdir/usr/share/licenses/$pkgname"
    cp LICENSE "$pkgdir/usr/share/licenses/$pkgname/"
}

# vim:set ts=2 sw=2 et: