# 🌀 Random Teleport

A Minecraft mod. **Press `G` in-game to teleport to a random nearby location.**

| | |
|---|---|
| Mod loader | Fabric |
| Minecraft | 26.1.2 |
| Java needed | 25 or newer |
| Fabric API | required |

## See the code

| File | What it does |
|---|---|
| 📄 [RandomTeleportMod.java](src/main/java/com/example/RandomTeleportMod.java) | Starts the mod when Minecraft loads |
| 📄 [RandomTeleportClient.java](src/client/java/com/example/client/RandomTeleportClient.java) | Watches for the `G` key and does the teleport |
| 📄 [fabric.mod.json](src/main/resources/fabric.mod.json) | The mod's name, version, and what it needs |
| 📄 [build.gradle](build.gradle) | Build instructions |

## Build it yourself

```bash
git clone https://github.com/cmadisons/random-teleport.git
cd random-teleport
./gradlew build
```

The finished mod appears in `build/libs/` as a `.jar` file.

> **Need Java 25.** If `./gradlew build` complains about Java, install it with
> `brew install openjdk@25`, or point Gradle at your JDK with
> `JAVA_HOME=/path/to/jdk-25 ./gradlew build`.

## Install it

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 26.1.2
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) and put it in your `mods` folder
3. Put this mod's `.jar` in your `mods` folder too
4. Launch Minecraft with the Fabric profile
5. Press **G**

## My other projects

⚾ [All Live Baseball](https://github.com/cmadisons/all-live-baseball) ·
🎮 [Minigames](https://github.com/cmadisons/minigames) ·
🧪 [Example Mod](https://github.com/cmadisons/example-mod)

Made by Starbr0 · [@cmadisons](https://github.com/cmadisons) · License: CC0-1.0
