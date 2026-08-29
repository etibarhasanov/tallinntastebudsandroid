# Publishing

Two places, one build. F-Droid is the shop; GitHub Releases is the link you can
send someone today, and the fallback for anyone who does not use F-Droid.

Neither costs anything and neither wants a card.

---

## Contents

- [The signing key](#the-signing-key)
- [Cutting a release](#cutting-a-release)
- [Getting onto F-Droid](#getting-onto-f-droid)
- [The store listing](#the-store-listing)
- [Screenshots](#screenshots)
- [If you ever want Google Play](#if-you-ever-want-google-play)

---

## The signing key

**Read this part twice.** Android identifies an app by its signing key, not by
its name. Lose the key and you can never update an installed app again — every
user has to uninstall and reinstall, losing their saved list. Leak it and anyone
can publish an update that Android will happily install over yours.

Make it once, on your own machine, and never in CI:

```bash
keytool -genkeypair -v \
  -keystore tallinntastebuds-release.jks \
  -alias tallinntastebuds \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -storetype PKCS12
```

It asks for a password and for a name and locality. The name is what shows in
the certificate; nobody but you will look at it.

Then, and this is the part people skip: **back the `.jks` file and its password
up somewhere that is not this laptop.** A password manager attachment, an
encrypted drive, anywhere with a copy. It cannot be regenerated.

### Building signed, locally

Create `keystore.properties` in the repository root — it is gitignored, along
with every `*.jks`:

```properties
storeFile=/absolute/path/to/tallinntastebuds-release.jks
storePassword=…
keyAlias=tallinntastebuds
keyPassword=…
```

Then `./gradlew assembleRelease`. Without that file the release build is
unsigned, which is deliberate: it is what F-Droid builds, and it must never fail
for want of a key it does not have.

### Building signed, in CI

Four repository secrets, under **Settings → Secrets and variables → Actions**:

| Secret | What |
| --- | --- |
| `TTB_KEYSTORE_BASE64` | `base64 -w0 tallinntastebuds-release.jks` |
| `TTB_STORE_PASSWORD` | the keystore password |
| `TTB_KEY_ALIAS` | `tallinntastebuds` |
| `TTB_KEY_PASSWORD` | the key password |

And optionally `TTB_TILE_KEY`, the free CARTO basemap key, so released builds
draw the website's own tiles. Without it the map draws OpenStreetMap's, which
needs no key.

On macOS `base64 -w0` is just `base64`.

---

## Cutting a release

1. Bump both numbers in `app/build.gradle.kts`. They are literals on purpose —
   F-Droid reads them straight out of the tagged tree:

   ```kotlin
   versionCode = 2
   versionName = "1.1"
   ```

2. Write the changelog for the new `versionCode`, in at least English:

   ```
   fastlane/metadata/android/en-US/changelogs/2.txt
   ```

3. Commit, tag, push:

   ```bash
   git tag v1.1
   git push origin main --tags
   ```

The `release` workflow then checks the tag agrees with `versionName`, validates
the bundled snapshot, runs the tests, builds and signs the APK, proves it is
signed with `apksigner`, and publishes a GitHub Release with the APK and its
SHA-256 attached.

A tag that disagrees with `versionName` fails before anything is built. That is
the point of the check: F-Droid would otherwise build one version and publish it
under another's name.

---

## Getting onto F-Droid

F-Droid builds the app itself, from this repository, at the tag — it does not
take your APK. So it signs with its own key, and an F-Droid install and a
GitHub Releases install are *different apps* to Android. Someone who has one
cannot update to the other without uninstalling. That is normal and not worth
fighting.

The app already meets the [inclusion
policy](https://f-droid.org/en/docs/Inclusion_Policy/): every dependency is
FOSS, and there is no Play Services, no Firebase, no analytics and no ad SDK.
That was a build-time decision, not luck — see the location note in the README.

**To submit:**

1. Cut a real release first, so there is a tag to build.

2. Fork <https://gitlab.com/fdroid/fdroiddata>.

3. Copy `fdroid/ee.tallinntastebuds.yml` from this repo to
   `metadata/ee.tallinntastebuds.yml` in your fork. It is kept here so the
   recipe is versioned beside the thing it builds.

4. Open a merge request. There is a review; expect questions and expect it to
   take a while — they read the source.

5. Test the recipe first if you like, with their build tool:

   ```bash
   fdroid build -v -l ee.tallinntastebuds
   ```

After the first release, `AutoUpdateMode: Version` and `UpdateCheckMode: Tags`
mean F-Droid picks up each new tag on its own. You do not submit again.

---

## The store listing

`fastlane/metadata/android/` holds the listing in nine languages. Do not edit it
by hand — it is generated:

```bash
node Tools/build-store-metadata.mjs
```

The text is not written for the store. The short description is the site's own
`tagline` from `ui.json`, and the body is the about screen's own words from
`AppStrings.kt` — so the listing says what the app says, in the voice it was
written in, rather than in a translation of a translation. Change either source
and re-run.

The release workflow fails if `fastlane/` is out of date, so this cannot drift.

---

## Screenshots

Not in the repository, because they have to come off a real screen. See
[`fastlane/metadata/android/en-US/images/README.md`](../fastlane/metadata/android/en-US/images/README.md)
for which five to take and the `adb` line that takes them.

The listing works without them. It just looks like nobody bothered.

---

## If you ever want Google Play

Your Play account predates 13 November 2023, so you are exempt from the
twelve-testers-for-fourteen-days closed test that new personal accounts have to
pass. You could go straight to production.

What it would need on top of what is here:

- an **AAB** rather than an APK — `./gradlew bundleRelease`, one more line in
  the release workflow
- **`PRIVACY.md` published at a URL.** Play requires a link, not a file. It
  would want to go on `tallinntastebuds.ee`
- the **Data safety** form — the honest answers are all "no data collected",
  which is the easiest version of that form there is
- Play's own listing assets: a 512×512 icon (already in `fastlane/`), a
  1024×500 feature graphic, and the screenshots above

Nothing in the app would have to change. There is no proprietary dependency to
add and no flavour to split, so the same source can serve both shops.
