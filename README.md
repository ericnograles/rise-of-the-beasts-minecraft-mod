# Rise of the Beasts

A standalone mod for Minecraft Java Edition **1.20.1**, Forge **47.4.10** and Java **17**. Download the [installable JAR](dist/rise-of-the-beasts-1.0.0.jar) and put it in the Forge profile’s `mods` folder. For multiplayer, install the same JAR on the server and each client. Explore new Overworld terrain for natural caverns.

Watch the [narrated playthrough](Rise-of-the-Beasts-1.0.0-Playthrough.mp4) for a first-person tour of all three encounters, the stomach escape, and the final reward. The 1:41 video features an AI-generated English voiceover, matching captions, and eight chapter markers. Encounters are staged and boss health is advanced for the recording.

### Windegow

Look for the skull and branching antlers in dark forests at night. This rare hunter has **1,000 HP** and can leap to close the gap. When you hear the high shriek and see its arms lift, **stop moving**. It checks each player separately and holds its scratch when its target stops. Looking around is safe. Dawn kills it.

### Leviathan

Craft a **Sounding Shell** from a compass and a nautilus shell, then use it to get vent coordinates. Bring Water Breathing, Night Vision and a boat. Dive through the rings of sea lanterns in the deep ocean. Descending below Y 40 wakes one Leviathan per cavern. The tentacles extend south and the jaw is about 360 blocks north of the entrance at Y −42.

The **1,200-block** animal has **600 HP**. Its jaw opens for a lunging bite (6 damage). Its spotted tentacles curl and pull swimmers in (4). Its charge drives the head forward and knocks nearby players away (6). Back away from the active body part during the roar. The head dies after **1,200 consecutive dry game ticks**. Water around the jaw resets the count, including a one-block-deep pool.

The drawing’s **1,860-block** width fits the Overworld. The flooded chamber has a domed ceiling, with about **50 blocks of water** from Y −55 to −6 near the center. A roughly 16-block-wide shaft connects it to the ocean. The sketch’s 1,000-block height exceeds the Overworld’s build height.

### Jormungandr

Sneak and use an **echo shard** at the surface of an Overworld ocean. A serpent answers in local chat. Its head faces north and its armored back extends **1,800 blocks** south. You can damage it from any nearby segment. The boss bar tracks the shared **40,800 HP** pool.

- At the jaw, dodge the 8-damage bite and the spread of mouth spikes. If swallowed, walk to the **shroomlight in the far wall** and right-click it. Escape also triggers when the serpent changes phase, dies, or you rejoin after an interruption.
- Along the body, watch for an 8-damage slash or a blast that throws you roughly 20 blocks in open air. Near the tail, sidestep the 4-by-4 stone projectile (5 damage).
- The first defeat splits the armor in a flash of flame. Green spines and orange seams appear along the **2,560-block** second phase. It starts with **40,800 HP**. Fire launches a 9-damage fireball. Poison delivers 16 one-point pulses over 20 seconds. Burst accelerates movement for five seconds and deals zero direct damage. Mystery chooses another available move.
- The final defeat drops one **Star of the World Serpent**, a named Nether Star, at the segment that took the finishing blow.

The ocean ritual, second health pool, stomach valve and victory drop are gameplay choices for details left open in the drawings. All damage figures are base Minecraft HP before defensive effects. Two HP equal one heart.

### Quick checks

Use the three spawn eggs in Creative, or run these commands separately in a test world. Switch to Survival for attacks.

```mcfunction
/time set midnight
/summon riseofthebeasts:windegow ~ ~ ~6
/summon riseofthebeasts:leviathan ~ ~ ~16
/summon riseofthebeasts:jormungandr ~ ~ ~16
```

Try Leviathan in a shallow pool, drain the water around its head, then refill before a minute passes. To inspect the serpent’s transformation quickly, use `/kill @e[type=riseofthebeasts:jormungandr,sort=nearest,limit=1]`, wait for the flame cue to finish, and repeat for final defeat.

Giant bodies follow gently undulating paths through their encounter sites. Summon them in open space to see the silhouette. Only sections within about 78 blocks of a player are simulated and rendered. The saved encounter owns health, phase, position, stranded time and pending attacks. Sections can unload and reform when a player travels along the body. The land timer advances while the jaw is active.

### Build and test

The wrapper pins Gradle **8.8**. ForgeGradle **6.0.54** uses official **1.20.1** mappings.

```sh
./gradlew build runGameTestServer
./gradlew runClient
./gradlew runServer
```

The build reobfuscates the installable JAR. The GameTests exercise individual movement, scratch cancellation, shallow water and stranding, stomach escape, both defeats and the final reward. The dedicated development server uses `run-server/` and requires the server owner’s Minecraft EULA acceptance.
