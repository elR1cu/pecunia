# ADR-0025: ngx-translate for Frontend Internationalization

## Status

Accepted

## Context

Pecunia targets Senior Backend Java positions at Swiss private banks and
financial services companies. Switzerland has four language regions, and
the three national languages relevant to a finance application
(French, German, Italian) plus English are the expected coverage for a
credible Swiss-facing product. As a CV piece, a working in-app language
switcher across **English, French, German, and Italian** is a feature that
speaks directly to that audience.

Until now the Angular frontend hardcodes French strings directly in
templates (e.g. `dashboard.html`). Internationalization is a cross-cutting
concern that is cheap to design in early and expensive to retrofit: once
hardcoded strings spread across many templates, extracting them later means
revisiting each one. No prior decision recorded an i18n strategy, so the
choice is open.

Three forces shape the technology choice:

1. **The application is entirely behind authentication.** There are no
   public marketing pages and no SEO requirement. The principal advantage
   of build-time localization — serving a fully pre-rendered, per-locale
   bundle for initial-load performance and search indexing — does not apply
   here.
2. **The frontend is zoneless and signals-first** (see the Angular 22
   scaffolding). State is expressed with Signals (`toSignal`, signal
   fields). An i18n solution that integrates natively with Signals aligns
   with the existing architecture.
3. **Runtime language switching matters.** The single real user may switch
   language on the fly, and a one-click FR↔DE switch is the demonstrable
   moment for recruiters. A solution requiring a full page reload or a
   separate bundle/URL per locale works against this.

Scope of what i18n covers in Pecunia: the **application chrome** only —
labels, buttons, messages, and locale-sensitive formatting (dates, money).
It does **not** cover user data: bank transaction descriptions arrive in
whatever language the bank emits, and category names are user-authored.
Two i18n surfaces remain outside this decision: Keycloak's login/logout
screens have their own localization (Keycloak themes), independent of the
Angular layer.

## Decision

Adopt **ngx-translate v18** (`@ngx-translate/core`) as the
internationalization library for the Angular frontend.

- **Runtime, JSON-based translations.** Translation keys map to per-language
  JSON files loaded at runtime, enabling language switching without a page
  reload and lazy loading of translation resources.
- **Signal-native integration.** ngx-translate v18 is rebuilt on Angular
  Signals (peer dependency Angular 18+): `currentLang` / `fallbackLang` are
  `Signal`s, translations are signal-backed, and the standalone
  `translate()` function returns a `Signal<Translation>`. This matches the
  zoneless, signals-first design rather than fighting it.
- **Languages: English, French, German, Italian.** English is the
  fallback language (consistent with the project convention that code and
  keys are English).
- **Adopt the mechanism now.** From this point on, user-facing strings go
  through translation keys rather than being hardcoded. Existing hardcoded
  strings (e.g. in `dashboard.html`) are migrated to keys. Full translation
  coverage of all four languages is a continuous effort: French and English
  are populated first (the primary user and the fallback); German and
  Italian are completed incrementally. The decision here is the
  **plumbing**, not a one-shot full translation deliverable.

## Consequences

### Positive

- Runtime language switching across the four languages, with no reload — the
  intended demo and daily-use experience.
- Native Signals integration keeps i18n consistent with the existing
  zoneless/signals architecture.
- No more hardcoded UI strings: a single source of truth per language,
  edited without touching component code.
- Lazy loading of translation files keeps the initial bundle lean.
- ngx-translate is the most widely adopted Angular runtime i18n library,
  with a large body of examples and tooling integrations (translation
  management platforms read its JSON format).

### Negative

- A third-party dependency rather than the official `@angular/localize`.
- **Maintenance-continuity risk.** ngx-translate's current maintenance is a
  recent revival (v18, mid-2026) after a period of uncertainty. This is the
  principal risk accepted here. Mitigation: the library is wrapped behind a
  thin application boundary (a translation service / pipe usage), the JSON
  key format is portable, and migration to Transloco — the closest
  equivalent with a longer continuous track record — would be mechanical if
  the revival stalls. This trigger is added to the periodic technology
  review.
- Small runtime translation overhead versus build-time `$localize`
  (negligible at this scale, and offset by the absence of per-locale
  bundles).

### Neutral

- Locale-sensitive formatting (dates, `BigDecimal` money rendering) is a
  related but separate concern, handled by Angular's own `LOCALE_ID` /
  `formatDate` / `formatNumber` machinery, coordinated with the active
  ngx-translate language.
- Keycloak's authentication screens are localized separately via Keycloak
  themes; coordinating their language with the app's is deferred.
- The roadmap and `CLAUDE.md` scope sections must record i18n as in-scope
  (mechanism) — previously it was listed in neither in-scope nor
  out-of-scope.

## Alternatives Considered

### Angular built-in i18n (`@angular/localize`)

The official, Angular-team-maintained solution. Build-time: it produces one
bundle per locale, served per request. As of 2026 it still does **not**
support runtime language switching without a full page reload — `$localize`
messages are translated once when first encountered, and loading
translations later does not re-translate already-rendered text (Angular
issues #38953 and #56318 remain open). Its main advantages — zero runtime
overhead and pre-rendered per-locale output for SEO and initial-load — are
moot for an application that is entirely behind authentication with no
public pages. Rejected because the runtime switch is a core requirement and
its principal advantage does not apply to this app.

### Transloco (`@jsverse/transloco`)

A modern, actively maintained runtime i18n library with runtime switching,
lazy-loaded translation files, SSR support, and a well-regarded developer
experience. Functionally a close match to the requirement. Rejected in
favor of ngx-translate v18 primarily for the latter's native Signals
rebuild (tighter fit with the zoneless/signals architecture) and its larger
adoption and ecosystem. Transloco remains the designated fallback should
ngx-translate's maintenance revival stall (see Negative consequences).

### Defer i18n entirely to post-MVP

Keep hardcoding strings and add i18n later. Rejected because of the
retrofit cost: hardcoded strings spreading across templates make a later
migration a per-template sweep. Adopting only the mechanism now, while
populating translations incrementally, captures most of the benefit at a
fraction of the future cost.

## References

- [Angular issue #38953 — improve built-in i18n runtime translation support](https://github.com/angular/angular/issues/38953)
- [Angular issue #56318 — localize support dynamic/runtime translations](https://github.com/angular/angular/issues/56318)
- [ngx-translate v18 release (Signals rebuild) — issue #1618](https://github.com/ngx-translate/core/issues/1618)
- [ngx-translate documentation](https://ngx-translate.org/)
- [Transloco i18n guide — Lokalise](https://lokalise.com/blog/angular-localization-with-transloco/)
- [Best Angular i18n libraries 2026 — Intlayer](https://intlayer.org/blog/i18n-technologies/frameworks/angular)
