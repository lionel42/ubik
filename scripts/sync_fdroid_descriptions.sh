#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
README_PATH="${README_PATH:-$ROOT_DIR/README.md}"
SHORT_OUT="${SHORT_OUT:-$ROOT_DIR/fastlane/metadata/android/en-US/short_description.txt}"
FULL_OUT="${FULL_OUT:-$ROOT_DIR/fastlane/metadata/android/en-US/full_description.txt}"
SHORT_MAX_LEN="${SHORT_MAX_LEN:-80}"

usage() {
  cat <<EOF
Sync F-Droid/Fastlane description files from README.

Usage:
  ./scripts/sync_fdroid_descriptions.sh

Optional env overrides:
  README_PATH     Path to README source (default: $ROOT_DIR/README.md)
  SHORT_OUT       Output short description txt path
  FULL_OUT        Output full description txt path
  SHORT_MAX_LEN   Max characters for short description (default: 80)
EOF
}

log() {
  printf '[fdroid-desc] %s\n' "$*"
}

fail() {
  printf '[fdroid-desc] ERROR: %s\n' "$*" >&2
  exit 1
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

[[ -f "$README_PATH" ]] || fail "README not found: $README_PATH"

if ! [[ "$SHORT_MAX_LEN" =~ ^[0-9]+$ ]]; then
  fail "SHORT_MAX_LEN must be a positive integer"
fi

sanitize_markdown_inline() {
  sed -E \
    -e 's/`//g' \
    -e 's/\*\*([^*]+)\*\*/\1/g' \
    -e 's/\*([^*]+)\*/\1/g' \
    -e 's/\[([^]]+)\]\([^)]+\)/\1/g' \
    -e 's/[[:space:]]+/ /g' \
    -e 's/^ //; s/ $//'
}

extract_short_description() {
  awk '
    /^# / { seen_h1 = 1; next }
    seen_h1 && NF { print; exit }
  ' "$README_PATH" | sanitize_markdown_inline
}

extract_intro_blurb() {
  awk '
    /^# / { seen_h1 = 1; next }
    !seen_h1 { next }

    # Skip the short description line and surrounding blanks.
    !short_skipped {
      if (NF == 0) next
      short_skipped = 1
      next
    }

    # Stop once we hit the first level-2 heading.
    /^##[[:space:]]+/ { exit }

    {
      print
    }
  ' "$README_PATH" | sanitize_markdown_inline | trim_blank_edges
}

extract_section() {
  local heading="$1"
  awk -v wanted="$heading" '
    BEGIN {
      in_section = 0
    }
    $0 ~ "^##[[:space:]]*" wanted "[[:space:]]*$" {
      in_section = 1
      next
    }
    in_section && $0 ~ "^##[[:space:]]+" {
      exit
    }
    in_section {
      print
    }
  ' "$README_PATH"
}

trim_blank_edges() {
  awk '
    { lines[NR] = $0 }
    END {
      start = 1
      while (start <= NR && lines[start] ~ /^[[:space:]]*$/) start++
      end = NR
      while (end >= start && lines[end] ~ /^[[:space:]]*$/) end--
      for (i = start; i <= end; i++) print lines[i]
    }
  '
}

truncate_short_description() {
  local text="$1"
  local max_len="$2"

  if (( ${#text} <= max_len )); then
    printf '%s\n' "$text"
    return
  fi

  local cut
  cut="${text:0:max_len}"
  cut="${cut% }"
  printf '%s\n' "$cut"
}

main() {
  local short_desc
  short_desc="$(extract_short_description)"
  [[ -n "$short_desc" ]] || fail "Could not extract short description from README"
  short_desc="$(truncate_short_description "$short_desc" "$SHORT_MAX_LEN")"

  local concept_section
  concept_section="$(extract_section "Concept" | sanitize_markdown_inline | trim_blank_edges)"

  local intro_blurb
  intro_blurb="$(extract_intro_blurb)"

  local features_section
  features_section="$(extract_section "Features" | trim_blank_edges)"

  local features_formatted
  features_formatted="$(printf '%s\n' "$features_section" | \
    sed -E \
      -e 's/^-[[:space:]]+/ - /' \
      -e 's/\[([^]]+)\]\([^)]+\)/\1/g' \
      -e 's/`//g' \
      -e 's/\*\*([^*]+)\*\*/\1/g' \
      -e 's/\*([^*]+)\*/\1/g' \
      -e 's/[[:space:]]+$//' \
      -e '/^[[:space:]]*$/d')"

  mkdir -p "$(dirname "$SHORT_OUT")" "$(dirname "$FULL_OUT")"

  {
    printf '%s\n' "$short_desc"
  } > "$SHORT_OUT"

  {
    printf '%s\n\n' "$short_desc"
    if [[ -n "$intro_blurb" ]]; then
      printf '%s\n\n' "$intro_blurb"
    fi
    if [[ -n "$concept_section" ]]; then
      printf '%s\n\n' "$concept_section"
    fi
    if [[ -n "$features_formatted" ]]; then
      printf 'Features:\n'
      printf '%s\n' "$features_formatted"
      printf '\n'
    fi
  } > "$FULL_OUT"

  log "Updated: $SHORT_OUT"
  log "Updated: $FULL_OUT"
}

main
