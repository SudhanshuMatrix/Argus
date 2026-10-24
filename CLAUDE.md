# Argus

Offline AI scam protector for parents' Android phones. Gemma runs on the phone, reads
incoming SMS and WhatsApp messages, and warns the parent loudly before they act.
The full plan is docs/PLAN.md. It is the source of truth: follow it, and if something
in it is wrong or impossible, stop and tell me instead of improvising.

## Stack
- Android only. Kotlin, Jetpack Compose, Material 3, coroutines and Flow, Koin.
- MVVM: each screen has a ViewModel exposing one immutable UiState as StateFlow.
- Single `app` module, packages by feature as in the plan's project structure.
- Min SDK 26, target the latest stable SDK. Gradle Kotlin DSL with a version catalog.
- Flavors: `dev` (no bundled model, loads the adb-pushed file), `standard`, `lite`.
  All share one applicationId: com.ajayinnovations.argus.

## Hard rules
- NEVER add the INTERNET permission or any networking library.
- Message text is never logged, saved to disk, put in Intent extras, or sent anywhere,
  in any build type. Log only levels, timings and counts.
- Everything in `core/` is pure Kotlin with zero Android imports.
- Never commit `*.litertlm` files.
- For LiteRT-LM and any library you are unsure about, read the real API of the
  installed version (sources, docs, or decompiled classes) before writing code.
  Never guess class or method names. Pin every dependency version.
- Ask me before adding a dependency the plan doesn't mention.
- Build only what the current prompt asks. No "if time permits" items unless I ask.

## Code quality
- No `!!`. Handle every failure path: no model, not enough space, permission revoked,
  model returns garbage. The app must never crash in the background.
- Small focused classes, constructor injection, no business logic in Composables.
- Unit tests for everything in `core/`.

## UI for older users
- Body text at least 18sp, headlines 28sp+, buttons at least 56dp tall and full width.
- High contrast, one main action per screen, plain words, no jargon or icons-only buttons.
- Support English and Hindi through string resources (values/ and values-hi/),
  switched from the in-app language setting with AppCompatDelegate.setApplicationLocales.

## Definition of done for every prompt
1. `./gradlew assembleDevDebug` succeeds and `./gradlew testDevDebugUnitTest` passes.
2. Update docs/PLAN.md if anything was built differently from the plan, and say why.
3. Commit with a clear message.
4. Stop and give me a short checklist of exactly what to test on my phone.