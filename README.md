# Rise of the Beasts

A standalone mod for Minecraft Java Edition **1.20.1**, Forge **47.4.10** and Java **17**. Download the [installable JAR](dist/rise-of-the-beasts-1.1.0.jar) and put it in the Forge profile’s `mods` folder. For multiplayer, install the same JAR on the server and each client. Explore new Overworld terrain for natural caverns.

Watch the [1.1 narrated playthrough](Rise-of-the-Beasts-1.1.0-Playthrough.mp4) for a 1:33 tour with an AI-generated English voiceover, optional captions, and 11 chapters. The new boss footage was recorded in Minecraft. The encounters are staged and boss health is advanced to show both phases and the endings.

| Time | In the playthrough |
| --- | --- |
| 0:00 | Windegow in the forest, followed by the descent through the lantern shaft |
| 0:24 | Leviathan awakens and spreads the eight arms attached to his head |
| 0:40 | The jaws open before the screech. Listen for the call and watch the two-second stun |
| 0:48 | Leviathan’s arms fold inward as he sinks |
| 0:54 | Jormungandr rises. The wide shot shows his full 200-block height, followed by the view from the waterline |
| 1:09 | The stomach escape and the shift to the second health bar |
| 1:23 | The World Serpent’s final descent and the victory drop |

The [original 1.0 playthrough](Rise-of-the-Beasts-1.0.0-Playthrough.mp4) is also available.

### Windegow

Look for the skull and branching antlers in dark forests at night. This rare hunter has **1,000 HP** and can leap to close the gap. When you hear the high shriek and see its arms lift, **stop moving**. It checks each player separately and holds its scratch when its target stops. Looking around is safe. Dawn kills it.

### Leviathan

Craft a **Sounding Shell** from a compass and a nautilus shell, then use it to get vent coordinates. Bring Water Breathing, Night Vision and a boat. Dive through the rings of sea lanterns in the deep ocean. Descending below Y 40 wakes one Leviathan per cavern. The tentacles extend south and the jaw is about 360 blocks north of the entrance around Y −46.

The **1,200-block** animal has **600 HP**. Its dark-blue mantle spans 44 blocks, with eight orange-lined arms attached around the back of the skull. Watch the mouth and arm tips during the wind-up. The jaws flare and snap for a lunging bite (6 damage), a curling arm pulls swimmers in (4), and a charge drives the head forward (6). When its mouth opens wide and trembles, get more than 28 blocks clear of the head. The screech stuns nearby Survival players for two seconds, interrupting movement and attacks. The intro shows the arms opening into a fan. On death, they draw inward as the head rolls and sinks. The head dies after **1,200 consecutive dry game ticks**. Water around the jaw resets the count, including a one-block-deep pool.

The drawing’s **1,860-block** width fits the Overworld. The flooded chamber has a domed ceiling, with about **50 blocks of water** from Y −55 to −6 near the center. A roughly 16-block-wide shaft connects it to the ocean. The sketch’s 1,000-block height exceeds the Overworld’s build height.

### Jormungandr

Sneak and use an **echo shard** at the surface of an Overworld ocean. Jormungandr, **The World Serpent**, answers in local chat. The camera follows his emergence as he rears to **200 blocks** from the waterline to the tips of his crown. During combat he periodically lowers his jaw to the surface. The custom boss bar changes from green to amber for phase two. Its head faces north and its armored back extends **1,800 blocks** south. You can damage it from any nearby segment. The boss bar tracks the shared **40,800 HP** pool.

- At the jaw, dodge the 8-damage bite and the spread of mouth spikes. If swallowed, walk to the **shroomlight in the far wall** and right-click it. Escape also triggers when the serpent changes phase, dies, or you rejoin after an interruption.
- Along the body, watch for an 8-damage slash or a blast that throws you roughly 20 blocks in open air. Near the tail, sidestep the 4-by-4 stone projectile (5 damage).
- The first defeat splits the armor in a flash of flame. Green spines and orange seams appear along the **2,560-block** second phase. It starts with **40,800 HP**. Fire launches a 9-damage fireball. Poison delivers 16 one-point pulses over 20 seconds. Burst accelerates movement for five seconds and deals zero direct damage. Mystery chooses another available move.
- The final defeat brings the serpent down in a sinking death sequence and drops one **Star of the World Serpent**, a named Nether Star, at the segment that took the finishing blow.

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

Giant bodies follow gently undulating paths through their encounter sites. Summon them in open space to see the silhouette. The server materializes nearby sections in already-ticking chunks, up to 112 blocks from a player for Leviathan and 246 for Jormungandr. Actual visibility also depends on simulation and render distance. Each boss has a five-second introduction and a four-and-a-half-second death sequence. The camera returns to the player’s view afterward. The saved encounter owns health, phase, position, stranded time and pending attacks. Sections can unload and reform when a player travels along the body. The land timer advances while the jaw is active.

### Build and test

The wrapper pins Gradle **8.8**. ForgeGradle **6.0.54** uses official **1.20.1** mappings.

```sh
./gradlew build runGameTestServer
./gradlew runClient
./gradlew runServer
```

The build reobfuscates the installable JAR. The GameTests cover the serpent’s height, both defeats, stomach escape and the final reward. They also check Leviathan’s arm attachment points, charge reach, screech range and stun recovery, water and stranding, and the Windegow’s movement and scratch rules. The dedicated development server uses `run-server/` and requires the server owner’s Minecraft EULA acceptance.
