#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RES_DIR="$ROOT_DIR/app/src/main/res"
PLAYSTORE_ICON_PNG="$ROOT_DIR/app/src/main/ic_launcher-playstore.png"
DESIGN_ASSETS_DIR="$ROOT_DIR/design-assets"

MENU_SVG_DEFAULT="$DESIGN_ASSETS_DIR/ubik_logo.svg"
ICON_SVG_DEFAULT="$DESIGN_ASSETS_DIR/ubik_image.svg"

MENU_SVG="$MENU_SVG_DEFAULT"
ICON_SVG="$ICON_SVG_DEFAULT"
ICON_PAD_PERCENT="${ICON_PAD_PERCENT:-10}"
MONO_ICON_PAD_PERCENT="${MONO_ICON_PAD_PERCENT:-33}"
PLAYSTORE_ICON_PAD_PERCENT="${PLAYSTORE_ICON_PAD_PERCENT:-16}"

usage() {
  cat <<EOF
Generate Android image assets from two SVG files.

Usage:
  ./scripts/generate_android_assets.sh [--menu-svg PATH] [--icon-svg PATH]

Defaults:
  --menu-svg $MENU_SVG_DEFAULT
  --icon-svg $ICON_SVG_DEFAULT

Outputs:
  - $RES_DIR/drawable-nodpi/app_menu_logo.png
  - $RES_DIR/drawable-nodpi/ic_launcher_logo.png
  - $RES_DIR/drawable-nodpi/ic_launcher_round_logo.png
  - $ROOT_DIR/app/src/main/ic_launcher-playstore.png
  - $RES_DIR/mipmap-*/ic_launcher.(webp|png)
  - $RES_DIR/mipmap-*/ic_launcher_round.(webp|png)

Notes:
  - For mipmaps, script prefers WebP if converter is available.
  - Square composed icons use ICON_PAD_PERCENT (default: 36). Round and monochrome composed icons use MONO_ICON_PAD_PERCENT (default: 31).
  - If WebP conversion tool is missing, it falls back to PNG.
EOF
}

log() {
  printf '[asset-gen] %s\n' "$*"
}

fail() {
  printf '[asset-gen] ERROR: %s\n' "$*" >&2
  exit 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --menu-svg)
      MENU_SVG="${2:-}"
      shift 2
      ;;
    --icon-svg)
      ICON_SVG="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      fail "Unknown argument: $1"
      ;;
  esac
done

[[ -f "$MENU_SVG" ]] || fail "Menu SVG not found: $MENU_SVG"
[[ -f "$ICON_SVG" ]] || fail "Icon SVG not found: $ICON_SVG"

if ! [[ "$ICON_PAD_PERCENT" =~ ^[0-9]+$ ]]; then
  fail "ICON_PAD_PERCENT must be an integer between 0 and 49"
fi

if (( ICON_PAD_PERCENT < 0 || ICON_PAD_PERCENT > 49 )); then
  fail "ICON_PAD_PERCENT must be between 0 and 49"
fi

has_cmd() {
  command -v "$1" >/dev/null 2>&1
}

render_svg_to_png() {
  local input="$1"
  local output="$2"
  local width="$3"
  local height="${4:-}"

  mkdir -p "$(dirname "$output")"

  if has_cmd inkscape; then
    if [[ -n "$height" ]]; then
      inkscape "$input" --export-type=png --export-filename="$output" --export-width="$width" --export-height="$height" >/dev/null
    else
      inkscape "$input" --export-type=png --export-filename="$output" --export-width="$width" >/dev/null
    fi
    return 0
  fi

  if has_cmd rsvg-convert; then
    if [[ -n "$height" ]]; then
      rsvg-convert -w "$width" -h "$height" "$input" -o "$output"
    else
      rsvg-convert -w "$width" "$input" -o "$output"
    fi
    return 0
  fi

  if has_cmd magick; then
    if [[ -n "$height" ]]; then
      magick -background none "$input" -resize "${width}x${height}" "$output"
    else
      magick -background none "$input" -resize "${width}" "$output"
    fi
    return 0
  fi

  fail "No SVG renderer found. Install one of: inkscape, librsvg (rsvg-convert), or ImageMagick (magick)."
}

png_to_webp() {
  local input="$1"
  local output="$2"

  if has_cmd cwebp; then
    cwebp -quiet -q 95 "$input" -o "$output"
    return 0
  fi

  if has_cmd magick; then
    magick "$input" -quality 95 "$output"
    return 0
  fi

  if has_cmd ffmpeg; then
    ffmpeg -v error -y -i "$input" "$output"
    return 0
  fi

  return 1
}

cleanup_icon_variants() {
  local dir="$1"
  local base="$2"

  rm -f "$dir/${base}.png" "$dir/${base}.webp" "$dir/${base}.jpg" "$dir/${base}.jpeg"
}

generate_composed_icon_png() {
  local input_svg="$1"
  local output_png="$2"
  local size="$3"
  local pad_percent="${4:-$ICON_PAD_PERCENT}"
  local background_markup="${5:-}"

  local input_abs
  input_abs="$(cd "$(dirname "$input_svg")" && pwd)/$(basename "$input_svg")"

  local pad=$(( size * pad_percent / 100 ))
  local inner=$(( size - (2 * pad) ))

  local composed_svg="$TMP_DIR/composed_icon_${size}.svg"

  cat > "$composed_svg" <<EOF
<svg xmlns="http://www.w3.org/2000/svg" width="$size" height="$size" viewBox="0 0 $size $size">
${background_markup}
  <image href="file://$input_abs" x="$pad" y="$pad" width="$inner" height="$inner" preserveAspectRatio="xMidYMid meet"/>
</svg>
EOF

  render_svg_to_png "$composed_svg" "$output_png" "$size" "$size"
}

generate_round_icon_png() {
  local input_svg="$1"
  local output_png="$2"
  local size="$3"

  local center=$(( size / 2 ))
  local radius=$(( size * 48 / 100 ))
  local background_markup="  <circle cx=\"$center\" cy=\"$center\" r=\"$radius\" fill=\"#1D4ED8\"/>"

  generate_composed_icon_png "$input_svg" "$output_png" "$size" "$MONO_ICON_PAD_PERCENT" "$background_markup"
}

generate_square_icon_png() {
  local input_svg="$1"
  local output_png="$2"
  local size="$3"
  local pad_percent="${4:-$ICON_PAD_PERCENT}"

  local background_markup="  <rect x=\"0\" y=\"0\" width=\"$size\" height=\"$size\" fill=\"#1D4ED8\"/>"

  generate_composed_icon_png "$input_svg" "$output_png" "$size" "$pad_percent" "$background_markup"
}

generate_monochrome_icon_png() {
  local input_svg="$1"
  local output_png="$2"
  local size="$3"
  local pad_percent="${4:-$MONO_ICON_PAD_PERCENT}"

  generate_composed_icon_png "$input_svg" "$output_png" "$size" "$pad_percent"
}

TMP_DIR="$ROOT_DIR/.asset-gen-tmp"
rm -rf "$TMP_DIR"
mkdir -p "$TMP_DIR"
trap 'rm -rf "$TMP_DIR"' EXIT

log "Generating menu logo"
render_svg_to_png "$MENU_SVG" "$RES_DIR/drawable-nodpi/app_menu_logo.png" 640

log "Generating adaptive launcher foreground bitmap"
generate_square_icon_png "$ICON_SVG" "$RES_DIR/drawable-nodpi/ic_launcher_logo.png" 432

log "Generating adaptive round launcher foreground bitmap"
generate_round_icon_png "$ICON_SVG" "$RES_DIR/drawable-nodpi/ic_launcher_round_logo.png" 432

log "Generating monochrome launcher bitmap (no background)"
generate_monochrome_icon_png "$ICON_SVG" "$RES_DIR/drawable-nodpi/ic_launcher_mono_logo.png" 432

log "Generating Play Store launcher image"
generate_square_icon_png "$ICON_SVG" "$PLAYSTORE_ICON_PNG" 512 "$PLAYSTORE_ICON_PAD_PERCENT"

declare -a DENSITIES=(
  "mdpi:48"
  "hdpi:72"
  "xhdpi:96"
  "xxhdpi:144"
  "xxxhdpi:192"
)

for entry in "${DENSITIES[@]}"; do
  density="${entry%%:*}"
  size="${entry##*:}"
  target_dir="$RES_DIR/mipmap-$density"

  mkdir -p "$target_dir"
  cleanup_icon_variants "$target_dir" "ic_launcher"
  cleanup_icon_variants "$target_dir" "ic_launcher_round"

  square_png="$TMP_DIR/ic_launcher_${density}.png"
  round_png="$TMP_DIR/ic_launcher_round_${density}.png"
  generate_square_icon_png "$ICON_SVG" "$square_png" "$size"
  generate_round_icon_png "$ICON_SVG" "$round_png" "$size"

  if png_to_webp "$square_png" "$target_dir/ic_launcher.webp"; then
    if ! png_to_webp "$round_png" "$target_dir/ic_launcher_round.webp"; then
      cp "$square_png" "$target_dir/ic_launcher_round.png"
      log "mipmap-$density round -> PNG fallback"
    fi
    log "mipmap-$density -> WebP"
  else
    cp "$square_png" "$target_dir/ic_launcher.png"
    cp "$round_png" "$target_dir/ic_launcher_round.png"
    log "mipmap-$density -> PNG (no WebP converter found)"
  fi
done

log "Done. Rebuild with: ./gradlew :app:assembleDebug"
