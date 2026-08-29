# Tallinn Tastebuds — Android app

The Android app for [Tallinn Tastebuds](https://tallinntastebuds.ee), built from
the same content the website reads, and a port of the
[iOS app](https://github.com/etibarhasanov/TallinnTasteBudsApp) screen for screen.

Kotlin and Jetpack Compose, Android 8.0 (API 26) and later.

---

## Contents

- [The short answer to "will my website edits show up in the app?"](#the-short-answer-to-will-my-website-edits-show-up-in-the-app)
- [Building it](#building-it)
- [The map tiles](#the-map-tiles)
- [Layout](#layout)
- [What is different from the iOS app, and why](#what-is-different-from-the-ios-app-and-why)
- [Permissions](#permissions)
- [Publishing](#publishing)
- [Keeping it honest](#keeping-it-honest)
- [Licence](#licence)

---

## The short answer to "will my website edits show up in the app?"

Yes. That is the whole design.

The website is a static site with no build step: everything it shows lives in
five JSON files under `data/` in the
[tallinntastebuds](https://github.com/etibarhasanov/tallinntastebuds) repo, and
the page reads them at load time. This app reads **exactly those same files over
the network**, from the same host.

```
   etibarhasanov/tallinntastebuds  (git push)
                 |
                 v
   Cloudflare Pages — tallinntastebuds.ee
                 |
        +--------+--------+--------+
        |                 |        |
   data/*.json       data/*.json   data/*.json
   photos/*          photos/*      photos/*
        |                 |        |
        v                 v        v
     website           iOS app   Android app
```

So: edit `data/restaurants.json`, push, wait for Cloudflare Pages to deploy, and
the new place is on the map in the app — on the next launch, or the next time
the app comes back to the foreground. Nothing is submitted to Google, nothing is
reviewed, no one has to update anything.

The site serves the `data` files with `Cache-Control: must-revalidate`, so the
app's conditional request gets the truth every time rather than a stale cached
copy.

### What updates by itself

| Change on the website | Shows up in the app |
| --- | --- |
| Add, edit or remove a place in `data/restaurants.json` | Yes, next refresh |
| Rewrite a blurb, add a translation | Yes, next refresh |
| Add or replace photos in `photos/<id>/` | Yes, next refresh |
| Add or retire a type in `data/taxonomy.json` | Yes — the filter chips follow |
| Any interface wording in `data/ui.json` | Yes — the app ships no strings of its own |
| **Add a whole new language** to `ui.json` | Yes — it appears in the language picker |
| Start or stop a discount in `data/deals.json` | Yes, next refresh |
| Change the radio station in `data/radio.json` | Yes, next refresh |
| Mark a place `"closed": true` | Yes — greyed out, with the site's closed note |

### What still needs a Play Store release

Anything that is a change to the *app* rather than to the *content*: new screens,
a different layout, a new gesture, the launcher icon, the tab bar. And the eight
words of the app's own furniture in `AppStrings.kt` — the tab names, the saved
list, the about screen — which have no equivalent on the site to be published
from.

Add a language to `ui.json` and the app will show it; the four tab labels will
fall back to English until `AppStrings.kt` catches up. The unit tests fail when
that happens, which is the point of them.

### Three layers, so it always draws something

`ContentClient` fetches through three layers, in this order:

1. **the network**, revalidated with the stored ETag, so an unchanged file costs
   a `304` and no body;
2. **the disk copy** of whatever was fetched last, in the app's files directory;
3. **the seed copy** in `app/src/main/assets/seed`, compiled into the APK.

Layers 2 and 3 mean the app opens instantly and offline. Layer 1 means an edit on
the website reaches the reader on the next refresh. A failure on one file leaves
the other four alone, and a snapshot is only written to disk once it has decoded
— a half-written file the app cannot read is worse than the older one it would
replace.

The seed is refreshed weekly by
[`.github/workflows/refresh-seed.yml`](.github/workflows/refresh-seed.yml), or by
hand:

```bash
./Tools/refresh-seed.sh
node Tools/validate-seed.mjs
```

---

## Building it

```bash
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`. Every push builds one in CI and
attaches it to the run, so you can install it on a phone without a toolchain —
see [docs/RUN-ON-YOUR-PHONE.md](docs/RUN-ON-YOUR-PHONE.md).

You need JDK 17 and an Android SDK with API 35. Android Studio brings both;
`./gradlew` brings its own Gradle.

Run the checks the same way CI does:

```bash
node Tools/validate-seed.mjs     # the bundled snapshot, before it ships
./gradlew testDebugUnitTest      # the decoders, against that same snapshot
```

---

## The map tiles

The website draws CARTO's Positron and Dark Matter through Leaflet. This app
draws the same two through osmdroid — but CARTO now wants a key, and it stamps
**API KEY REQUIRED** diagonally across every tile fetched without one.

The website's key is **deliberately not in this repo**. It is locked to the
site's domain in the CARTO dashboard, and a referrer lock means nothing to an
app: copying it here would ship something that half-works.

So the app has two honest states:

| | Light style (Red) | Dark style (Pink) |
| --- | --- | --- |
| **With a key** | CARTO Positron | CARTO Dark Matter |
| **Without a key** | OpenStreetMap standard | the same, inverted |

Without a key the map draws OpenStreetMap's own tiles, which need none and carry
no watermark. OSM publishes nothing dark, so the dark style inverts them —
serviceable, and better than plum cards over a pale basemap.

Get a free key at [carto.com/basemaps/apikey](https://carto.com/basemaps/apikey/)
— five million tiles a month, which a map of seventy restaurants will never come
close to — and put it in `local.properties`, which is not committed:

```properties
ttb.tileKey=your_key_here
```

`TTB_TILE_KEY` in the environment works too, which is how a release build would
pick it up in CI. Empty is a working state either way, on purpose: the map falls
back rather than failing.

Both states name their tiles on the map itself, because the licence asks to be
named where the map is and not in an about screen two taps away.

---

## Layout

```
app/src/main/java/ee/tallinntastebuds/
  TasteBudsApplication.kt    the five objects that outlive a screen
  MainActivity.kt            edge-to-edge, the style, the refresh on ON_START

  model/                     the website's five files, as types
    Place.kt                 one approved place; empty string == missing key
    Deal.kt                  a discount — never the secret behind its code
    Taxonomy.kt              type chips, whose language keys are open-ended
    RadioStation.kt          a default station plus per-language overrides

  content/
    ContentSource.kt         every URL the app knows, and the only place they live
    ContentClient.kt         network -> disk -> seed, with ETag revalidation
    ContentStore.kt          the single source of truth, as a ViewModel
    Strings.kt               interface text, read from the site's ui.json
    AppStrings.kt            the app's own furniture, in the site's nine languages

  service/
    Favourites.kt            the saved list — the one thing the app owns
    LocationProvider.kt      one fix when asked, never a running trace
    RadioPlayer.kt           the button's handle on the stream
    RadioService.kt          the media session that keeps it alive behind the lock

  ui/
    Theme.kt                 the two styles, on eight colour roles
    RootScreen.kt            four tabs and the one detail screen they all open
    MapScreen.kt             the map, which is the site's front page and the app's
    ListScreen.kt            search, chips, sort, every place in order
    PlaceDetailScreen.kt     one place, and the lightbox behind its photos
    SavedScreen.kt           the shortlist
    AboutScreen.kt           what the map is and where it comes from
    FilterChips.kt           "All" plus one chip per type in use, OR semantics
    PlaceRow.kt              one line in the list
    TileMap.kt               the basemap, and the pins drawn onto it
    RemoteImage.kt           a photo from photos/<id>/
    Actions.kt               directions, the dialler, a Custom Tab
    LocationRequest.kt       ask for the permission, then for the fix
```

---

## What is different from the iOS app, and why

Everything a reader sees is the same. These are the places where the platform
made the decision:

| | iOS | Android |
| --- | --- | --- |
| Map | MapKit — no key, no watermark | osmdroid over CARTO or OSM tiles ([above](#the-map-tiles)) |
| Radio | `AVPlayer` plus a background audio mode | a Media3 `MediaSessionService`, so it gets lock screen controls and audio focus too |
| Location | CoreLocation | the platform `LocationManager`, not Google's fused provider — that would be a Play Services dependency for a feature the app can live without, and would leave the map unable to find you on a phone with no Google on it |
| Web pages | `SFSafariViewController` | a Chrome Custom Tab |
| Directions | Apple Maps, walking | turn-by-turn if the phone has it, otherwise a dropped pin, otherwise a web map |
| Detail screen | a sheet | a full screen, with the back gesture closing it |
| Removing a saved place | swipe | the bookmark button on the row, which is the same button that put it there |
| Fonts | the system faces at the site's weights | the same, and the same serif for running text |

The two styles are the same eight colours to the value. Red is the site's own
light palette from `assets/styles.css`; the dark one is the app's pink, not the
site's green, and it is named Pink in the picker for that reason.

---

## Permissions

Three, and the app works without any of them:

- **Internet** — everything the app shows comes from the website.
- **Location**, coarse or fine — only to sort by distance and to centre the map.
  Asked for the first time the reader presses **Show my location** or picks
  **Nearest**, never at launch. One fix, then it stops; leaving the app stops it
  too.
- **Notifications**, on Android 13 and later — asked for the first time the radio
  is started, because a stream playing behind the lock screen has to be visible
  in the shade. Refuse it and the radio still plays.

There are no accounts, no analytics, and nothing is sent anywhere. The saved list
is a set of ids in the app's own preferences.

---

## Publishing

Two shops, one build, neither of which costs anything:

- **F-Droid**, which builds the app itself from a tag in this repository and
  signs it with its own key. The app meets the
  [inclusion policy](https://f-droid.org/en/docs/Inclusion_Policy/) as it
  stands — every dependency is FOSS, and there is no Play Services, no
  Firebase, no analytics and no ad SDK. The recipe to submit is
  [`fdroid/ee.tallinntastebuds.yml`](fdroid/ee.tallinntastebuds.yml).
- **GitHub Releases**, which is the link you can send someone today. Pushing a
  `v*` tag builds a signed APK, proves it is signed, and publishes it with its
  SHA-256.

Both are driven by the same tag:

```bash
git tag v1.1 && git push origin main --tags
```

The signing key, the four repository secrets, the release checklist and the
F-Droid submission are all in [docs/PUBLISHING.md](docs/PUBLISHING.md). Read
the part about the signing key before you make one — losing it means never
being able to update an installed app again.

The listing text in `fastlane/metadata/android/` is generated rather than
written, from the site's own `tagline` and the about screen's own words, in all
nine languages:

```bash
node Tools/build-store-metadata.mjs
```

The app collects nothing, and [PRIVACY.md](PRIVACY.md) says so at length.

---

## Keeping it honest

[`.github/workflows/android.yml`](.github/workflows/android.yml) runs on every
push:

1. `node Tools/validate-seed.mjs` — the bundled snapshot, checked against what
   the decoders require, before anything is built with it.
2. `./gradlew testDebugUnitTest` — the same snapshot read back through the real
   decoders. A change on the website the models cannot read fails here.
3. `./gradlew assembleDebug` — the APK, attached to the run.

The tests are deliberately pointed at `app/src/main/assets/seed` rather than at
fixtures of their own. Fixtures would only ever prove the app can read data the
app made up.

Pushing a `v*` tag runs [`release.yml`](.github/workflows/release.yml), which
adds two more checks before it will publish anything: that the tag agrees with
`versionName` in `app/build.gradle.kts`, and that `fastlane/` is what the
generator would produce. The first stops F-Droid building one version and
publishing it as another; the second stops the listing drifting away from the
words the app actually shows.

---

## Licence

The **code** is [MIT](LICENSE). Fork it, build your own, point it at your own
content.

The **content** is not — the write-ups, the photographs and the mark are
covered by [LICENSE-CONTENT](LICENSE-CONTENT). You may redistribute them
verbatim as part of a build of this app, which is what an app store does and
what F-Droid needs; lifting the reviews out to republish elsewhere needs a
conversation first. They are one person's opinions about where to eat, written
by hand and verified by going there. They are not a dataset.
