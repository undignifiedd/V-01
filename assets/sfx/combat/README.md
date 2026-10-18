# Combat sound candidates

Downloaded and integrated 18 October 2026. Main loads these once as shared libGDX Sound instances and wires them into both fights. Original audio is unchanged; filenames were shortened for the project.

| File | Intended event | Original file / creator |
| --- | --- | --- |
| `punch_hit_01.ogg` | Successful player punch, variant 1 | `impactPunch_medium_000.ogg` — Kenney |
| `punch_hit_02.ogg` | Successful player punch, variant 2 | `impactPunch_medium_001.ogg` — Kenney |
| `heavy_hit.ogg` | Player actually loses health in combat | `impactPunch_heavy_000.ogg` — Kenney |
| `shield_impact.ogg` | Falling shield landing | `impactMetal_heavy_000.ogg` — Kenney |
| `rage_launch.wav` | Rage wave / boss wave launch | `105016__julien-matthey__jm-fx-fireball-01.wav` — Julien Matthey; submitted to OpenGameArt by diligentcircle |
| `rage_explosion.ogg` | Rage wall explosion / bomb mob blast / opening screen-crack impact | `explosion1_0.ogg` — EZduzziteh |

## Sources and licenses

All six files are offered under **CC0 1.0**, which allows commercial use and modification without required attribution. Credits are still recommended.

- Kenney: [Impact Sounds](https://kenney.nl/assets/impact-sounds). Original included license is preserved as `kenney_license.txt`. [Kenney usage/attribution FAQ](https://kenney.nl/support).
- Julien Matthey: [Fireball on OpenGameArt](https://opengameart.org/content/fireball-1), explicitly marked CC0. [Original Freesound source](https://freesound.org/people/Julien%20Matthey/sounds/105016/). Download: <https://opengameart.org/sites/default/files/105016__julien-matthey__jm-fx-fireball-01.wav>.
- EZduzziteh: [Explosions on OpenGameArt](https://opengameart.org/content/explosions-4), explicitly marked CC0. Download: <https://opengameart.org/sites/default/files/explosion1_0.ogg>.
- [CC0 1.0 legal text](https://creativecommons.org/publicdomain/zero/1.0/legalcode).

Suggested credit: "Combat sound effects: Kenney, Julien Matthey and EZduzziteh (CC0)."

## Integration notes

Main passes the six shared resources to FightPhase via `setCombatSounds` and disposes them on shutdown. FightPhase stops active effects on scene exit/retry, preventing tails from spilling into the diary. Effects stay separate from streamed diary/NPC music.

Punch audio alternates the two variants on confirmed damage, at most once per swing, with slight pitch variation. Misses, protected enemies and invulnerable player hits do not play impact audio. Each projectile consumes one launch event; explosions and each shield landing also use one-time triggers. Bombs sound even when dodged, but killed bombs do not explode. Opening crack audio starts at impact, and shield audio starts at each landing, preserving the anticipation pauses.

Initial mix: punches 65%, player hurt 55%, player wave launch 60%, boss wave launch 35%, rage explosion 70%, bomb blast 45%, opening crack 80%, shield landings 75%. Music defaults to 50%; the menu Options/volume panel was removed at the author's request.

Licenses, filenames and decode compatibility have been checked. Mocked playback tests verify event counts and real Main-to-fight resource wiring. Final volume/pitch mixing still needs an audible playthrough; the mix has not been auditioned here.
