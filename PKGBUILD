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
    'git'
)
optdepends=(
    'pulseaudio: PulseAudio support'
    'pipewire: PipeWire support'
)
provides=('chord')
conflicts=('chord-git')
source=("Chord-1.0.0.tar.gz")
sha256sums=('SKIP')

build() {
    cd "$srcdir"
    
    # Build with Gradle
    ./gradlew :composeApp:packageTarGz --no-daemon
}

package() {
    cd "$srcdir"
    
    # Find the built distribution
    local dist_file=$(find composeApp/build/distributions -name "Chord-*.tar.gz" | head -1)
    if [[ -z "$dist_file" ]]; then
        echo "Error: Distribution not found"
        return 1
    fi
    
    # Extract and install
    tar -xzf "$dist_file" -C "$pkgdir"
    
    # Find extracted directory
    local extracted_dir=$(basename "$dist_file" .tar.gz)
    
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
    
    # License
    install -dm755 "$pkgdir/usr/share/licenses/$pkgname"
    cp LICENSE "$pkgdir/usr/share/licenses/$pkgname/"
}

# vim:set ts=2 sw=2 et: