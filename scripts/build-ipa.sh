#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
if ! command -v xcodebuild >/dev/null 2>&1; then
  echo "Ce script nécessite un Mac avec Xcode complet installé et sélectionné."
  exit 1
fi
xcodebuild -project Brumes.xcodeproj -scheme Brumes \
  -configuration Release -sdk iphoneos -destination 'generic/platform=iOS' \
  -derivedDataPath build CODE_SIGNING_ALLOWED=NO build
stage_dir=$(mktemp -d "${TMPDIR:-/tmp}/brumes-ipa.XXXXXX")
trap 'rm -rf "$stage_dir"' EXIT
mkdir -p "$stage_dir/Payload" dist
cp -R build/Build/Products/Release-iphoneos/Brumes.app "$stage_dir/Payload/"
output_path="$PWD/dist/Brumes-unsigned.ipa"
# Use a fresh ZIP so old app resources cannot remain in a previous archive.
(cd "$stage_dir" && /usr/bin/zip -qry Brumes-unsigned.ipa Payload)
cp "$stage_dir/Brumes-unsigned.ipa" "$output_path"
echo "IPA non signé créé : $output_path"
echo "Il reste à le signer avec un certificat et un profil compatibles avec ton iPhone."
