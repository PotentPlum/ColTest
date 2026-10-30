# Pokédex Tracker (v1.0)

An Android app for tracking your Pokémon TCG collection against the National Pokédex (Gen 1–3).
It looks like a Pokédex: red shell, blue lens, teal screens. Fill in each entry by logging the
cards you own.

| Generation Grid | Pokédex Detail | Add Card |
|---|---|---|
| ![Grid](docs/screenshots/1-generation-grid.png) | ![Detail](docs/screenshots/2-pokedex-detail.png) | ![Add card](docs/screenshots/3-add-card.png) |

## Tech stack

| Concern | Choice |
|---|---|
| UI | Jetpack Compose, Material 3 (custom Pokédex theme) |
| Storage | Room (local only, no account or server) |
| Networking | Retrofit + OkHttp + kotlinx.serialization |
| Images | Coil 3, 512 MB persistent disk cache that ignores server cache headers |
| DI | A small manual container (`AppContainer`) |
| Min / target SDK | 26 / 35 |

## Data sources (free, no API key)

| API | Used for | Caching |
|---|---|---|
| [PokeAPI](https://pokeapi.co) | Generation grid, Dex numbers, name, types, height, weight, flavor text, sprites | The species list for #1–386 is cached in Room on first launch. Each detail page is cached in Room the first time it's opened. After that the grid works offline. |
| [Pokémon TCG API](https://pokemontcg.io) | Card search and card art | Live queries, kept in memory for the session. Card art is kept in Coil's disk cache. |

Both APIs are called over HTTPS without any key. The TCG API's keyless tier is rate limited,
which is plenty for one person adding cards by hand. Transient 5xx responses are retried
automatically (`RetryInterceptor`).

### Strict rules engine

`domain/CardRules.kt` hard-filters every TCG result before it reaches the UI:

* Only cards with the **Pokémon** supertype are kept. **Trainer** and Energy cards are dropped.
* **TAG TEAM** cards are dropped.
* **Alolan** and **Galarian** forms are dropped. The check looks at both the name and the subtypes.

The rules are covered by `CardRulesTest`.

## Screen flow

1. **Generation Grid**: tabs for Gen 1, 2 and 3, and a "GEN 1 – x / 151 CAUGHT" counter.
   Pokémon you have no cards for are shown as gray sprites. Collected ones are in color with a
   yellow badge showing how many unique card variants you've logged.
2. **Pokédex Detail**: the top half shows PokeAPI data (name, Dex #, types, height, weight,
   flavor text). Below it is a scrolling gallery of the cards you own. Tap a card to zoom in or
   remove it. The yellow **ADD CARD** button sits bottom-right.
3. **Add Card (auto-query)**: opening it immediately searches for every valid card of that
   Pokémon, with the exclusion rules applied, and shows them in a grid.
   * **Filter by Set** dropdown (each set shows its year and card count)
   * **Sort by year** toggle (oldest/newest)
   * Free-text search over set, series, number, rarity and year
   * Tap a card to select it, then confirm to write it to Room. Long-press to zoom.
     Cards you already own are tagged **OWNED**.

The search queries by National Dex number (`nationalPokedexNumbers:6 supertype:pokemon`)
rather than by name. That way "Charizard ex", "Dark Charizard" and similar cards are included,
and names like *Farfetch'd*, *Mr. Mime* and *Nidoran♀* don't need escaping.

## Project layout

```
app/src/main/java/com/potentplum/dextracker/
├── data/
│   ├── local/        Room entities, DAOs, database
│   ├── remote/       Retrofit services + DTOs, OkHttp setup
│   ├── PokedexRepository.kt
│   └── CardRepository.kt
├── domain/           Generation ranges, CardRules engine, name formatting, TcgCard
└── ui/
    ├── components/   Pokédex chrome (shell, lens, screens, buttons, Poké Ball, cards)
    ├── grid/         Screen 1
    ├── detail/       Screen 2
    ├── addcard/      Add Card flow
    └── theme/
```

## Building

Requires JDK 17+ and the Android SDK (platform 35).

```bash
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # rules engine + formatting tests
```

Or open the project in Android Studio and run the `app` configuration.
