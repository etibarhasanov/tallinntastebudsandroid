# Listing images

`icon.png` is the 512×512 store icon, cut from the same watercolour as the
launcher icon.

**Screenshots are missing on purpose.** They belong in a `phoneScreenshots/`
folder beside this file, named `1.png`, `2.png` and so on, and they have to be
taken on a real device or an emulator — there is no honest way to draw them by
hand, and a mock-up of a screen the app does not draw would be a lie in a shop
window.

Take five, in this order, which is the order someone reading the listing wants
to be told the story in:

1. The map, red style, pins across the old town.
2. A place open — photos, the write-up, the Must order list.
3. The list, with the filter chips visible.
4. The map again in the pink style, so both are on show.
5. A place with a discount on it, if one is live.

With the app running on a device:

```bash
adb exec-out screencap -p > 1.png
```

Then drop them in `phoneScreenshots/` and commit. F-Droid picks them up from
this folder on the next build.
