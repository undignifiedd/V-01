# Mr. Monster: Game Jam Brief and Handoff

Updated: 18 October 2026. Read this first in a fresh chat, then inspect the current source before editing. This file distinguishes implemented behavior from agreed but unfinished features. Current user instructions override older design decisions.

## 0. Working agreement — read before doing anything

- The developer chooses which classes/files to implement and reviews all code.
- Follow this brief unless the developer explicitly changes it. Ask when something is ambiguous.
- Discuss changes to existing code before editing, especially changes to architecture or public contracts. Do not silently change locked signatures.
- Keep code simple. Avoid duplicate code, duplicate logic, unnecessary classes, abstractions, frameworks, or dependencies.
- Only implement the requested scope. Do not advance to the next class or feature without instruction.
- Never push without asking first. Leave local edits unstaged; only stage when the developer explicitly asks while preparing to push. Do not automatically commit, stage new assets, or change tracking rules.
- Stash/pull/pop only when requested. Inspect the stash contents rather than assuming the newest stash contains all work. The boss implementation was previously in an older stash while the newest stash only held assets.
- Keep responses concise. After code changes, give 2–4 useful notes about behavior, assumptions, wiring, or validation.
- Update this brief as decisions and implementation change.

## 1. Project

A desktop-only, 2D pixel-art narrative action game in Java/libGDX, built for a two-day game jam that permits AI-written code. A man splits into himself and his inner monster. The deliberately incomplete story ends on a cliffhanger. Themes: **Time, Fire, Shadow**. Art is supplied by a two-person team.

The original fixed-ground/no-jump fight design was explicitly changed: fights now include double jump, aerial attacks, and dash. Do not remove these to match an older brief.

## 2. Technical constraints and conventions

- Repository: `C:\Mr monster`. Modules: `core`, `lwjgl3`; shared `assets/`.
- Java package root: `com.Version_1`. Source root: `core/src/main/java/com/Version_1/`. Java source compatibility is 11; current libGDX version is 1.14.2.
- `Main extends GameEngineBase`, which extends `ApplicationAdapter`. Do not migrate to `Game`/`Screen` architecture.
- Call `super.create()` first. The base owns `SpriteBatch`, runs `update(delta)`, clears the screen, calls `draw()` inside `batch.begin()/end()`, and calls `drawUI()` afterward.
- `OrthographicCamera` + `FitViewport`, virtual size **640×360**. Raw screen input must use `viewport.unproject()`.
- No Box2D use. Generated build dependencies already include some unused libraries; do not add or expand physics dependencies. Collision uses AABB rectangles.
- Load textures once in `Main`, pass resources into constructors, and dispose shared resources in `Main`. Entities/phases must not load their own textures.
- `Stage.draw()` stays outside the main batch block. `Actor.act()` overrides must call `super.act(delta)`.
- Use index loops over libGDX `Array`; its default iterator is not nestable.
- Spawn entities only through `world.spawn(entity)`. Call `entity.remove()` rather than modifying entity arrays directly.
- Combat overlap/damage decisions belong in `FightPhase`. `Player`, `Boss`, `Mob`, `Mob2`, and `Projectile` expose data and update their own state.
- Phases receive only needed dependencies, never `Main` itself.
- Prefer lowercase asset names with underscores for new assets. Existing filenames with spaces are already in use; do not rename them without discussion.
- Crack effect: static sprite overlay plus camera shake, no shader.
- Developer knows lifecycle, batching, cameras/viewports, input, animation state machines, and Scene2D Actions. Avoid unnecessary unfamiliar systems such as AssetManager, Skin, audio frameworks, or physics engines.

## 3. Story and themes

Story order to preserve:

1. Short inner-world boss fight, ending with a huge attack that cracks the screen.
2. Cut out of the body into the real world, with light gameplay.
3. Return inside the character as he seeks the monster to kill it.
4. Top-down exploration: NPC exposition and short fetch tasks, e.g. bringing a flower to a girl.
5. An NPC conversation reveals the monster, starting fight 2.
6. Cut to the real world partway through that fight.
7. End on a cliffhanger; optional fade and poem lines.

**Do not reveal the larger twist** that he was the monster and killed his better half; that is outside this jam.

- **Time:** nonlinear inner/real-world phase swaps. Inner world is dark; real world should be lighter.
- **Shadow:** implemented one-time 50%-health mob interlude; boss sinks into a black puddle under its feet, summons smaller mobs, and returns when the wave is cleared.
- **Fire:** planned rage attack, plus implemented boss-contact burning.

Only the opening boss fight is currently playable. Its temporary end card says the story continues; this does not mean the real-world/exploration/final ending is implemented.

## 4. Current player mechanics

### Controls

| Input | Fight behavior | Exploration behavior |
| --- | --- | --- |
| A/D | Horizontal movement | Horizontal movement |
| W | Jump, then one additional air jump | Move upward |
| S | No vertical movement | Move downward |
| J | Melee attack on ground or in air | Melee attack |
| Space | Dash | Dash along movement input |
| R | Retry after defeat; replay after opening ends | No implemented exploration flow yet |
| K | Planned full-rage attack; not wired | Not implemented |
| L | Planned block/parry; not wired | Not implemented |

`setTopDown(true)` switches to normalized WASD exploration movement; false enables fight gravity/jumps. `setGroundY(float)` places the player on a fight's ground line. The initial ground line is 60.

### Movement and attacks

- Base speed: **120 units/s**. Diagonal exploration input is normalized.
- Double jump: initial vertical speed **360**, gravity **800**, two jumps total before landing. First jump rises about **81 units** with about **0.9 s** airtime. Each air jump reapplies the upward impulse.
- Jump buffer: **0.15 s**. Ground grace/coyote time: **0.1 s**. Holding W does not repeatedly jump. Landing resets jump count; position clamps to the ground.
- Air control remains available. Attacks allow **50% movement speed**, with facing locked during the swing. Gravity continues during aerial attacks.
- Attack input buffer: **0.2 s**, consumed once. It accepts presses near attack/dash recovery; holding J does not automatically repeat. Aerial J now starts immediately, rather than waiting for landing.
- Attack damage: **10**. Horizontal reach: **45** (increased from 28; only player reach was increased). Active hit time: **0.10–0.25 s** into the swing. Phase collision tracking limits each target to one hit per swing.
- Three attack frames take **0.5 s total**, i.e. `ATTACK_FRAME_TIME = 0.5f / 3f` in `Main`.
- Current `ATTACK_WIDTH_SCALE` is **1.4**, centered on the player. The developer tuned this after earlier experiments with 1.3/2.0; preserve the actual current value unless asked to change it.
- Current idle pose turns left/right every **3 s** (`IDLE_TURN_TIME`); it was initially requested at 2 s and later tuned. Preserve the current value.
- Health: **100**. Normal post-hit immunity: **0.6 s**. Damage ignores nonpositive amounts.

### Dash

- Space cancels the current attack and clears its queued attack press.
- Speed **475 units/s**, normal duration **0.18 s** (~85.5 units), **0.65 s between dash starts**.
- Input direction determines dash direction; with no movement input, use facing. Direction remains locked through the dash.
- Dash grants invulnerability and has a cyan afterimage trail.
- Works airborne without consuming a jump; gravity keeps running.
- When a dash overlaps the boss, `FightPhase` extends it just enough to emerge on the other side, retaining immunity until clear. This also handles overlap on the final normal dash step.
- Boss movement boundaries reserve player-sized exits at both arena edges. Ordinary dashes away from enemies retain their original distance.

## 5. Current boss fight

- `Boss extends Enemy`; it receives player/world and animation dependencies through its constructor.
- Boss health: **240**. Sprite scale **2.6**, so the visible body remains about **2.55×** player height. Draw size accounts for the source sheet's transparent padding.
- Boss is grounded. Direction/distance helpers are x-only. It approaches the player and uses telegraphed attacks with recovery windows.
- **Slash:** 0.55 s warning, 18 damage, 150 units of horizontal reach, active at 0.32–0.56 s into the attack. The low attack box is 48 units high. Boss reach was not increased with player reach.
- **Ranged wave:** 0.65 s warning, cast emits once at 0.32 s, 14 damage, speed 180, 56×30 collision size, 4 s lifetime. Uses existing spell art rotated into a low horizontal wave. Spawn goes through `world.spawn()`; phase handles overlap/removal.
- Normal attack recovery: **0.7 s**. Ranged cooldown: **3.2 s**.
- Facing locks through attack windup/action, so crossing to the other side can evade a swing.
- At **50% health (120 HP)** the opening boss becomes untargetable and sinks into a black ground puddle over **0.9 s**. The crossing hit clamps at 120 HP so it cannot skip this interlude. Existing boss waves and burning clear on entry.
- While submerged it summons **two leeches and two bomb mobs**, alternating every **0.8 s** from its puddle. After all four die or finish exploding, it rises over **0.9 s**, recovers, and resumes combat at 120 HP. This happens once per fight; damage, slash and contact fire are disabled during sinking/submersion/rising. Ordinary bosses configured with zero finisher fraction retain their normal death path without this opening-specific interlude.
- At **25% health (60 HP)**, the one-time story finisher starts. A lethal hit clamps health at this threshold instead of skipping the ending. Further damage is ignored during the finisher.
- Finisher warning: **1.4 s**; impact occurs 0.4 s into the finishing animation. It clears stray projectiles, flashes/cracks the screen, and shakes the camera. Crack holds for **2.4 s**, then the phase completes.
- A zero finisher-health fraction supports ordinary death with a death animation before removal, for reuse later.
- Background eye animation is ping-pong, **0.65 s/frame**, slowed from 0.3 s/frame.
- Main currently provides simple health bars, combat controls, warnings, retry, and a temporary opening-end card.

### Boss collision leniency

Draw size is unchanged; collision boxes are deliberately smaller:

- `getBounds()` is the attackable hurtbox: **75%** of the original body height, grounded.
- `getContactBounds()` is the contact-fire box: **18 source pixels wide** (46.8 world units at scale 2.6), centered on the torso, and **50%** of original body height, grounded. It no longer spans the full 104-unit hurtbox width.
- Fire ignition tests this against the player's physical contact bounds (**24×55.04** at normal scale), excluding transparent sides of the 64×64 player sprite. Player melee and other existing hurtboxes are unchanged.
- A well-timed single jump can clear contact fire while aerial attacks can still connect with the torso. Descending into the lower body still ignites the player.
- Dash crossing and contact pushback use the contact box. Player melee targets the hurtbox.
- Do not enlarge these back to the full sprite bounds or silently extend the boss's slash hitbox.

### Contact fire

- Overlapping the boss's contact box while not dashing starts burning and deals an immediate eligible tick.
- Damage: **5 every 0.75 s**. Burn persists **1.5 s after separation**; renewed contact refreshes duration, not the tick rate.
- A successful fire tick while overlapping separates the player fully from the boss and applies outward knockback at **220 units/s**, decaying with drag. At walls it chooses the available arena side so the player is not trapped.
- Burn damage uses normal player immunity; dash immunity prevents damage and fresh ignition during crossing. Existing burn can remain visually active during a dash.
- Orange/gold flickering tint, small flames, and rising embers communicate fire. These use existing sprite/pixel resources, without a new particle framework or texture.
- Burning is cleared for defeat, boss death, or the finisher; it must not kill the player during the scripted ending.

## 6. Architecture and locked contracts

Current layout:

```text
core/src/main/java/com/Version_1/
  Main.java
  Engine/GameEngineBase.java
  Anim/AnimationStateMachine.java
  Entities/Entity.java, Attackable.java, Interactable.java, Player.java, NPC.java, Projectile.java
  Entities/Enemies/Enemy.java, Boss.java, Mob.java, Mob2.java
  World/World.java
  Phases/Phase.java, FightPhase.java, ExplorePhase.java, RealWorldPhase.java
```

`NPC`, `ExplorePhase`, and `RealWorldPhase` are currently placeholders. `DialogueBox` is planned, not implemented. `Mob` and `Mob2` are wired into the opening's shadow interlude.

### Ownership

- `Main` owns camera, FitViewport, Stage, font, and loaded textures/animation definitions. `buildPhase(int)` is the single story-order switch; null means unwritten/completed story.
- `Main.update()` ends with camera update and batch projection assignment. `drawUI()` draws the Stage; `resize()` updates viewport with centering.
- `FightPhase` owns one fight's World/actors, combat collision rules, hit-per-swing tracking, burn timers, and crack/shake sequence. It accepts needed dependencies, not Main. Its constructor flushes initial actor spawns with `world.update(0)`.
- `Player` no longer needs World in its constructor: `Player(float x, float y, float width, float height, Map<State, Animation<TextureRegion>> animations)`.
- Player exposes `isAttackActive()`, `getAttackBounds()`, `getAttackDamage()`, `getAttackId()`, jump/ground/dash and burn helpers. Boss exposes its melee data, hurt/contact bounds, warnings, and latched finisher status.
- `Projectile` only moves, animates, expires, and exposes damage/owner. FightPhase applies damage/removal.
- `Enemy` owns shared health/target references and x-only targeting helpers. Keep it reusable.
- Shared persistent game-state class is still missing. Before adding later phases, agree on its name/data and have Main own it. Health, rage, and quests should eventually survive phase disposal through that data, rather than hidden globals.

Do not silently change these locked contracts:

```java
public interface Attackable {
    void takeDamage(int amount);
    boolean isDead();
    Rectangle getBounds();
}

public interface Interactable {
    void interact();
    Rectangle getBounds();
}

// Entity is abstract and implements neither interface.
// protected Vector2 position; protected float width, height
// Entity(float x, float y, float width, float height)
// abstract void update(float delta);
// abstract void draw(SpriteBatch batch);
// Rectangle getBounds(); // rebuilt from position/size each call
// Vector2 getPosition(); void remove(); boolean isRemoved();

public interface Phase {
    void update(float delta);
    void draw(SpriteBatch batch); // inside Main's batch block
    boolean isFinished();
    void dispose();
}

// World:
// void spawn(Entity e); // queued; added AFTER current update pass
// void update(float delta); // update, remove flagged entities, flush pending
// void draw(SpriteBatch batch);
// Array<Entity> getEntities(); // read-only use; index loops only
```

`AnimationStateMachine<T>` is enum-keyed, wraps `Animation<TextureRegion>`, validates registered states, and provides `setState`, `restart`, `update`, `getCurrentFrame`, `getStateTime`, `isAnimationFinished`, and `getCurrentState`. `setState` does not restart the same state. Use LOOP for idle/walk and NORMAL for one-shot attacks; explicitly restart repeated attacks. Check completion for the one-shot actions, not idle/walk loops.

## 7. Assets and animation layout

### Player

- `assets/sprites/combined_spritesheet.png`: 566×356. Player is the last row; spacing is irregular.
- Main crops six 64×64 regions at y=277, x origins **108, 166, 214, 272, 326, 390**. Preserve equal crop sizes and foot alignment.
- Idle uses frame 1 with horizontal turns. Walk loops frames 1–3 at 0.1 s/frame. Attack uses frames 4–6 once at 0.5/3 s/frame.
- Idle/walk source art faces LEFT, attack source art faces RIGHT. Rendering accounts for this per state. Do not use one source-facing flag for every animation.
- `assets/sprites/girl_jump_spritesheet_4x48x80.png`: 192×80, four 48×80 frames left-to-right. Poses are TAKEOFF, RISE, FALL, LAND, selected from jump physics rather than looping in time. Render with preserved proportions/foot alignment.
- `assets/sprites/girl_stop_block_spritesheet_3x48x64.png` exists for future block/parry; not integrated.

### Unified boss atlas

Runtime loads **one** `assets/big_boss/boss_combined.png` (2240×1280, currently about 80 KB). Source assets are retained, not deleted. All source boss cells are 140×93.

| Row / region | Pixel y | Contents |
| --- | --- | --- |
| 0 | 0 | Idle, 8 frames |
| 1 | 93 | Walk, 8 frames |
| 2 | 186 | Attack, 10 frames |
| 3 | 279 | Cast, 9 frames |
| 4 | 372 | Spell, 16 frames |
| 5 | 465 | Hurt, 3 frames reserved; currently unused |
| 6 | 558 | Death, 10 frames |
| Eye strip | 651 | Four 256×256 frames, x=0/256/512/768 |
| Crack | 907 | 640×360 overlay, x=0 |
| White pixel | 1279 | x=2239; used for rectangles, fire, health bars |

Original consolidated source sheets are `assets/big_boss/attack.png`, `idle.png`, and `walk.png`. Cast/Spell/Hurt/Death source frames live in corresponding subdirectories. Main crops each spell cell at local x=50, y=32, size 36×61 before the projectile rotates it.

Atlas packing helper is `build/pack_boss_atlas.ps1` if still present locally. It copies source pixels without rescaling and packs the eye, crack, and white pixel. `build/` is ignored; this helper is not a tracked project dependency.

Other supplied assets are available under `assets/health and rage/`, `assets/sfx/`, and `assets/ui elements/`. Health/rage artwork, music, and menu art exist but are not wired into gameplay/UI yet. Do not assume presence means implementation.

## 8. Agreed but unfinished features

### Rage

Agreed design:

- Gain rage proportional to damage actually received.
- Full rage stays ready until the player presses **K**; do not auto-fire merely because it fills.
- Launch one animated travelling wave in the player's launch-facing direction.
- Pass through enemies, damaging each target once per wave.

Current code only has a rage float/max and clamped `addRage`; damage does not fill it and K does nothing yet. Still ask about conversion rate, rage-wave art, damage/speed/range, meter UI, reset/drain rules, and persistent state. Resetting to zero after firing was suggested but not explicitly finalized.

### Block/parry

**L** is reserved for a block/parry mechanic. It is not implemented. Decide parry window/recovery, whether holding L blocks afterward, damage reduction, and enemy stun/counterattack behavior. Do not silently pick these rules.

### Mobs and summoning (implemented 18 October)

- `Mob` uses **`assets/evileye_shadow/waechter-20 Charset.png`**. It flies toward the player at 82 units/s, latches on physical contact, and drains **4 HP every 0.8 s**, subject to normal player immunity. **Dash shakes it off**, drops it back to ground level, and prevents relatching for **1 s**. It can chase and attach again afterward.
- `Mob2 extends Mob` to reuse flying movement and rendering. It uses **`assets/eye_shadow/waechter-19.png`**, stops when the player enters its blast bounds, flashes for **0.7 s**, then explodes once for **16 damage** within a **92×92** box centered on itself. Escape or dash avoids the blast; killing it during the fuse cancels it. Blast visual holds **0.25 s**, then removes the mob.
- Punches now give both mobs a **0.18 s stagger**, outward recoil starting at **160 units/s** with drag, red hit tint, squash/stretch and bright impact sparks. Stagger prevents new latches and briefly pauses an armed bomb's fuse before it resumes. Lethal punches show a **0.25 s** shrinking fade rather than instantly disappearing; the boss waits for defeat visuals to finish before returning.
- The puddle is anchored at the boss's original sinking position and stays visible afterward, with a subtle purple rim for readability on black. All four mobs emerge from its **same center point** over **0.4 s**, alternating slight left/right spreading before pursuit. No latch or bomb arming during emergence. The wave remains finite: **four total, no replacements**, as explicitly confirmed by the developer.
- Both have **20 HP**, a **30×32** draw/body size, and remain much smaller than the boss (and smaller than the player). Main owns both textures, crops three right-facing frames from charset row 2, builds shared looping animations, and disposes textures.
- FightPhase resolves attachment, drain and blast collisions/damage, owns the four-mob wave, draws the puddle and blast warnings, and resumes the boss only after the wave clears. No new textures or physics dependencies were added.
- The old unused melee Mob constructor/state machine was replaced for this requested leech behavior. FightPhase's constructor now also receives the two shared mob animations. Locked Entity/Attackable/Phase/World contracts are unchanged.

## 9. Scope and build order

Opening boss fight through screen crack is now playable. Continue only with the next feature/class the developer requests. Later fights should reuse FightPhase with different parameters rather than duplicate logic.

If time runs short, cut in this order: sound, particles, extra shake, extra boss phase, optional Mob2, second obstacle type. Never cut the narrative ending. Reserve the last 3–4 hours for art fixes, bugs, and producing a submittable build.

The end card after the opening is temporary. Real-world scenes, NPC dialogue/exploration, fight 2, and the true jam cliffhanger still need implementation.

## 10. Progress checklist

- [x] Repo skeleton, GameEngineBase, Main, Entity/interfaces, Phase, World
- [x] AnimationStateMachine with validation/restart
- [x] Player movement, health, ground/air melee, animation selection
- [x] Double jump, ground clamp, jump buffer, coyote time
- [x] Attack movement/facing lock, input buffer, longer player reach
- [x] Dash, attack cancel, immunity, boss pass-through
- [x] Contact fire, lingering burn, pushback, visual feedback
- [x] Enemy base
- [x] Boss slash, ranged wave, death path, 25% finisher
- [x] Projectile movement/animation/lifetime and phase collision
- [x] Unified boss atlas and slower background eye
- [x] FightPhase wired into Main, health bars, warnings, R retry/replay
- [x] Screen crack and camera shake for opening
- [x] Mob leech/drain/dash detachment and Mob2 fuse/explosion with supplied assets
- [x] 50%-health shadow puddle, boss sink/return and four-mob interlude
- [ ] Shared persistent state
- [ ] Rage fill, K wave, rage UI
- [ ] L block/parry
- [ ] Supplied healthbar/menu/music integration
- [ ] RealWorldPhase
- [ ] NPC + DialogueBox + dialogue data
- [ ] ExplorePhase and fetch task
- [ ] Fight 2, real-world cut, final cliffhanger
- [ ] Polish and distributable build

## 11. Remaining decisions

Ask before implementing ambiguous behavior, particularly:

- Playtest mob count, health, drain cadence and bomb fuse/radius; current defaults are documented above.
- Rage conversion and wave tuning/assets/reset rules.
- Block versus parry semantics, timing, and counters.
- Shared game-state class name/data and persistence rules.
- Story triggers/configuration for the later phases and fight 2.
- Two-person team ownership split remains undecided. Real world should be lighter than inner world; exact palette/background is still to be chosen.

## 12. Validation and local repository handoff

As of this handoff:

- Core and desktop Java sources compile against cached dependencies with `javac --release 11`.
- Latest shadow pass: core/desktop compilation, `build/ShadowFightCheck.java`, and existing `PlayerJumpCheck.java` passed. Coverage includes physical-only fire/expiry, 50% damage clamp, sink timing, projectile clearing, exact four-mob wave, clear/return/one-time trigger, 25% finisher completion, leech drain/dash detachment, bomb fuse/single blast/escape/kill cancellation, and existing jump/attack/dash movement. The follow-up `MobFeedbackCheck.java` covers punch recoil/stagger, fatal fade, armed-bomb feedback and fixed-point emergence. Hidden LWJGL `MobRenderCheck.java` passed actual melee-hit checks and captured sinking, emergence, wave, punch, defeat and return; the emergence/punch/return PNGs were visually inspected. Manual gameplay tuning is still useful.
- Local checks cover jump stages/height, double-jump limits, coyote/buffer behavior, attack movement, air attacks/gravity, reach in both directions, dash cancellation/immunity/cooldown/crossing, boss/projectile damage, contact fire/expiry/pushback/walls, reduced collision boxes, finisher threshold and camera reset.
- Previous hidden LWJGL render checks verified boss size/facing, warnings/slash/wave, jump, crack, burn/embers, dash trail, end card, and both retry flows. The most recent air-attack/bounds/reach changes passed compilation and behavior checks; continue playtesting the feel.
- Ignored `build/` contains temporary Java check harnesses, a cached runtime classpath file, atlas helper, and preview PNGs if still present. Do not stage these or add test dependencies merely to retain them.
- The Gradle wrapper can need permission to access the user's external Gradle cache. Java is installed under `C:\Program Files\Java\jdk-24`; source target remains Java 11. Do not modify build settings simply to work around environment access.
- `assets/assets.txt` is generated by Gradle's `generateAssetList`. Earlier stash conflicts left markers in it; those were removed. Do not treat it as hand-authored gameplay data. Do not change tracking/ignore rules unless requested.
- Gameplay/code changes are currently local and unstaged. `boss_combined.png` and `Mob2.java` are now tracked. `assets/assets.txt` already had a local edit before this shadow pass and was left untouched. Recheck `git status` before acting; the developer may change files between chats.
- Older backup stashes named **Local changes v2** and **Local stuff before pull** remain. They are older snapshots, not replacements for the latest working files. Do not apply/pop/drop them without instruction.
- No final push has been performed by the assistant. The developer must review and explicitly authorize staging/commits/pushes.
