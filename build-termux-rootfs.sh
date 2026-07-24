#!/bin/bash
# Build Termux rootfs for embedding in APK
# Run this once to generate termux-rootfs.tar.gz in app/src/main/assets/

set -e

ASSETS_DIR="app/src/main/assets"
ROOTFS_FILE="$ASSETS_DIR/termux-rootfs.tar.gz"

echo "=== Building Termux rootfs ==="

# Option 1: Download prebuilt from Termux (fastest)
# Termux doesn't provide a direct rootfs tarball, so we use proot-distro or build manually

# Option 2: Use proot-distro to create minimal rootfs
# This creates a minimal Termux-like environment

mkdir -p "$ASSETS_DIR"

# Create a minimal bootstrap using termux-packages bootstrap
# This is a simplified version - in practice you'd use the official bootstrap

cat > /tmp/build_rootfs.sh << 'BUILD_EOF'
#!/bin/bash
set -e

# Create minimal Termux-like rootfs
ROOTFS_DIR="/tmp/termux-rootfs"
rm -rf "$ROOTFS_DIR"
mkdir -p "$ROOTFS_DIR"

# Essential directories
mkdir -p "$ROOTFS_DIR"/{bin,lib,usr/bin,usr/lib,home,tmp,etc,var,opt}

# Download and extract Termux bootstrap (aarch64)
BOOTSTRAP_URL="https://github.com/termux/termux-packages/releases/download/bootstrap-2024.01.12/bootstrap-aarch64.zip"
cd /tmp
wget -q "$BOOTSTRAP_URL" -O bootstrap.zip
unzip -q bootstrap.zip -d "$ROOTFS_DIR"
rm bootstrap.zip

# Install essential packages via pkg (if running in Termux) or manually add binaries
# For APK embedding, we include: python3, proot, git, openssh, nodejs, vim, nano

# Create minimal packages list
cat > "$ROOTFS_DIR/etc/apt/sources.list" << 'EOF'
deb https://packages.termux.org/apt/termux-main stable main
EOF

# Add proot binary (needed for proot environment)
# Download static proot
PROOT_URL="https://github.com/termux/proot/releases/download/v5.4.0/proot-aarch64"
wget -q "$PROOT_URL" -O "$ROOTFS_DIR/bin/proot"
chmod +x "$ROOTFS_DIR/bin/proot"

# Create tarball
cd "$ROOTFS_DIR"
tar -czf /tmp/termux-rootfs.tar.gz .
echo "Created: /tmp/termux-rootfs.tar.gz ($(du -h /tmp/termux-rootfs.tar.gz))"
BUILD_EOF

chmod +x /tmp/build_rootfs.sh

# Note: This script downloads ~50MB. Run manually or in CI.
echo "To build rootfs, run: /tmp/build_rootfs.sh"
echo "Then copy /tmp/termux-rootfs.tar.gz to $ROOTFS_FILE"
echo ""
echo "For CI, the workflow will run this automatically."