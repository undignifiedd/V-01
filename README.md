# The Other Half

A 2D pixel-art narrative action game about a man and his inner monster, created for a game jam around **Time, Fire, and Shadow**. Fight, explore, and discover what waits on the other side.

## Play the game — judges and players

**[Download The Other Half for Windows (64-bit)](https://github.com/undignifiedd/V-01/releases/download/v1.0.0-jam/TheOtherHalf-Windows-x64.zip)**

1. Download the ZIP from the link above or the [release page](https://github.com/undignifiedd/V-01/releases/tag/v1.0.0-jam).
2. Extract the entire ZIP.
3. Open the extracted `TheOtherHalf` folder and double-click **TheOtherHalf.exe**.

Java is bundled; no separate Java installation is needed. Keep the `app` and `runtime` folders beside the EXE, and run the game from the extracted folder.

Choose **Start** from the main menu to begin at the opening boss fight. This build includes both boss fights, the exploration and flower quest, boss-fight music, and the final "To be continued..." ending.

## Controls

| Key | Action |
| --- | --- |
| A / D | Move left / right |
| W | Jump and double jump in combat; move up while exploring |
| S | Move down while exploring |
| J | Melee attack, including in the air |
| Space | Dash |
| K | Special attack when the rage meter is full |
| E | Interact and advance dialogue |
| Enter | Advance dialogue |
| Y / N | Accept / decline the flower request |

## Build from source

Built with Java and [libGDX](https://libgdx.com/), starting from [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.
