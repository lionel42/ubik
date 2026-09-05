# Ubik

An Android news reader app.

Named after Philip K. Dick's novel *Ubik* — because as critic Lev Grossman wrote in *Time*, it is "a deeply unsettling existential horror story, a nightmare you'll never be sure you've woken up from" ([Wikipedia](https://en.wikipedia.org/wiki/Ubik)). Reading the news in this world feels about the same.


## Concept 

Ubik reads news feed from various news providers and brings them into one single news feed.
It opens the articles directly in an integrated webbrowser.

## Features

- Aggregated feed from multiple sources
- Article focus mode (hides site headers/navbars)
- Filters: unread only, hide sport, keyword blacklist, source toggles

## Logo generation

The project includes a helper script to generate Android-ready app assets from two SVG files in `app/src/main/res/original/`:

- `ubik_logo.svg`: used for the in-app menu/header logo
- `ubik_image.svg`: used for the launcher icon assets

Run:

	./scripts/generate_android_assets.sh


The script generates files required for the app.

## Provider configuration

Provider are defined in a JSON file.

- Bundled default file: `app/src/main/assets/providers.json`
- Optional runtime override file (device): `files/providers_override.json`

Loading precedence:

1. Runtime override file (if present and valid)
2. Bundled asset file

## License

GPLv3 — see [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html).
