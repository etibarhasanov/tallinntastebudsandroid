/* Builds the F-Droid store listing from the app's own words.
 *
 * Nothing here is written for the store. The tagline comes from the website's
 * `ui.json` and the two paragraphs come from `AppStrings.kt`, which is where the
 * about screen reads them from — so the listing says, in each of the nine
 * languages, exactly what the app says about itself, in the voice it was
 * actually written in rather than in a translation of a translation.
 *
 * Re-run it after changing either source:
 *
 *   node Tools/build-store-metadata.mjs
 */
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const ui = JSON.parse(readFileSync(join(root, 'app/src/main/assets/seed/ui.json'), 'utf8'));
const appStrings = readFileSync(
  join(root, 'app/src/main/java/ee/tallinntastebuds/content/AppStrings.kt'), 'utf8');

/* F-Droid takes either form, but the regional codes are what a Play listing
   would want too, so the directory names stay useful if this ever goes there. */
const LOCALES = {
  en: 'en-US', et: 'et', ru: 'ru-RU', uk: 'uk', fi: 'fi-FI',
  az: 'az', pt: 'pt-PT', es: 'es-ES', tr: 'tr-TR',
};

/* AppStrings.kt is a Kotlin map of `"lang" to mapOf(Key.X to "…")`. Read rather
   than duplicated: a second copy of nine languages is a second copy to forget. */
function fromAppStrings(lang, key) {
  const block = appStrings.split(`"${lang}" to mapOf(`)[1];
  if (!block) throw new Error(`AppStrings.kt has no "${lang}" block`);
  const match = block.match(new RegExp(`Key\\.${key} to "((?:[^"\\\\]|\\\\.)*)"`));
  if (!match) throw new Error(`AppStrings.kt: "${lang}" has no ${key}`);
  return match[1].replace(/\\"/g, '"').replace(/\\\\/g, '\\');
}

/* Stores cap the short description at 80 characters. Spanish runs over, so it
   falls back to the first sentence of the about text rather than being cut off
   mid-word — still his sentence, just the shorter one. */
function shortDescription(tagline, about) {
  if (tagline.length <= 80) return tagline;
  const firstSentence = about.split(/(?<=\.)\s/)[0];
  if (firstSentence.length <= 80) return firstSentence;
  return firstSentence.slice(0, 77).replace(/\s+\S*$/, '') + '…';
}

let written = 0;
for (const [lang, locale] of Object.entries(LOCALES)) {
  if (!ui[lang]) throw new Error(`ui.json has no "${lang}"`);
  const tagline = ui[lang].tagline;
  const about = fromAppStrings(lang, 'ABOUT_BODY');
  const sync = fromAppStrings(lang, 'SYNC_NOTE');

  const dir = join(root, 'fastlane/metadata/android', locale);
  mkdirSync(join(dir, 'changelogs'), { recursive: true });

  /* One changelog per versionCode, named for it. The first release needs no
     notes beyond what it is, and every language gets the same sentence it
     already has for the about screen. */
  writeFileSync(join(dir, 'changelogs', '1.txt'), fromAppStrings(lang, 'ABOUT_BODY') + '\n');

  const short = shortDescription(tagline, about);
  if (short.length > 80) throw new Error(`${locale}: short description is ${short.length} characters`);

  writeFileSync(join(dir, 'title.txt'), ui[lang].wordmark + '\n');
  writeFileSync(join(dir, 'short_description.txt'), short + '\n');
  /* The tagline is the short description and says the same thing as the first
     line of the about text, so it is deliberately not repeated here. */
  writeFileSync(join(dir, 'full_description.txt'), `${about}\n\n${sync}\n`);
  written += 1;
}

console.log(`store listing written for ${written} languages`);
