# Privacy

Tallinn Tastebuds collects nothing.

There is no account, no sign-in, no analytics, no advertising, no crash
reporting and no tracking of any kind. Nothing you do in the app is sent
anywhere, because there is nowhere for it to be sent: the app has no server of
its own.

## What leaves the phone

Four things, all of them because you asked for them:

| What | When | Where it goes |
| --- | --- | --- |
| A request for the map's content | On launch, and when the app returns to the foreground | `tallinntastebuds.ee`, served by Cloudflare Pages |
| A request for a photo | When a place with photos is on screen | The same host |
| A request for map tiles | While the map is on screen | OpenStreetMap's tile servers, or CARTO's if this build has a key |
| The radio stream | Only while the radio is playing | Whichever station `data/radio.json` names |

Each is an ordinary HTTP request, so the server at the other end sees what any
web server sees: an IP address, a time, and what was asked for. None of them
carries an identifier for you or for the app's installation.

## Location

The app asks for location permission the first time you press **Show my
location** or choose the **Nearest** sort, and never at launch.

It is used on the device and only on the device — to centre the map and to
order the list by distance. It is not stored, not logged, and not transmitted
anywhere, including to the website. The app asks for a single fix each time and
stops listening as soon as it has one, or when you leave the app.

Refusing the permission costs you those two features. Everything else works.

## What is kept on the phone

In the app's own private storage, readable by nothing else:

- your saved list, as a set of place ids
- your chosen language and colour style
- a cached copy of the website's content, so the app opens offline
- the tile and image caches

Uninstalling the app deletes all of it. Android's own backup may carry the first
two to a new phone, if you have that switched on.

## Leaving the app

Directions, phone numbers, the website, Instagram and TikTok links hand over to
another app or to your browser. Once you are there, that app's privacy policy
applies, not this one. The discount page opens on `tallinntastebuds.ee` in a
Custom Tab — your browser, your cookies.

Note that the **website** loads Google Analytics. The app does not, and the
Custom Tab is the one place where opening a page from the app can meet it.

## Children

The app is a restaurant map. It has no content directed at children and
collects no data from anybody, of any age.

## Changes

This file is versioned with the app. Its history is at
<https://github.com/etibarhasanov/tallinntastebudsandroid/commits/main/PRIVACY.md>.

## Asking

<https://www.instagram.com/tallinntastebuds/>
