# \# Mr. Monster: Game Jam Brief (paste this to any LLM first)

# 

# \## 0. How to use this file

# Paste this whole file at the start of a new chat, then say which task you want (e.g. "write `Boss.java`").

# Section 9 holds the rules the LLM must follow. Section 10 is the live progress checklist: update it as you go.

# 

# \## 1. Project in one paragraph

# 2D pixel-art narrative action game in \*\*libGDX (Java), desktop only\*\*, built in a \*\*2-day game jam\*\* ("vibecoding game dev", so AI-written code is allowed). A man splits into himself and his inner monster. Story is deliberately incomplete and ends on a cliffhanger. Art is self-made. Jam themes: \*\*Time, Fire, Shadow\*\*.

# 

# \## 2. Tech constraints

# \- Java, libGDX, IntelliJ, project generated with gdx-liftoff (modules: `core`, `lwjgl3`, shared `assets/`).

# \- Existing reusable code from the developer's earlier work: `GameEngineBase` (abstract, extends `ApplicationAdapter`, owns the `SpriteBatch`). Each frame it runs `update(delta)`, then `draw()` \*\*inside\*\* `batch.begin()/end()`, then `drawUI()` (empty hook, called \*\*after\*\* `batch.end()`, used for `Stage.draw()`). It draws no background and has no `resize()`. Subclasses must call `super.create()` first. Also a generic `AnimationStateMachine<T>` in `Version_1.Anim` (enum-keyed, wraps `Animation<TextureRegion>`). API: `setState(T)` (no-op if already in that state), `restart()` (replays the current animation from frame 0), `update(delta)`, `getCurrentFrame()`, `getStateTime()`, `isAnimationFinished()`, `getCurrentState()`. The constructor and `setState` throw if a state has no registered animation, so every enum value used must be in the map. Use `PlayMode.LOOP` for idle/walk and `PlayMode.NORMAL` for attacks, because `isAnimationFinished()` is only ever true for non-looping animations..

# \- Camera/viewport: `OrthographicCamera` + `FitViewport` (640x360 virtual size, set in `Main`). Raw screen input must go through `viewport.unproject()`.

# \- \*\*No Box2D.\*\* Use simple AABB (`Rectangle.overlaps`) for collisions.

# \- Scene2D is available (Stage/Actions) for UI and fades. `Stage.draw()` must be called OUTSIDE the main `batch.begin()/end()` (that is what `drawUI()` is for).

# \- If overriding `Actor.act`, always call `super.act(delta)`.

# \- Developer knows: lifecycle, SpriteBatch, cameras/viewports, input, animation state machines, Scene2D through Actions. Has NOT done: Screens/Game class, AssetManager, audio, particles, Box2D, Skin.

# 

# \## 3. Themes and how they appear

# \- \*\*Time:\*\* the story loops between the character's inner world and the real world, told non-linearly. Mechanically this is just phase swapping (inner, real, inner, real). Real world must look different so jumps read clearly: inner world is dark, real world is a bit lighter.

# \- \*\*Shadow:\*\* the boss spawns mobs through shadows. Visual: a shadow puddle appears (telegraph), then a mob rises from it.

# \- \*\*Fire:\*\* a \*\*rage meter\*\* that fires up the player. (Exact rules are an OPEN QUESTION, see section 11. Keep it to one float plus a threshold effect.)

# 

# \## 4. Story beats (in order)

# 1\. Short inner-world boss fight. Boss ends with a huge attack that \*\*cracks the screen\*\*.

# 2\. Out of the body into the \*\*real world\*\* (light gameplay).

# 3\. Back \*\*inside\*\* the character: he tries to find the monster in him in an attempt to kill him.

# 4\. \*\*Exploration scene:\*\* walk around, collect exposition from NPCs, short fetch tasks (e.g. take a flower to a girl at the end of the room) to feel natural.

# 5\. A conversation with an NPC \*\*reveals the monster again\*\* (start of fight #2).

# 6\. Cut to the \*\*real world\*\* partway through the fight again.

# 7\. \*\*End (cliffhanger).\*\* Optional: fade to black plus a line or two of poem text.

# 

# Bigger twist (NOT in this jam): he WAS the monster and killed his better half. Do not reveal it.

# 

# \## 5. Enemy design

# \- \*\*Boss:\*\* summons mobs through shadows, uses ranged attacks (projectiles).

# \- \*\*Mob1:\*\* approaches the player and attacks.

# \- \*\*Mob2:\*\* optional, first thing cut. If both mobs only differ by stats/sprite, they are ONE `Mob` class with parameters.

# \- Fight 1 and fight 2 should reuse the same `FightPhase` with different parameters.

# 

# \## 6. Architecture

# 

# \### Folder layout

# Java package root is `Version\_1`; each folder below is its own sub-package (e.g. `Version\_1.Entities`).

# ```

# core/src/main/java/Version\_1/

# ├── Main.java            (orchestrator, extends GameEngineBase; see below)

# ├── Engine/              GameEngineBase

# ├── Anim/                AnimationStateMachine

# ├── Entities/            Entity, Attackable, Interactable, Player, Npc, Projectile

# ├── Entities/Enemies/    Enemy, Boss, Mob

# ├── World/               World

# ├── Phases/              Phase, FightPhase, ExplorePhase, RealWorldPhase

# └── UI/                  DialogueBox

# assets/  sprites/(player,boss,obstacles)  backgrounds/  ui/  fonts/

# ```

# 

# \### The orchestrator: `Main`

# \- `Main extends GameEngineBase`. It owns the shared things: camera, `FitViewport`, the Scene2D `Stage` (shares the viewport), and all loaded textures.

# \- It runs \*\*one `Phase` at a time\*\*. When `phase.isFinished()` it disposes that phase, resets the camera, and builds the next one.

# \- \*\*Story order lives in one place:\*\* the `buildPhase(int index)` switch in `Main`. `null` means the story is over / not written yet.

# \- In `update()`, `Main` ends with `camera.update()` and `batch.setProjectionMatrix(camera.combined)` so the batch uses the camera before drawing. `drawUI()` calls `stage.draw()`. `resize()` calls `viewport.update(width, height, true)`.

# \- Exposes `getCamera()`, `getViewport()`, `getStage()`.

# \- \*\*Phases receive only what they need through their constructors\*\* (camera, stage, textures, shared state). \*\*Never pass `Main` itself into a phase.\*\*

# 

# \### Shared state across phases

# Phases are disposed on every swap, so anything that must survive (player stats, quest flags such as "has flower", rage meter) cannot live inside a phase. Plan: one small plain-data class held by `Main` and passed into phases through constructors. (Name and contents: see section 11.)

# 

# \### Locked contracts (DO NOT change signatures without telling the developer)

# ```java

# // Entities/Attackable.java: player and all enemies

# public interface Attackable {

# &#x20;   void takeDamage(int amount);

# &#x20;   boolean isDead();

# &#x20;   Rectangle getBounds();

# }

# 

# // Entities/Interactable.java: NPCs / quest objects

# public interface Interactable {

# &#x20;   void interact();

# &#x20;   Rectangle getBounds();

# }

# 

# // Entities/Entity.java (abstract)

# // fields: protected Vector2 position; protected float width, height

# // ctor:   Entity(float x, float y, float width, float height)

# // abstract void update(float delta); abstract void draw(SpriteBatch batch);

# // Rectangle getBounds()   // rebuilt from position/size each call

# // Vector2 getPosition(); void remove(); boolean isRemoved();

# 

# // Phases/Phase.java

# public interface Phase {

# &#x20;   void update(float delta);

# &#x20;   void draw(SpriteBatch batch);   // called between batch.begin()/end()

# &#x20;   boolean isFinished();           // true -> Main swaps to next phase

# &#x20;   void dispose();

# }

# 

# // World/World.java

# // spawn(Entity e)  -> queued, added AFTER the update pass (safe mid-update)

# // update(float delta)  -> update all, remove isRemoved(), then flush pending

# // draw(SpriteBatch batch)

# // Array<Entity> getEntities()  -> read-only; iterate with INDEX loops, never for-each

# ```

# `Entity` implements neither interface. Subclasses write `extends Entity implements Attackable`.

# 

# \### Class plan

# \- `Player extends Entity implements Attackable`: movement, one attack, health, rage meter.

# \- `Enemy` (abstract, extends Entity, implements Attackable): receives `Player` and `World` via \*\*constructor\*\* (no hidden globals). Helpers: `directionToPlayer()`, `distanceToPlayer()`.

# \- `Boss extends Enemy`: state machine of 2-3 telegraphed attacks, summons via `world.spawn(...)`, ranged attacks spawn `Projectile`s via `world.spawn(...)`.

# \- `Mob extends Enemy`: chases and melees; parameterized (speed, hp, damage, sprite).

# \- `Projectile extends Entity`: moves, damages on overlap, removes itself.

# \- `Npc extends Entity implements Interactable`: holds a dialogue id and calls the DialogueBox on `interact()`. Dialogue text lives in data, not inside the NPC.

# \- `DialogueBox`: typewriter-style text box with a "next" action (Scene2D).

# \- Phases: `FightPhase` (parameterized), `ExplorePhase`, `RealWorldPhase`. `Main` holds the current phase and swaps when `isFinished()`.

# 

# \## 7. Technical conventions

# \- Load textures ONCE in `Main`, pass them into constructors. Never load inside a phase or entity constructor.

# \- Entities never remove themselves from lists: call `remove()`, World cleans up.

# \- Mid-game additions only via `world.spawn(e)`.

# \- Index loops over libGDX `Array` (its for-each iterator is not nestable).

# \- Asset names: lowercase\_with\_underscores, state in the name (`boss\_attack.png`).

# \- Collision: `a.getBounds().overlaps(b.getBounds())` inside the phase, not inside entities.

# \- Phases get dependencies via constructor; never receive `Main`.

# \- Simplest viable design wins. No extra abstraction layers, no frameworks.

# \- Screen crack effect = crack sprite overlay + camera shake. No shader.

# \- Do not reference classes that do not exist yet without saying so.

# 

# \## 8. Scope and cut list

# Build order: \*\*fight 1 through the screen-crack, end to end, first.\*\* Everything else reuses it. If time runs out, that opening is still a complete, shippable build.

# 

# Cut order if behind: sound, particles, extra screen shake, boss phase 2, Mob2, second obstacle type. \*\*Never cut the ending.\*\*

# Reserve the last 3-4 hours for art fixes, bug fixes, and producing a submittable build.

# 

# \## 9. Rules for the LLM

# 1\. Follow the locked contracts in section 6 exactly. If you think one must change, say so and stop. Do not silently change it.

# 2\. Output \*\*complete files\*\* (full class, imports included, correct `Version\_1.\*` package line), not fragments, unless I ask for a patch.

# 3\. Only write the file(s) I ask for. Do not generate extra classes "for convenience."

# 4\. Keep code simple and commented only where non-obvious. No unnecessary design patterns.

# 5\. If something I want depends on a class I have not shown you, ask for its signature or state your assumption in one line.

# 6\. Use libGDX idioms from section 2/7 (index loops, spawn queue, `super.act`, `viewport.unproject`).

# 7\. Do not add Box2D, new dependencies, or Screen/Game-class architecture. Never pass `Main` into phases.

# 8\. After writing code, list in 2-4 bullets any assumptions or things I must wire up elsewhere.

# 

# \## 10. Progress checklist (update and re-paste)

# \- \[✅] Repo created, skeleton committed (GameEngineBase, Main, Entity, Attackable, Interactable, Phase, World)

# \-\- [x] `AnimationStateMachine` moved to `Version_1.Anim` (with validation and `restart()`)

# \- \[ ] Shared game-state class (quest flags, rage meter, player stats)

# \- \[ ] `Player` (move, attack, health)

# \- \[ ] `Enemy` base

# \- \[ ] `Mob`

# \- \[ ] `Boss` (attacks, shadow summon, projectiles)

# \- \[ ] `FightPhase` (fight 1) playable, wired into `Main.buildPhase`

# \- \[ ] Screen-crack ending of fight 1

# \- \[ ] `RealWorldPhase` (light gameplay)

# \- \[ ] `Npc` + `DialogueBox`

# \- \[ ] `ExplorePhase` (NPCs, fetch quest)

# \- \[ ] Rage meter

# \- \[ ] Fight 2 + cut to real world + ending

# \- \[ ] Polish, build, submit

# 

# \## 11. Open questions (decide, then record the answer here)

# \- Rage meter rules: what fills it, what happens when full, does it drain? \*\*Not decided\*\*

# \- How does the real world look vs. the inner world (palette/background)? \*\*Inner world is dark, real world is a bit lighter\*\*

# \- Team size and who owns code / art / writing? \*\*2 people.\*\* Ownership split: not decided

# \- Final package name: \*\*Version\_1\*\*

# \- Controls: \*\*WASD to move, SPACE, JKL for attacks\*\* (if only two attacks are finalized, J and K only)

# \- Shared state: where do quest flags / rage / player stats live across phases? \*\*Proposed: one plain-data class owned by `Main`, name TBD\*\*

