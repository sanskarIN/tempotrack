# Localization

TempoTrack ships English first and keeps shared user-facing copy in Compose Multiplatform resources so additional locales can be added without rewriting screens.

## Resource location

Default English strings live in:

`shared/src/commonMain/composeResources/values/strings.xml`

The generated resource package is configured in `shared/build.gradle.kts` as:

`in.sanskar.tempotrack.resources`

Shared Compose screens access strings through `Res.string.*` and `stringResource(...)`. Suspend/non-composable code that needs a localized UI message uses Compose Resources `getString(...)` at the UI boundary.

## Adding a language

Create a locale-specific resource directory beneath `shared/src/commonMain/composeResources`, following Compose Multiplatform resource qualifier conventions, and provide translated values using the same string names as the default file.

Do not translate:

- product or repository identifiers such as `TempoTrack`;
- email addresses;
- URLs;
- license identifiers such as `MIT`;
- values written to serialized data formats.

## Error boundaries

Domain and persistence layers should prefer typed errors or validation results. User-facing error sentences belong in resource files and should be selected by the shared UI. This keeps localization concerns out of storage/domain code and reduces the chance of exposing raw exception text or sensitive input.

## Formatting

Use resource placeholders for values that appear inside sentences. Time values are formatted by the domain formatter before being inserted into localized UI strings. Avoid building full sentences from multiple translated fragments where word order may differ between languages.

## Review checklist

When adding or changing UI copy:

1. Add or update the default resource value.
2. Reference the resource from the screen instead of hardcoding a visible sentence.
3. Preserve accessibility content descriptions in resources as well.
4. Check placeholder count and order.
5. Run the shared/Desktop/Android compile checks from `docs/testing.md`.
6. Review long translations on compact layouts and with large controls enabled.

Platform window titles, platform names, and system-owned text may remain platform-specific when they are not part of shared translatable product copy.
