# Translating Rivo

Thank you for helping translate Rivo into your language.

## Where translations live

- Source strings: [`app/src/main/res/values/strings.xml`](app/src/main/res/values/strings.xml)
- Translations: `app/src/main/res/values-<locale>/strings.xml`
  (for example `values-pl/strings.xml` for Polish, `values-de/strings.xml` for German)

Any string not yet translated for a locale automatically falls back to the
English source, so a partial translation is fine — it will never break the app.

## Preferred way: Crowdin

The project syncs translations through Crowdin. The
[`Crowdin Sync`](.github/workflows/crowdin.yaml) workflow uploads new source
strings and opens a pull request with completed translations, so in most cases
you do not edit XML by hand:

1. Join the project on Crowdin and pick your language.
2. Translate the strings in the Crowdin editor.
3. Completed translations are pulled back into the repository automatically and
   ship in the next preview build.

## Alternative: contribute a locale directly

If you would rather submit a translation as a pull request:

1. Copy `app/src/main/res/values/strings.xml` to
   `app/src/main/res/values-<locale>/strings.xml`.
2. Translate the text inside each `<string>` element. Leave the `name`
   attributes unchanged and keep any format placeholders (`%1$s`, `%d`, `\n`,
   `\'`) intact.
3. Do not translate strings marked `translatable="false"`.
4. Open a pull request.

New locales are picked up automatically — `generateLocaleConfig` is enabled, so
the language appears in the system per-app language picker without any extra
wiring.
