# Running it on your phone

Three ways, easiest first. None of them needs a developer account, and Android
does not make you re-sign anything every seven days.

---

## 1. Install the APK that CI already built

Every push builds a debug APK and attaches it to the run. Nothing to install on
your computer.

1. Go to the repo's **Actions** tab and open the most recent **build** run.
2. Scroll to **Artifacts** at the bottom and download **tallinntastebuds-debug**.
   It arrives as a `.zip` — unzip it and you have `app-debug.apk`.
3. Get that file onto the phone: email it to yourself, drop it in Drive, or plug
   the phone in and copy it over.
4. Open it on the phone. Android will say the app came from an unknown source
   and offer a settings screen — allow *the app you are installing from* (Files,
   Chrome, Gmail, whichever) to install unknown apps, then go back and tap
   **Install**.

That is it. It is a debug build, so it is signed with the standard debug key and
Play Protect may show one more "unsafe app" warning. **Install anyway** is the
button.

To update later, download the newer artifact and install it over the top. Same
signing key, so it upgrades in place and your saved list survives.

---

## 2. Build it yourself, from Android Studio

1. Install [Android Studio](https://developer.android.com/studio). It brings the
   SDK and JDK with it.
2. **File → Open** and pick this folder. Let it sync — the first sync downloads
   the dependencies and takes a few minutes.
3. Turn on developer options on the phone: **Settings → About phone**, tap
   **Build number** seven times. Then **Settings → System → Developer options →
   USB debugging**.
4. Plug the phone in, accept the *Allow USB debugging* prompt, pick it from the
   device dropdown, and press **Run**.

---

## 3. Build it from the command line

You need JDK 17 and an Android SDK with API 35. If you have Android Studio you
have both.

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`adb` lives in `~/Android/Sdk/platform-tools/` on Linux, and
`~/Library/Android/sdk/platform-tools/` on macOS.

---

## Things worth knowing once it is on there

### The map may say API KEY REQUIRED

It will not, unless you added a CARTO key. Without one the app draws
OpenStreetMap's own tiles, which need no key and carry no watermark. See
[The map tiles](../README.md#the-map-tiles) in the README if you want the site's
exact basemap.

### It does not need the network

The app ships a snapshot of the website's content, so a first launch on a plane
still draws the whole map. The photos are the one thing that needs a connection,
and they are cached once seen.

### Adding a place does not need a new APK

Edit `data/restaurants.json` on the
[website repo](https://github.com/etibarhasanov/tallinntastebuds), push, and wait
for Cloudflare Pages. The next time the app comes to the foreground, the place is
there. Same for blurbs, photos, translations, discounts and the radio station.

### The radio keeps playing with the screen off

That is the one thing the website cannot do. It shows up in the notification
shade with a stop button, takes your headphone buttons, and ducks out of the way
of a navigation prompt. Stopping it in the shade stops it in the app.

### Where your saved list lives

On the phone, in the app's own preferences, and nowhere else. There is no
account to sign into. Uninstalling the app takes it with it; Android's own backup
carries it to a new phone.

---

## If something goes wrong

**"App not installed."** Usually an older copy signed with a different key.
Uninstall the old one and install again.

**The map is blank grey.** The tiles could not be fetched. Check the connection;
osmdroid caches what it has already drawn, so a map you have panned over before
will still be there.

**Nearest does nothing.** It needs a location fix. Check that location is on for
the phone, and that the app was granted the permission — **Settings → Apps →
Tallinn Tastebuds → Permissions**.

**No radio notification on Android 13 or later.** The notification permission was
refused. The radio still plays; grant it in the same permissions screen to get
the controls back.
