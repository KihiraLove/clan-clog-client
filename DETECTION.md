# Collection log item detection

This document covers **1,717 catalog entries across 124 pages**, with **1,926 page slots**, shared entries appear on every relevant page.
Each row describes the current client code and acquisition routes.

## Catalog sources, normalisation, and maintenance

- Captured collection log obtained from [Clan Clog catalog/snapshot.json](https://github.com/KihiraLove/Vardorvis-clan-clog/blob/main/catalog/snapshot.json) which collected from [TempleOSRS collection log categories](https://templeosrs.com/api/collection-log/categories.php), captured on **2026-10-08**, source revision `25da8ab34b0e7a6cd5fe38f8615325a49712a5a20918779a4d0ef1f7072e5c34`.
- Page corrections obtained from [Clan Clog `catalog/clue-taxonomy.json`](https://github.com/KihiraLove/Vardorvis-clan-clog/blob/main/catalog/clue-taxonomy.json) based on [OSRS Wiki collection log data](https://oldschool.runescape.wiki/w/Module:Collection_log/data.json?oldid=15347607), OSRS Wiki revision `15347607`: Hard, Elite and Master rare pages replace the retired Gilded, Third Age and Mimic grouping.
- Name/ID reference and source hook inspection obtained from locally cached `net.runelite:runelite-api:1.13.2:sources` and `net.runelite:client:1.13.2:sources`.
- At runtime, [CollectionLogCatalog](src/main/java/com/clanclogclient/CollectionLogCatalog.java) reads the game cache (enum 2102, tab parameter 683, page parameter 690).
- This is a dated inventory, not a claim that a live game cache will always contain exactly these entries.
- Newly added cache entries can be detected without adding rows to the plugin through collection log messages, valuable drop messages, untradeable drop messages, clan broadcasts, and the `Loot Tracker` plugin.
- Detection for new items added to the collection log may need manual maintenance, old items in the collection log may receive new detection methods through the `RuneLite API` or the `Loot Tracker` plugin

## Reading the tables

| Code | Trigger and limits |
| --- | --- |
| N | Personal new collection log chat message or collection log popup. This can only happen once per account and must be emitted. |
| M | Personal Valuable drop or Untradeable drop chat message. Must be emitted. |
| +ID | Name is shared by distinct canonical collection IDs. N or M additionally needs exactly one matching recent ID from inventory/equipment quantity gains or loot stacks. Evidence and unresolved personal messages are retained for three game ticks. |
| L: source | Detected by `Loot Tracker` or other RuneLite loot hook. A submission occurs only if its non player LootReceived event includes this exact collection ID. The plugin dependency is optional, number supported items can change without it. |
| L? | unconfirmed loot hook |
| B | Clan broadcast for recognized collection log, valuable drop, raid special loot or first/repeat pets, only if emitted. |
| Skip shared name | Clan broadcast skipped due to multiple items sharing the same name. |
| U | Unique acquisition game chat message. |
| D | Unique acquisition NPC dialog. |
| C | Context plus item gain correlation. Used for items where multiple detection methods needed at once. |
| No … hook | That acquisition itself has no independent submission trigger. N/M/L can still work if emitted/registered |

Implementation: [event handling and correlation](src/main/java/com/clanclogclient/ClanClogPlugin.java), [message patterns](src/main/java/com/clanclogclient/DropMessageParser.java), [catalog and aliases](src/main/java/com/clanclogclient/CollectionLogCatalog.java), [HTTP submission](src/main/java/com/clanclogclient/SubmissionClient.java).
Specific chat/dialog acquisition patterns are in [RewardMessageParser](src/main/java/com/clanclogclient/RewardMessageParser.java).
The inspected external hooks are from RuneLite API 1.13.2, LootTrackerPlugin and ItemManager.

## Page index

**Bosses**

- [Abyssal Sire](#abyssal-sire)
- [Alchemical Hydra](#alchemical-hydra)
- [Amoxliatl](#amoxliatl)
- [Araxxor](#araxxor)
- [Barrows Chests](#barrows-chests)
- [Brutus](#brutus)
- [Bryophyta](#bryophyta)
- [Callisto and Artio](#callisto-and-artio)
- [Cerberus](#cerberus)
- [Chaos Elemental](#chaos-elemental)
- [Chaos Fanatic](#chaos-fanatic)
- [Commander Zilyana](#commander-zilyana)
- [Corporeal Beast](#corporeal-beast)
- [Crazy Archaeologist](#crazy-archaeologist)
- [Dagannoth Kings](#dagannoth-kings)
- [Deranged Archaeologist](#deranged-archaeologist)
- [Doom of Mokhaiotl](#doom-of-mokhaiotl)
- [Duke Sucellus](#duke-sucellus)
- [The Fight Caves](#the-fight-caves)
- [Fortis Colosseum](#fortis-colosseum)
- [The Gauntlet](#the-gauntlet)
- [General Graardor](#general-graardor)
- [Giant Mole](#giant-mole)
- [Grotesque Guardians](#grotesque-guardians)
- [Hespori](#hespori)
- [Hueycoatl](#hueycoatl)
- [The Inferno](#the-inferno)
- [Kalphite Queen](#kalphite-queen)
- [King Black Dragon](#king-black-dragon)
- [Kraken](#kraken)
- [Kree'arra](#kree-arra)
- [K'ril Tsutsaroth](#kril-tsutsaroth)
- [The Leviathan](#the-leviathan)
- [The Mad Angel](#the-mad-angel)
- [Maggot King](#maggot-king)
- [Moons of Peril](#moons-of-peril)
- [Nex](#nex)
- [The Nightmare](#the-nightmare)
- [Obor](#obor)
- [Phantom Muspah](#phantom-muspah)
- [Royal Titans](#royal-titans)
- [Sarachnis](#sarachnis)
- [Scorpia](#scorpia)
- [Scurrius](#scurrius)
- [Shellbane Gryphon](#shellbane-gryphon)
- [Skotizo](#skotizo)
- [Tempoross](#tempoross)
- [Thermonuclear Smoke Devil](#thermonuclear-smoke-devil)
- [Vardorvis](#vardorvis)
- [Venenatis and Spindel](#venenatis-and-spindel)
- [Vet'ion and Calvar'ion](#vetion-and-calvarion)
- [Vorkath](#vorkath)
- [The Whisperer](#the-whisperer)
- [Wintertodt](#wintertodt)
- [Yama](#yama)
- [Zalcano](#zalcano)
- [Zulrah](#zulrah)

**Raids**

- [Chambers of Xeric](#chambers-of-xeric)
- [Theatre of Blood](#theatre-of-blood)
- [Tombs of Amascut](#tombs-of-amascut)

**Clues**

- [Beginner Treasure Trails](#beginner-treasure-trails)
- [Easy Treasure Trails](#easy-treasure-trails)
- [Medium Treasure Trails](#medium-treasure-trails)
- [Hard Treasure Trails](#hard-treasure-trails)
- [Hard Treasure Trails (Rare)](#hard-treasure-trails-rare)
- [Elite Treasure Trails](#elite-treasure-trails)
- [Elite Treasure Trails (Rare)](#elite-treasure-trails-rare)
- [Master Treasure Trails](#master-treasure-trails)
- [Master Treasure Trails (Rare)](#master-treasure-trails-rare)
- [Shared Treasure Trail Rewards](#shared-treasure-trail-rewards)
- [Scroll Cases](#scroll-cases)

**Minigames**

- [Barbarian Assault](#barbarian-assault)
- [Barracuda Trials](#barracuda-trials)
- [Brimhaven Agility Arena](#brimhaven-agility-arena)
- [Castle Wars](#castle-wars)
- [Fishing Trawler](#fishing-trawler)
- [Giants' Foundry](#giants-foundry)
- [Gnome Restaurant](#gnome-restaurant)
- [Guardians of the Rift](#guardians-of-the-rift)
- [Hallowed Sepulchre](#hallowed-sepulchre)
- [Last Man Standing](#last-man-standing)
- [Magic Training Arena](#magic-training-arena)
- [Mahogany Homes](#mahogany-homes)
- [Pest Control](#pest-control)
- [Mastering Mixology](#mastering-mixology)
- [Rogues' Den](#rogues-den)
- [Shades of Mort'ton](#shades-of-mortton)
- [Soul Wars](#soul-wars)
- [Temple Trekking](#temple-trekking)
- [Tithe Farm](#tithe-farm)
- [Trouble Brewing](#trouble-brewing)
- [Vale Totems](#vale-totems)
- [Volcanic Mine](#volcanic-mine)

**Other**

- [Aerial Fishing](#aerial-fishing)
- [All Pets](#all-pets)
- [Boat Paints](#boat-paints)
- [Camdozaal](#camdozaal)
- [Champion's Challenge](#champions-challenge)
- [Chaos Druids](#chaos-druids)
- [Chompy Bird Hunting](#chompy-bird-hunting)
- [Colossal Wyrm Agility](#colossal-wyrm-agility)
- [Creature Creation](#creature-creation)
- [Cyclopes](#cyclopes)
- [Forestry](#forestry)
- [Fossil Island Notes](#fossil-island-notes)
- [Glough's Experiments](#gloughs-experiments)
- [Hunter Guild](#hunter-guild)
- [Lost Schematics](#lost-schematics)
- [Monkey Backpacks](#monkey-backpacks)
- [Motherlode Mine](#motherlode-mine)
- [My Notes](#my-notes)
- [Ocean Encounters](#ocean-encounters)
- [Random Events](#random-events)
- [Revenants](#revenants)
- [Rooftop Agility](#rooftop-agility)
- [Sailing Miscellaneous](#sailing-miscellaneous)
- [Sea Treasures](#sea-treasures)
- [Shayzien Armour](#shayzien-armour)
- [Shooting Stars](#shooting-stars)
- [Skilling Pets](#skilling-pets)
- [Slayer](#slayer)
- [Tormented Demons](#tormented-demons)
- [TzHaar](#tzhaar)
- [Miscellaneous](#miscellaneous)

## Bosses

<a id="abyssal-sire"></a>

### Abyssal Sire

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Abyssal orphan | 13262 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Unsired | 25624 → 13273 | N, M | L: NPC | B | Explicit ID alias. |
| Abyssal head | 7979 | N, M | L: NPC / Unsired reward | B | - |
| Bludgeon spine | 13274 | N, M | L: Unsired reward | B | - |
| Bludgeon claw | 13275 | N, M | L: Unsired reward | B | - |
| Bludgeon axon | 13276 | N, M | L: Unsired reward | B | - |
| Jar of Miasma | 13277 | N, M | L: Unsired reward | B | - |
| Abyssal dagger | 13265 | N, M | L: NPC / Unsired reward | B | - |
| Abyssal whip | 4151 | N, M | L: NPC / Unsired reward | B | - |

<a id="alchemical-hydra"></a>

### Alchemical Hydra

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Ikkle Hydra | 22746 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Hydra's claw | 22966 | N, M | L: NPC | B | - |
| Hydra tail | 22988 | N, M | L: NPC | B | - |
| Hydra leather | 22983 | N, M | L: NPC | B | - |
| Hydra's fang | 22971 | N, M | L: NPC | B | - |
| Hydra's eye | 22973 | N, M | L: NPC | B | - |
| Hydra's heart | 22969 | N, M | L: NPC | B | - |
| Dragon knife | 22804 | N, M | L: NPC | B | - |
| Dragon thrownaxe | 20849 | N, M | L: NPC | B | - |
| Jar of Chemicals | 23064 | N, M | L: NPC | B | - |
| Alchemical hydra heads | 23077 | N, M | L: NPC | B | - |

<a id="amoxliatl"></a>

### Amoxliatl

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Moxi | 30154 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Glacial temotli | 29889 | N, M | L: NPC | B | - |
| Pendant of Ates (inert) | 29892 | N, M | L: NPC | B | - |
| Frozen tear | 29895 | N, M | L: NPC | B | - |

<a id="araxxor"></a>

### Araxxor

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Nid | 29836 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Araxyte venom sack | 29784 | N, M | L: NPC | B | Cache name: Araxyte venom sac. |
| Spider cave teleport | 29782 | N, M | L: NPC | B | - |
| Araxyte fang | 29799 | N, M | L: NPC | B | - |
| Noxious point | 29790 | N, M | L: NPC | B | - |
| Noxious blade | 29792 | N, M | L: NPC | B | - |
| Noxious pommel | 29794 | N, M | L: NPC | B | - |
| Araxyte head | 29788 | N, M | L: NPC | B | - |
| Jar of Venom | 29786 | N, M | L: NPC | B | - |
| Coagulated venom | 29781 | N, M | L: NPC | B | - |

<a id="barrows-chests"></a>

### Barrows Chests

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Karil's coif | 4732 | N, M | L: Barrows reward interface | B | - |
| Ahrim's hood | 4708 | N, M | L: Barrows reward interface | B | - |
| Dharok's helm | 4716 | N, M | L: Barrows reward interface | B | - |
| Guthan's helm | 4724 | N, M | L: Barrows reward interface | B | - |
| Torag's helm | 4745 | N, M | L: Barrows reward interface | B | - |
| Verac's helm | 4753 | N, M | L: Barrows reward interface | B | - |
| Karil's leathertop | 4736 | N, M | L: Barrows reward interface | B | - |
| Ahrim's robetop | 4712 | N, M | L: Barrows reward interface | B | - |
| Dharok's platebody | 4720 | N, M | L: Barrows reward interface | B | - |
| Guthan's platebody | 4728 | N, M | L: Barrows reward interface | B | - |
| Torag's platebody | 4749 | N, M | L: Barrows reward interface | B | - |
| Verac's brassard | 4757 | N, M | L: Barrows reward interface | B | - |
| Karil's leatherskirt | 4738 | N, M | L: Barrows reward interface | B | - |
| Ahrim's robeskirt | 4714 | N, M | L: Barrows reward interface | B | - |
| Dharok's platelegs | 4722 | N, M | L: Barrows reward interface | B | - |
| Guthan's chainskirt | 4730 | N, M | L: Barrows reward interface | B | - |
| Torag's platelegs | 4751 | N, M | L: Barrows reward interface | B | - |
| Verac's plateskirt | 4759 | N, M | L: Barrows reward interface | B | - |
| Karil's crossbow | 4734 | N, M | L: Barrows reward interface | B | - |
| Ahrim's staff | 4710 | N, M | L: Barrows reward interface | B | - |
| Dharok's greataxe | 4718 | N, M | L: Barrows reward interface | B | - |
| Guthan's warspear | 4726 | N, M | L: Barrows reward interface | B | - |
| Torag's hammers | 4747 | N, M | L: Barrows reward interface | B | - |
| Verac's flail | 4755 | N, M | L: Barrows reward interface | B | - |
| Bolt rack | 4740 | N, M | L: Barrows reward interface | B | - |

<a id="brutus"></a>

### Brutus

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Beef | 33124 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Mooleta | 33101 | N, M | L: NPC | B | - |
| Bottomless milk bucket (empty) | 33091 | N, M | L: NPC | B | - |
| Cow slippers | 33093 | N, M | L: NPC | B | - |

<a id="bryophyta"></a>

### Bryophyta

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Bryophyta's essence | 22372 | N, M | L: NPC | B | - |

<a id="callisto-and-artio"></a>

### Callisto and Artio

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Callisto cub | 13178 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Tyrannical ring | 12603 | N, M | L: NPC | B | - |
| Dragon pickaxe | 11920 | N, M | L: NPC | B | - |
| Dragon 2h sword | 7158 | N, M | L: NPC | B | - |
| Claws of Callisto | 27667 | N, M | L: NPC | B | - |
| Voidwaker hilt | 27681 | N, M | L: NPC | B | - |

<a id="cerberus"></a>

### Cerberus

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Hellpuppy | 13247 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Eternal crystal | 13227 | N, M | L: NPC | B | - |
| Pegasian crystal | 13229 | N, M | L: NPC | B | - |
| Primordial crystal | 13231 | N, M | L: NPC | B | - |
| Jar of Souls | 13245 | N, M | L: NPC | B | - |
| Smouldering stone | 13233 | N, M | L: NPC | B | - |
| Key master teleport | 13249 | N, M | L: NPC | B | - |

<a id="chaos-elemental"></a>

### Chaos Elemental

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Pet Chaos Elemental | 11995 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Dragon pickaxe | 11920 | N, M | L: NPC | B | - |
| Dragon 2h sword | 7158 | N, M | L: NPC | B | - |

<a id="chaos-fanatic"></a>

### Chaos Fanatic

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Pet Chaos Elemental | 11995 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Odium shard 1 | 11928 | N, M | L: NPC | B | - |
| Malediction shard 1 | 11931 | N, M | L: NPC | B | - |

<a id="commander-zilyana"></a>

### Commander Zilyana

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Pet Zilyana | 12651 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Armadyl crossbow | 11785 | N, M | L: NPC | B | - |
| Saradomin hilt | 11814 | N, M | L: NPC | B | - |
| Saradomin sword | 11838 | N, M | L: NPC | B | - |
| Saradomin's light | 13256 | N, M | L: NPC | B | - |
| Godsword shard 1 | 11818 | N, M | L: NPC | B | - |
| Godsword shard 2 | 11820 | N, M | L: NPC | B | - |
| Godsword shard 3 | 11822 | N, M | L: NPC | B | - |

<a id="corporeal-beast"></a>

### Corporeal Beast

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Pet dark core | 12816 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Elysian sigil | 12819 | N, M | L: NPC | B | - |
| Spectral sigil | 12823 | N, M | L: NPC | B | - |
| Arcane sigil | 12827 | N, M | L: NPC | B | - |
| Holy elixir | 12833 | N, M | L: NPC | B | - |
| Spirit shield | 12829 | N, M | L: NPC | B | - |
| Jar of Spirits | 25521 | N, M | L: NPC | B | - |

<a id="crazy-archaeologist"></a>

### Crazy Archaeologist

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Odium shard 2 | 11929 | N, M | L: NPC | B | - |
| Malediction shard 2 | 11932 | N, M | L: NPC | B | - |
| Fedora | 11990 | N, M | L: NPC | B | - |

<a id="dagannoth-kings"></a>

### Dagannoth Kings

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Pet Dagannoth Prime | 12644 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Pet Dagannoth Supreme | 12643 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Pet Dagannoth Rex | 12645 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Berserker ring | 6737 | N, M | L: NPC | B | - |
| Archers ring | 6733 | N, M | L: NPC | B | - |
| Seers ring | 6731 | N, M | L: NPC | B | - |
| Warrior ring | 6735 | N, M | L: NPC | B | - |
| Dragon axe | 6739 | N, M | L: NPC | B | - |
| Seercull | 6724 | N, M | L: NPC | B | - |
| Mud battlestaff | 6562 | N, M | L: NPC | B | - |

<a id="deranged-archaeologist"></a>

### Deranged Archaeologist

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Steel ring | 30895 | N, M | L: NPC | B | - |

<a id="doom-of-mokhaiotl"></a>

### Doom of Mokhaiotl

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Dom | 31130 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Avernic treads | 31088 | N, M | L: Doom loot claim | B | - |
| Eye of Ayak (uncharged) | 31115 | N, M | L: Doom loot claim | B | - |
| Mokhaiotl cloth | 31109 | N, M | L: Doom loot claim | B | - |
| Mokhaiotl waystone | 31099 | N, M | L: Doom loot claim | B | - |
| Demon tear | 31111 | N, M | L: Doom loot claim | B | - |

<a id="duke-sucellus"></a>

### Duke Sucellus

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Baron | 28250 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Eye of the Duke | 28321 | N, M | L: NPC | B | - |
| Virtus mask | 26241 | N, M | L: NPC | B | - |
| Virtus robe top | 26243 | N, M | L: NPC | B | - |
| Virtus robe bottom | 26245 | N, M | L: NPC | B | - |
| Magus vestige | 28281 | N, M | L: NPC | B | - |
| Ice quartz | 28270 | N, M | L: NPC | B | - |
| Frozen tablet | 28333 | N, M | L: NPC | B | - |
| Chromium ingot | 28276 | N, M | L: NPC | B | - |
| Awakener's orb | 28334 | N, M | L: NPC | B | - |

<a id="the-fight-caves"></a>

### The Fight Caves

TODO: invetigate if cape parsing is possible

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Tzrek-Jad | 13225 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Fire cape | 6570 | N, M | - | B | - |

<a id="fortis-colosseum"></a>

### Fortis Colosseum

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Smol Heredit | 28960 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Dizana's quiver (uncharged) | 28947 | N, M | L: Colosseum reward interface | B | - |
| Sunfire fanatic cuirass | 28936 | N, M | L: Colosseum reward interface | B | - |
| Sunfire fanatic chausses | 28939 | N, M | L: Colosseum reward interface | B | - |
| Sunfire fanatic helm | 28933 | N, M | L: Colosseum reward interface | B | - |
| Echo crystal | 28942 | N, M | L: Colosseum reward interface | B | - |
| Tonalztics of Ralos (uncharged) | 28919 | N, M | L: Colosseum reward interface | B | - |
| Sunfire splinters | 28924 | N, M | L: Colosseum reward interface | B | - |
| Uncut onyx | 6571 | N, M | L: Colosseum reward interface | B | - |

<a id="the-gauntlet"></a>

### The Gauntlet

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Youngllef | 23757 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Crystal armour seed | 23956 | N, M, U | L: Gauntlet reward | B |- |
| Crystal weapon seed | 4207 | N, M, U | L: Gauntlet reward | B |- |
| Enhanced crystal weapon seed | 25859 | N, M, U | L: Gauntlet reward | B |- |
| Gauntlet cape | 23859 | N, U | L: Gauntlet reward | B |-  |

<a id="general-graardor"></a>

### General Graardor

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Pet general Graardor | 12650 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Bandos chestplate | 11832 | N, M | L: NPC | B | - |
| Bandos tassets | 11834 | N, M | L: NPC | B | - |
| Bandos boots | 11836 | N, M | L: NPC | B | - |
| Bandos hilt | 11812 | N, M | L: NPC | B | - |
| Godsword shard 1 | 11818 | N, M | L: NPC | B | - |
| Godsword shard 2 | 11820 | N, M | L: NPC | B | - |
| Godsword shard 3 | 11822 | N, M | L: NPC | B | - |

<a id="giant-mole"></a>

### Giant Mole

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Baby Mole | 12646 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Mole skin | 7418 | N, M | L: NPC | B | - |
| Mole claw | 7416 | N, M | L: NPC | B | - |
| Immaculate mole skin | 33382 | N, M | L: NPC | B | - |

<a id="grotesque-guardians"></a>

### Grotesque Guardians

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Noon | 21748 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Black tourmaline core | 21730 | N, M | L: NPC | B | - |
| Granite gloves | 21736 | N, M | L: NPC | B | - |
| Granite ring | 21739 | N, M | L: NPC | B | - |
| Granite hammer | 21742 | N, M | L: NPC | B | - |
| Jar of Stone | 21745 | N, M | L: NPC | B | - |
| Granite dust | 21726 | N, M | L: NPC | B | - |

<a id="hespori"></a>

### Hespori

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Bottomless compost bucket | 22994 | N, M | L: NPC | B | - |
| Iasor seed | 22883 | N, M | L: NPC | B | - |
| Kronos seed | 22885 | N, M | L: NPC | B | - |
| Attas seed | 22881 | N, M | L: NPC | B | - |

<a id="hueycoatl"></a>

### Hueycoatl

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Huberte | 30152 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Dragon hunter wand | 30070 | N, M | L: NPC | B | - |
| Tome of Earth (empty) | 30066 | N, M | L: NPC | B | - |
| Soiled page | 30068 | N, M | L: NPC | B | - |
| Hueycoatl hide | 30085 | N, M | L: NPC | B | - |
| Huasca seed | 30088 | N, M | L: NPC | B | - |

<a id="the-inferno"></a>

### The Inferno

TODO: investigate if obtain can be parsed for cape

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Jal-Nib-Rek | 21291 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Infernal cape | 21295 | N, M | - | B | - |

<a id="kalphite-queen"></a>

### Kalphite Queen

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Kalphite Princess | 12647 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Kq head | 7981 | N, M | L: NPC | B | - |
| Jar of Sand | 12885 | N, M | L: NPC | B | - |
| Dragon 2h sword | 7158 | N, M | L: NPC | B | - |
| Dragon chainbody | 3140 | N, M | L: NPC | B | - |
| Dragon pickaxe | 11920 | N, M | L: NPC | B | - |

<a id="king-black-dragon"></a>

### King Black Dragon

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Prince Black Dragon | 12653 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Kbd heads | 7980 | N, M | L: NPC | B | - |
| Dragon pickaxe | 11920 | N, M | L: NPC | B | - |
| Draconic visage | 11286 | N, M | L: NPC | B | - |

<a id="kraken"></a>

### Kraken

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Pet Kraken | 12655 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Kraken tentacle | 12004 | N, M | L: NPC | B | - |
| Trident of the Seas (full) | 11905 | N, M | L: NPC | B | - |
| Jar of Dirt | 12007 | N, M | L: NPC | B | - |

<a id="kree-arra"></a>

### Kree'arra

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Pet Kree'arra | 12649 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Armadyl helmet | 11826 | N, M | L: NPC | B | - |
| Armadyl chestplate | 11828 | N, M | L: NPC | B | - |
| Armadyl chainskirt | 11830 | N, M | L: NPC | B | - |
| Armadyl hilt | 11810 | N, M | L: NPC | B | - |
| Godsword shard 1 | 11818 | N, M | L: NPC | B | - |
| Godsword shard 2 | 11820 | N, M | L: NPC | B | - |
| Godsword shard 3 | 11822 | N, M | L: NPC | B | - |

<a id="kril-tsutsaroth"></a>

### K'ril Tsutsaroth

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Pet K'ril Tsutsaroth | 12652 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Staff of the Dead | 11791 | N, M | L: NPC | B | - |
| Zamorakian spear | 11824 | N, M | L: NPC | B | - |
| Steam battlestaff | 11787 | N, M | L: NPC | B | - |
| Zamorak hilt | 11816 | N, M | L: NPC | B | - |
| Godsword shard 1 | 11818 | N, M | L: NPC | B | - |
| Godsword shard 2 | 11820 | N, M | L: NPC | B | - |
| Godsword shard 3 | 11822 | N, M | L: NPC | B | - |

<a id="the-leviathan"></a>

### The Leviathan

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Lil'viathan | 28252 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Leviathan's lure | 28325 | N, M | L: NPC | B | - |
| Virtus mask | 26241 | N, M | L: NPC | B | - |
| Virtus robe top | 26243 | N, M | L: NPC | B | - |
| Virtus robe bottom | 26245 | N, M | L: NPC | B | - |
| Venator vestige | 28283 | N, M | L: NPC | B | - |
| Smoke quartz | 28274 | N, M | L: NPC | B | - |
| Scarred tablet | 28332 | N, M | L: NPC | B | - |
| Chromium ingot | 28276 | N, M | L: NPC | B | - |
| Awakener's orb | 28334 | N, M | L: NPC | B | - |

<a id="the-mad-angel"></a>

### The Mad Angel

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Aggy | 34042 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Hallowfell | 34027 | N, M | L: NPC | B | - |
| Ardeaglais teleport | 34033 | N, M | L: NPC | B | - |
| Granite dust | 21726 | N, M | L: NPC | B | - |
| Jar of Light | 34030 | N, M | L: NPC | B | - |

<a id="maggot-king"></a>

### Maggot King

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Maggot marquess | 33642 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Crimson kisten | 33631 | N, M | L: Maggot King reward | B |- |
| Elder venator fang | 33634 | N, M | L: Maggot King reward | B |- |

<a id="moons-of-peril"></a>

### Moons of Peril

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Eclipse Moon chestplate | 29004 | N, M | L: Lunar Chest | B | - |
| Eclipse Moon tassets | 29007 | N, M | L: Lunar Chest | B | - |
| Eclipse Moon helm | 29010 | N, M | L: Lunar Chest | B | - |
| Eclipse atlatl | 29000 | N, M | L: Lunar Chest | B | - |
| Blue Moon chestplate | 29013 | N, M | L: Lunar Chest | B | - |
| Blue Moon tassets | 29016 | N, M | L: Lunar Chest | B | - |
| Blue Moon helm | 29019 | N, M | L: Lunar Chest | B | - |
| Blue Moon spear | 28988 | N, M | L: Lunar Chest | B | - |
| Blood Moon chestplate | 29022 | N, M | L: Lunar Chest | B | - |
| Blood Moon tassets | 29025 | N, M | L: Lunar Chest | B | - |
| Blood Moon helm | 29028 | N, M | L: Lunar Chest | B | - |
| Dual macuahuitl | 28997 | N, M | L: Lunar Chest | B | - |
| Atlatl dart | 28991 | N, M | L: Lunar Chest | B | - |

<a id="nex"></a>

### Nex

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Nexling | 26348 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Ancient hilt | 26370 | N, M | L: NPC | B | - |
| Nihil horn | 26372 | N, M | L: NPC | B | - |
| Zaryte vambraces | 26235 | N, M | L: NPC | B | - |
| Torva full helm (damaged) | 26376 | N, M | L: NPC | B | - |
| Torva platebody (damaged) | 26378 | N, M | L: NPC | B | - |
| Torva platelegs (damaged) | 26380 | N, M | L: NPC | B | - |
| Nihil shard | 26231 | N, M | L: NPC | B | - |

<a id="the-nightmare"></a>

### The Nightmare

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Little Nightmare | 24491 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Inquisitor's mace | 24417 | N, M | L: NPC | B | - |
| Inquisitor's great helm | 24419 | N, M | L: NPC | B | - |
| Inquisitor's hauberk | 24420 | N, M | L: NPC | B | - |
| Inquisitor's plateskirt | 24421 | N, M | L: NPC | B | - |
| Nightmare staff | 24422 | N, M | L: NPC | B | - |
| Volatile orb | 24514 | N, M | L: NPC | B | - |
| Harmonised orb | 24511 | N, M | L: NPC | B | - |
| Eldritch orb | 24517 | N, M | L: NPC | B | - |
| Jar of Dreams | 24495 | N, M | L: NPC | B | - |
| Slepey tablet | 25837 | N, M | L: NPC | B | - |
| Parasitic egg | 25838 | N, M | L: NPC | B | - |

<a id="obor"></a>

### Obor

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Hill giant club | 20756 | N, M | L: NPC | B | - |

<a id="phantom-muspah"></a>

### Phantom Muspah

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Muphin | 27590 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Venator shard | 27614 | N, M | L: NPC | B | - |
| Ancient icon | 27627 | N, M | L: NPC | B | - |
| Charged ice | 27643 | N, M | L: NPC | B | - |
| Frozen cache | 27622 | N, M | L: NPC | B | - |
| Ancient essence | 27616 | N, M | L: NPC | B | - |

<a id="royal-titans"></a>

### Royal Titans

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Bran | 30622 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Deadeye prayer scroll | 30626 | N, M | L: NPC | B | - |
| Mystic vigour prayer scroll | 30627 | N, M | L: NPC | B | - |
| Giantsoul amulet (uncharged) | 30637 | N, M | L: NPC | B | - |
| Ice element staff crown | 30628 | N, M | L: NPC | B | - |
| Fire element staff crown | 30631 | N, M | L: NPC | B | - |
| Desiccated page | 30640 | N, M | L: NPC | B | - |

<a id="sarachnis"></a>

### Sarachnis

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Sraracha | 23495 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Jar of Eyes | 23525 | N, M | L: NPC | B | - |
| Giant egg sac(full) | 23517 | N, M | L: NPC | B | - |
| Sarachnis cudgel | 23528 | N, M | L: NPC | B | - |
| Pristine spider silk | 33133 | N, M | L: NPC | B | - |

<a id="scorpia"></a>

### Scorpia

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Scorpia's offspring | 13181 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Odium shard 3 | 11930 | N, M | L: NPC | B | - |
| Malediction shard 3 | 11933 | N, M | L: NPC | B | - |
| Dragon 2h sword | 7158 | N, M | L: NPC | B | - |

<a id="scurrius"></a>

### Scurrius

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Scurry | 28801 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Scurrius' spine | 28798 | N, M | L: NPC | B | - |

<a id="shellbane-gryphon"></a>

### Shellbane Gryphon

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Gull | 31285 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Belle's folly (tarnished) | 31245 | N, M | L: NPC | B | - |
| Gryphon feather | 31235 | N, M | L: NPC | B | - |
| Jar of Feathers | 32921 | N, M | L: NPC | B | - |

<a id="skotizo"></a>

### Skotizo

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Skotos | 21273 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Jar of Darkness | 19701 | N, M | L: NPC | B | - |
| Dark claw | 21275 | N, M | L: NPC | B | - |
| Dark totem | 19685 | N, M | L: NPC | B | - |
| Uncut onyx | 6571 | N, M | L: NPC | B | - |
| Ancient shard | 19677 | N, M | L: NPC | B | - |

<a id="tempoross"></a>

### Tempoross

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Tiny tempor | 25602 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Big harpoonfish | 25559 | N, M | L: reward pool / casket | B | - |
| Spirit angler headband | 25592 | N, M, C | No conversion loot hook | B | Prompt plus output gain, original piece removed and 1,200-flake spent. |
| Spirit angler top | 25594 | N, M, C | No conversion loot hook | B | Prompt plus output gain, original piece removed and 1,200-flake spent. |
| Spirit angler waders | 25596 | N, M, C | No conversion loot hook | B | Prompt plus output gain, original piece removed and 1,200-flake spent. |
| Spirit angler boots | 25598 | N, M, C | No conversion loot hook | B | Prompt plus output gain, original piece removed and 1,200-flake spent. |
| Tome of Water (empty) | 25576 | N, M | L: reward pool / casket | B | - |
| Soaked page | 25578 | N, M | L: reward pool / casket | B | - |
| Tackle box | 25580 | N, M | L: reward pool / casket | B | - |
| Fish barrel | 25582 | N, M | L: reward pool / casket | B | - |
| Dragon harpoon | 21028 | N, M | L: reward pool / casket | B | - |
| Spirit flakes | 25588 | N, M | L: reward pool / casket | B | - |

<a id="thermonuclear-smoke-devil"></a>

### Thermonuclear Smoke Devil

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Pet Smoke Devil | 12648 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Occult necklace | 12002 | N, M | L: NPC | B | - |
| Smoke battlestaff | 11998 | N, M | L: NPC | B | - |
| Dragon chainbody | 3140 | N, M | L: NPC | B | - |
| Jar of Smoke | 25524 | N, M | L: NPC | B | - |

<a id="vardorvis"></a>

### Vardorvis

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Butch | 28248 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Executioner's axe head | 28319 | N, M | L: NPC | B | - |
| Virtus mask | 26241 | N, M | L: NPC | B | - |
| Virtus robe top | 26243 | N, M | L: NPC | B | - |
| Virtus robe bottom | 26245 | N, M | L: NPC | B | - |
| Ultor vestige | 28285 | N, M | L: NPC | B | - |
| Blood quartz | 28268 | N, M | L: NPC | B | - |
| Strangled tablet | 28330 | N, M | L: NPC | B | - |
| Chromium ingot | 28276 | N, M | L: NPC | B | - |
| Awakener's orb | 28334 | N, M | L: NPC | B | - |

<a id="venenatis-and-spindel"></a>

### Venenatis and Spindel

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Venenatis spiderling | 13177 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Treasonous ring | 12605 | N, M | L: NPC | B | - |
| Dragon pickaxe | 11920 | N, M | L: NPC | B | - |
| Dragon 2h sword | 7158 | N, M | L: NPC | B | - |
| Fangs of Venenatis | 27670 | N, M | L: NPC | B | - |
| Voidwaker gem | 27687 | N, M | L: NPC | B | - |

<a id="vetion-and-calvarion"></a>

### Vet'ion and Calvar'ion

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Vet'ion Jr. | 13179 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Ring of the gods | 12601 | N, M | L: NPC | B | - |
| Dragon pickaxe | 11920 | N, M | L: NPC | B | - |
| Dragon 2h sword | 7158 | N, M | L: NPC | B | - |
| Skull of Vet'ion | 27673 | N, M | L: NPC | B | - |
| Voidwaker blade | 27684 | N, M | L: NPC | B | - |

<a id="vorkath"></a>

### Vorkath

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Vorki | 21992 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Vorkath's head | 21907 | N, M | L: NPC | B | - |
| Draconic visage | 11286 | N, M | L: NPC | B | - |
| Skeletal visage | 22006 | N, M | L: NPC | B | - |
| Jar of Decay | 22106 | N, M | L: NPC | B | - |
| Dragonbone necklace | 22111 | N, M | L: NPC | B | - |

<a id="the-whisperer"></a>

### The Whisperer

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Wisp | 28246 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Siren's staff | 28323 | N, M | L: NPC | B | - |
| Virtus mask | 26241 | N, M | L: NPC | B | - |
| Virtus robe top | 26243 | N, M | L: NPC | B | - |
| Virtus robe bottom | 26245 | N, M | L: NPC | B | - |
| Bellator vestige | 28279 | N, M | L: NPC | B | - |
| Shadow quartz | 28272 | N, M | L: NPC | B | - |
| Sirenic tablet | 28331 | N, M | L: NPC | B | - |
| Chromium ingot | 28276 | N, M | L: NPC | B | - |
| Awakener's orb | 28334 | N, M | L: NPC | B | - |

<a id="wintertodt"></a>

### Wintertodt

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Phoenix | 20693 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Tome of Fire (empty) | 20716 | N, M | L: reward cart / crate | B | - |
| Burnt page | 20718 | N, M | L: reward cart / crate | B | - |
| Pyromancer garb | 20704 | N, M | L: reward cart / crate | B | - |
| Pyromancer hood | 20708 | N, M | L: reward cart / crate | B | - |
| Pyromancer robe | 20706 | N, M | L: reward cart / crate | B | - |
| Pyromancer boots | 20710 | N, M | L: reward cart / crate | B | - |
| Warm gloves | 20712 | N, M | L: reward cart / crate | B | - |
| Bruma torch | 20720 | N, M | L: reward cart / crate | B | - |
| Dragon axe | 6739 | N, M | L: reward cart / crate | B | - |

<a id="yama"></a>

### Yama

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Yami | 30888 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Chasm teleport scroll | 30775 | N, M | L: NPC / dossier | B | - |
| Oathplate helm | 30750 | N, M | L: NPC / forgotten lockbox | B | - |
| Oathplate chest | 30753 | N, M | L: NPC / forgotten lockbox | B | - |
| Oathplate legs | 30756 | N, M | L: NPC / forgotten lockbox | B | - |
| Soulflame horn | 30759 | N, M | L: NPC | B | - |
| Oathplate shards | 30765 | N, M | L: NPC / forgotten lockbox | B | - |
| Rite of vile transference | 30806 | N | L: dossier | B |  |
| Forgotten lockbox | 30763 | N, M | L: NPC | B | - |
| Dossier | 30805 → 30803 | N, M | L: NPC | B | Explicit ID alias. |
| Barrel of demonic tallow (full) | 30795 | N, M | L: NPC | B | - |

<a id="zalcano"></a>

### Zalcano

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Smolcano | 23760 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Crystal tool seed | 23953 | N, M | L: NPC | B | - |
| Zalcano shard | 23908 | N, M | L: NPC | B | - |
| Uncut onyx | 6571 | N, M | L: NPC | B | - |

<a id="zulrah"></a>

### Zulrah

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Pet Snakeling | 12921 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Tanzanite mutagen | 13200 | N, M | L: NPC | B | - |
| Magma mutagen | 13201 | N, M | L: NPC | B | - |
| Jar of Swamp | 12936 | N, M | L: NPC | B | - |
| Magic fang | 12932 | N, M | L: NPC | B | - |
| Serpentine visage | 12927 | N, M | L: NPC | B | - |
| Tanzanite fang | 12922 | N, M | L: NPC | B | - |
| Zul-andra teleport | 12938 | N, M | L: NPC | B | - |
| Uncut onyx | 6571 | N, M | L: NPC | B | - |
| Zulrah's scales | 12934 | N, M | L: NPC | B | - |

## Raids

<a id="chambers-of-xeric"></a>

### Chambers of Xeric

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Olmlet | 20851 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Metamorphic dust | 22386 | N, M | L: Chambers of Xeric reward interface | B | - |
| Twisted bow | 20997 | N, M | L: Chambers of Xeric reward interface | B | - |
| Elder maul | 21003 | N, M | L: Chambers of Xeric reward interface | B | - |
| Kodai insignia | 21043 | N, M | L: Chambers of Xeric reward interface | B | - |
| Dragon claws | 13652 | N, M | L: Chambers of Xeric reward interface | B | - |
| Ancestral hat | 21018 | N, M | L: Chambers of Xeric reward interface | B | - |
| Ancestral robe top | 21021 | N, M | L: Chambers of Xeric reward interface | B | - |
| Ancestral robe bottom | 21024 | N, M | L: Chambers of Xeric reward interface | B | - |
| Dinh's bulwark | 21015 | N, M | L: Chambers of Xeric reward interface | B | - |
| Dexterous prayer scroll | 21034 | N, M | L: Chambers of Xeric reward interface | B | - |
| Arcane prayer scroll | 21079 | N, M | L: Chambers of Xeric reward interface | B | - |
| Dragon hunter crossbow | 21012 | N, M | L: Chambers of Xeric reward interface | B | - |
| Twisted buckler | 21000 | N, M | L: Chambers of Xeric reward interface | B | - |
| Torn prayer scroll | 21047 | N, M | L: Chambers of Xeric reward interface | B | - |
| Dark relic | 21027 | N, M | L: Chambers of Xeric reward interface | B | - |
| Onyx | 6573 | N, M | L: Chambers of Xeric reward interface | B | - |
| Twisted ancestral colour kit | 24670 | N, M | L: Chambers of Xeric reward interface | B | - |
| Xeric's guard | 22388 | N, M | L: Chambers of Xeric reward interface | B | - |
| Xeric's warrior | 22390 | N, M | L: Chambers of Xeric reward interface | B | - |
| Xeric's sentinel | 22392 | N, M | L: Chambers of Xeric reward interface | B | - |
| Xeric's general | 22394 | N, M | L: Chambers of Xeric reward interface | B | - |
| Xeric's champion | 22396 | N, M | L: Chambers of Xeric reward interface | B | - |

<a id="theatre-of-blood"></a>

### Theatre of Blood

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Lil' Zik | 22473 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Scythe of Vitur (uncharged) | 22486 | N, M | L: Theatre of Blood reward interface | B | - |
| Ghrazi rapier | 22324 | N, M | L: Theatre of Blood reward interface | B | - |
| Sanguinesti staff (uncharged) | 22481 | N, M | L: Theatre of Blood reward interface | B | - |
| Justiciar faceguard | 22326 | N, M | L: Theatre of Blood reward interface | B | - |
| Justiciar chestguard | 22327 | N, M | L: Theatre of Blood reward interface | B | - |
| Justiciar legguards | 22328 | N, M | L: Theatre of Blood reward interface | B | - |
| Avernic defender hilt | 22477 | N, M | L: Theatre of Blood reward interface | B | - |
| Vial of blood | 22446 | N, M | L: Theatre of Blood reward interface | B | - |
| Sinhaza shroud tier 1 | 22494 | N, M | L: Theatre of Blood reward interface | B | - |
| Sinhaza shroud tier 2 | 22496 | N, M | L: Theatre of Blood reward interface | B | - |
| Sinhaza shroud tier 3 | 22498 | N, M | L: Theatre of Blood reward interface | B | - |
| Sinhaza shroud tier 4 | 22500 | N, M | L: Theatre of Blood reward interface | B | - |
| Sinhaza shroud tier 5 | 22502 | N, M | L: Theatre of Blood reward interface | B | - |
| Sanguine dust | 25746 | N, M | L: Theatre of Blood reward interface | B | - |
| Holy ornament kit | 25742 | N, M | L: Theatre of Blood reward interface | B | - |
| Sanguine ornament kit | 25744 | N, M | L: Theatre of Blood reward interface | B | - |

<a id="tombs-of-amascut"></a>

### Tombs of Amascut

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Tumeken's guardian | 27352 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Tumeken's shadow (uncharged) | 27277 | N, M | L: Tombs of Amascut reward interface | B | - |
| Elidinis' ward | 25985 | N, M | L: Tombs of Amascut reward interface | B | - |
| Masori mask | 27226 | N, M | L: Tombs of Amascut reward interface | B | - |
| Masori body | 27229 | N, M | L: Tombs of Amascut reward interface | B | - |
| Masori chaps | 27232 | N, M | L: Tombs of Amascut reward interface | B | - |
| Lightbearer | 25975 | N, M | L: Tombs of Amascut reward interface | B | - |
| Osmumten's fang | 26219 | N, M | L: Tombs of Amascut reward interface | B | - |
| Thread of Elidinis | 27279 | N, M | L: Tombs of Amascut reward interface | B | - |
| Breach of the Scarab | 27283 | N, M | L: Tombs of Amascut reward interface | B | - |
| Eye of the Corruptor | 27285 | N, M | L: Tombs of Amascut reward interface | B | - |
| Jewel of the Sun | 27289 | N, M | L: Tombs of Amascut reward interface | B | - |
| Jewel of Amascut | 30893 | N, M | L: Tombs of Amascut reward interface | B | - |
| Menaphite ornament kit | 27255 | N, M | L: Tombs of Amascut reward interface | B | - |
| Cursed phalanx | 27248 | N, M | L: Tombs of Amascut reward interface | B | - |
| Masori crafting kit | 27372 | N, M | L: Tombs of Amascut reward interface | B | - |
| Cache of runes | 27293 | N, M | L: Tombs of Amascut reward interface | B | - |
| Icthlarin's shroud (tier 1) | 27257 | N, M | L: Tombs of Amascut reward interface | B | - |
| Icthlarin's shroud (tier 2) | 27259 | N, M | L: Tombs of Amascut reward interface | B | - |
| Icthlarin's shroud (tier 3) | 27261 | N, M | L: Tombs of Amascut reward interface | B | - |
| Icthlarin's shroud (tier 4) | 27263 | N, M | L: Tombs of Amascut reward interface | B | - |
| Icthlarin's shroud (tier 5) | 27265 | N, M | L: Tombs of Amascut reward interface | B | - |
| Remnant of Akkha | 27377 | N, M | L: Tombs of Amascut reward interface | B | - |
| Remnant of Ba-Ba | 27378 | N, M | L: Tombs of Amascut reward interface | B | - |
| Remnant of Kephri | 27379 | N, M | L: Tombs of Amascut reward interface | B | - |
| Remnant of Zebak | 27380 | N, M | L: Tombs of Amascut reward interface | B | - |
| Ancient remnant | 27381 | N, M | L: Tombs of Amascut reward interface | B | - |

## Clues

<a id="beginner-treasure-trails"></a>

### Beginner Treasure Trails

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Mole slippers | 23285 | N, M | L: clue reward | B | - |
| Frog slippers | 23288 | N, M | L: clue reward | B | - |
| Bear feet | 23291 | N, M | L: clue reward | B | - |
| Demon feet | 23294 | N, M | L: clue reward | B | - |
| Jester cape | 23297 | N, M | L: clue reward | B | - |
| Shoulder parrot | 23300 | N, M | L: clue reward | B | - |
| Monk's robe top (t) | 23303 | N, M | L: clue reward | B | - |
| Monk's robe (t) | 23306 | N, M | L: clue reward | B | - |
| Amulet of defence (t) | 23309 | N, M | L: clue reward | B | - |
| Sandwich lady hat | 23312 | N, M | L: clue reward | B | - |
| Sandwich lady top | 23315 | N, M | L: clue reward | B | - |
| Sandwich lady bottom | 23318 | N, M | L: clue reward | B | - |
| Rune scimitar ornament kit (guthix) | 23321 | N, M | L: clue reward | B | - |
| Rune scimitar ornament kit (saradomin) | 23324 | N, M | L: clue reward | B | - |
| Rune scimitar ornament kit (zamorak) | 23327 | N, M | L: clue reward | B | - |
| Black pickaxe | 12297 | N, M | L: clue reward | B | - |

<a id="easy-treasure-trails"></a>

### Easy Treasure Trails

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Team cape zero | 20211 | N, M | L: clue reward | B | - |
| Team cape i | 20217 | N, M | L: clue reward | B | - |
| Team cape x | 20214 | N, M | L: clue reward | B | - |
| Cape of skulls | 23351 | N, M | L: clue reward | B | - |
| Golden chef's hat | 20205 | N, M | L: clue reward | B | - |
| Golden apron | 20208 | N, M | L: clue reward | B | - |
| Wooden shield (g) | 20166 | N, M | L: clue reward | B | - |
| Black full helm (t) | 2587 | N, M | L: clue reward | B | - |
| Black platebody (t) | 2583 | N, M | L: clue reward | B | - |
| Black platelegs (t) | 2585 | N, M | L: clue reward | B | - |
| Black plateskirt (t) | 3472 | N, M | L: clue reward | B | - |
| Black kiteshield (t) | 2589 | N, M | L: clue reward | B | - |
| Black full helm (g) | 2595 | N, M | L: clue reward | B | - |
| Black platebody (g) | 2591 | N, M | L: clue reward | B | - |
| Black platelegs (g) | 2593 | N, M | L: clue reward | B | - |
| Black plateskirt (g) | 3473 | N, M | L: clue reward | B | - |
| Black kiteshield (g) | 2597 | N, M | L: clue reward | B | - |
| Black shield (h1) | 7332 | N, M | L: clue reward | B | - |
| Black shield (h2) | 7338 | N, M | L: clue reward | B | - |
| Black shield (h3) | 7344 | N, M | L: clue reward | B | - |
| Black shield (h4) | 7350 | N, M | L: clue reward | B | - |
| Black shield (h5) | 7356 | N, M | L: clue reward | B | - |
| Black helm (h1) | 10306 | N, M | L: clue reward | B | - |
| Black helm (h2) | 10308 | N, M | L: clue reward | B | - |
| Black helm (h3) | 10310 | N, M | L: clue reward | B | - |
| Black helm (h4) | 10312 | N, M | L: clue reward | B | - |
| Black helm (h5) | 10314 | N, M | L: clue reward | B | - |
| Black platebody (h1) | 23366 | N, M | L: clue reward | B | - |
| Black platebody (h2) | 23369 | N, M | L: clue reward | B | - |
| Black platebody (h3) | 23372 | N, M | L: clue reward | B | - |
| Black platebody (h4) | 23375 | N, M | L: clue reward | B | - |
| Black platebody (h5) | 23378 | N, M | L: clue reward | B | - |
| Steel full helm (t) | 20193 | N, M | L: clue reward | B | - |
| Steel platebody (t) | 20184 | N, M | L: clue reward | B | - |
| Steel platelegs (t) | 20187 | N, M | L: clue reward | B | - |
| Steel plateskirt (t) | 20190 | N, M | L: clue reward | B | - |
| Steel kiteshield (t) | 20196 | N, M | L: clue reward | B | - |
| Steel full helm (g) | 20178 | N, M | L: clue reward | B | - |
| Steel platebody (g) | 20169 | N, M | L: clue reward | B | - |
| Steel platelegs (g) | 20172 | N, M | L: clue reward | B | - |
| Steel plateskirt (g) | 20175 | N, M | L: clue reward | B | - |
| Steel kiteshield (g) | 20181 | N, M | L: clue reward | B | - |
| Iron platebody (t) | 12225 | N, M | L: clue reward | B | - |
| Iron platelegs (t) | 12227 | N, M | L: clue reward | B | - |
| Iron plateskirt (t) | 12229 | N, M | L: clue reward | B | - |
| Iron kiteshield (t) | 12233 | N, M | L: clue reward | B | - |
| Iron full helm (t) | 12231 | N, M | L: clue reward | B | - |
| Iron platebody (g) | 12235 | N, M | L: clue reward | B | - |
| Iron platelegs (g) | 12237 | N, M | L: clue reward | B | - |
| Iron plateskirt (g) | 12239 | N, M | L: clue reward | B | - |
| Iron kiteshield (g) | 12243 | N, M | L: clue reward | B | - |
| Iron full helm (g) | 12241 | N, M | L: clue reward | B | - |
| Bronze platebody (t) | 12215 | N, M | L: clue reward | B | - |
| Bronze platelegs (t) | 12217 | N, M | L: clue reward | B | - |
| Bronze plateskirt (t) | 12219 | N, M | L: clue reward | B | - |
| Bronze kiteshield (t) | 12223 | N, M | L: clue reward | B | - |
| Bronze full helm (t) | 12221 | N, M | L: clue reward | B | - |
| Bronze platebody (g) | 12205 | N, M | L: clue reward | B | - |
| Bronze platelegs (g) | 12207 | N, M | L: clue reward | B | - |
| Bronze plateskirt (g) | 12209 | N, M | L: clue reward | B | - |
| Bronze kiteshield (g) | 12213 | N, M | L: clue reward | B | - |
| Bronze full helm (g) | 12211 | N, M | L: clue reward | B | - |
| Studded body (g) | 7362 | N, M | L: clue reward | B | - |
| Studded chaps (g) | 7366 | N, M | L: clue reward | B | - |
| Studded body (t) | 7364 | N, M | L: clue reward | B | - |
| Studded chaps (t) | 7368 | N, M | L: clue reward | B | - |
| Leather body (g) | 23381 | N, M | L: clue reward | B | - |
| Leather chaps (g) | 23384 | N, M | L: clue reward | B | - |
| Blue wizard hat (g) | 7394 | N, M | L: clue reward | B | - |
| Blue wizard robe (g) | 7390 | N, M | L: clue reward | B | - |
| Blue skirt (g) | 7386 | N, M | L: clue reward | B | - |
| Blue wizard hat (t) | 7396 | N, M | L: clue reward | B | - |
| Blue wizard robe (t) | 7392 | N, M | L: clue reward | B | - |
| Blue skirt (t) | 7388 | N, M | L: clue reward | B | - |
| Black wizard hat (g) | 12453 | N, M | L: clue reward | B | - |
| Black wizard robe (g) | 12449 | N, M | L: clue reward | B | - |
| Black skirt (g) | 12445 | N, M | L: clue reward | B | - |
| Black wizard hat (t) | 12455 | N, M | L: clue reward | B | - |
| Black wizard robe (t) | 12451 | N, M | L: clue reward | B | - |
| Black skirt (t) | 12447 | N, M | L: clue reward | B | - |
| Monk's robe top (g) | 20199 | N, M | L: clue reward | B | - |
| Monk's robe (g) | 20202 | N, M | L: clue reward | B | - |
| Saradomin robe top | 10458 | N, M | L: clue reward | B | - |
| Saradomin robe legs | 10464 | N, M | L: clue reward | B | - |
| Guthix robe top | 10462 | N, M | L: clue reward | B | - |
| Guthix robe legs | 10466 | N, M | L: clue reward | B | - |
| Zamorak robe top | 10460 | N, M | L: clue reward | B | - |
| Zamorak robe legs | 10468 | N, M | L: clue reward | B | - |
| Ancient robe top | 12193 | N, M | L: clue reward | B | - |
| Ancient robe legs | 12195 | N, M | L: clue reward | B | - |
| Armadyl robe top | 12253 | N, M | L: clue reward | B | - |
| Armadyl robe legs | 12255 | N, M | L: clue reward | B | - |
| Bandos robe top | 12265 | N, M | L: clue reward | B | - |
| Bandos robe legs | 12267 | N, M | L: clue reward | B | - |
| Bob's red shirt | 10316 | N, M | L: clue reward | B | - |
| Bob's green shirt | 10320 | N, M | L: clue reward | B | - |
| Bob's blue shirt | 10318 | N, M | L: clue reward | B | - |
| Bob's black shirt | 10322 | N, M | L: clue reward | B | - |
| Bob's purple shirt | 10324 | N, M | L: clue reward | B | - |
| Highwayman mask | 2631 | N, M | L: clue reward | B | - |
| Blue beret | 2633 | N, M | L: clue reward | B | - |
| Black beret | 2635 | N, M | L: clue reward | B | - |
| White beret | 2637 | N, M | L: clue reward | B | - |
| Red beret | 12247 | N, M | L: clue reward | B | - |
| A powdered wig | 10392 | N, M | L: clue reward | B | - |
| Beanie | 12245 | N, M | L: clue reward | B | - |
| Imp mask | 12249 | N, M | L: clue reward | B | - |
| Goblin mask | 12251 | N, M | L: clue reward | B | - |
| Sleeping cap | 10398 | N, M | L: clue reward | B | - |
| Flared trousers | 10394 | N, M | L: clue reward | B | - |
| Pantaloons | 10396 | N, M | L: clue reward | B | - |
| Black cane | 12375 | N, M | L: clue reward | B | - |
| Staff of Bob the Cat | 23363 | N, M | L: clue reward | B | - |
| Red elegant shirt | 10404 | N, M | L: clue reward | B | - |
| Red elegant blouse | 10424 | N, M | L: clue reward | B | - |
| Red elegant legs | 10406 | N, M | L: clue reward | B | - |
| Red elegant skirt | 10426 | N, M | L: clue reward | B | - |
| Green elegant shirt | 10412 | N, M | L: clue reward | B | - |
| Green elegant blouse | 10432 | N, M | L: clue reward | B | - |
| Green elegant legs | 10414 | N, M | L: clue reward | B | - |
| Green elegant skirt | 10434 | N, M | L: clue reward | B | - |
| Blue elegant shirt | 10408 | N, M | L: clue reward | B | - |
| Blue elegant blouse | 10428 | N, M | L: clue reward | B | - |
| Blue elegant legs | 10410 | N, M | L: clue reward | B | - |
| Blue elegant skirt | 10430 | N, M | L: clue reward | B | - |
| Amulet of magic (t) | 10366 | N, M | L: clue reward | B | - |
| Amulet of power (t) | 23354 | N, M | L: clue reward | B | - |
| Black pickaxe | 12297 | N, M | L: clue reward | B | - |
| Ham joint | 23360 | N, M | L: clue reward | B | - |
| Rain bow | 23357 | N, M | L: clue reward | B | - |
| Willow comp bow | 10280 | N, M | L: clue reward | B | - |

<a id="medium-treasure-trails"></a>

### Medium Treasure Trails

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Ranger boots | 2577 | N, M | L: clue reward | B | - |
| Wizard boots | 2579 | N, M | L: clue reward | B | - |
| Holy sandals | 12598 | N, M | L: clue reward | B | - |
| Climbing boots (g) | 23413 | N, M | L: clue reward | B | - |
| Spiked manacles | 23389 | N, M | L: clue reward | B | - |
| Adamant full helm (t) | 2605 | N, M | L: clue reward | B | - |
| Adamant platebody (t) | 2599 | N, M | L: clue reward | B | - |
| Adamant platelegs (t) | 2601 | N, M | L: clue reward | B | - |
| Adamant plateskirt (t) | 3474 | N, M | L: clue reward | B | - |
| Adamant kiteshield (t) | 2603 | N, M | L: clue reward | B | - |
| Adamant full helm (g) | 2613 | N, M | L: clue reward | B | - |
| Adamant platebody (g) | 2607 | N, M | L: clue reward | B | - |
| Adamant platelegs (g) | 2609 | N, M | L: clue reward | B | - |
| Adamant plateskirt (g) | 3475 | N, M | L: clue reward | B | - |
| Adamant kiteshield (g) | 2611 | N, M | L: clue reward | B | - |
| Adamant shield (h1) | 7334 | N, M | L: clue reward | B | - |
| Adamant shield (h2) | 7340 | N, M | L: clue reward | B | - |
| Adamant shield (h3) | 7346 | N, M | L: clue reward | B | - |
| Adamant shield (h4) | 7352 | N, M | L: clue reward | B | - |
| Adamant shield (h5) | 7358 | N, M | L: clue reward | B | - |
| Adamant helm (h1) | 10296 | N, M | L: clue reward | B | - |
| Adamant helm (h2) | 10298 | N, M | L: clue reward | B | - |
| Adamant helm (h3) | 10300 | N, M | L: clue reward | B | - |
| Adamant helm (h4) | 10302 | N, M | L: clue reward | B | - |
| Adamant helm (h5) | 10304 | N, M | L: clue reward | B | - |
| Adamant platebody (h1) | 23392 | N, M | L: clue reward | B | - |
| Adamant platebody (h2) | 23395 | N, M | L: clue reward | B | - |
| Adamant platebody (h3) | 23398 | N, M | L: clue reward | B | - |
| Adamant platebody (h4) | 23401 | N, M | L: clue reward | B | - |
| Adamant platebody (h5) | 23404 | N, M | L: clue reward | B | - |
| Mithril full helm (g) | 12283 | N, M | L: clue reward | B | - |
| Mithril platebody (g) | 12277 | N, M | L: clue reward | B | - |
| Mithril platelegs (g) | 12279 | N, M | L: clue reward | B | - |
| Mithril plateskirt (g) | 12285 | N, M | L: clue reward | B | - |
| Mithril kiteshield (g) | 12281 | N, M | L: clue reward | B | - |
| Mithril full helm (t) | 12293 | N, M | L: clue reward | B | - |
| Mithril platebody (t) | 12287 | N, M | L: clue reward | B | - |
| Mithril platelegs (t) | 12289 | N, M | L: clue reward | B | - |
| Mithril plateskirt (t) | 12295 | N, M | L: clue reward | B | - |
| Mithril kiteshield (t) | 12291 | N, M | L: clue reward | B | - |
| Green d'hide body (g) | 7370 | N, M | L: clue reward | B | - |
| Green d'hide body (t) | 7372 | N, M | L: clue reward | B | - |
| Green d'hide chaps (g) | 7378 | N, M | L: clue reward | B | - |
| Green d'hide chaps (t) | 7380 | N, M | L: clue reward | B | - |
| Saradomin mitre | 10452 | N, M | L: clue reward | B | - |
| Saradomin cloak | 10446 | N, M | L: clue reward | B | - |
| Guthix mitre | 10454 | N, M | L: clue reward | B | - |
| Guthix cloak | 10448 | N, M | L: clue reward | B | - |
| Zamorak mitre | 10456 | N, M | L: clue reward | B | - |
| Zamorak cloak | 10450 | N, M | L: clue reward | B | - |
| Ancient mitre | 12203 | N, M | L: clue reward | B | - |
| Ancient cloak | 12197 | N, M | L: clue reward | B | - |
| Ancient stole | 12201 | N, M | L: clue reward | B | - |
| Ancient crozier | 12199 | N, M | L: clue reward | B | - |
| Armadyl mitre | 12259 | N, M | L: clue reward | B | - |
| Armadyl cloak | 12261 | N, M | L: clue reward | B | - |
| Armadyl stole | 12257 | N, M | L: clue reward | B | - |
| Armadyl crozier | 12263 | N, M | L: clue reward | B | - |
| Bandos mitre | 12271 | N, M | L: clue reward | B | - |
| Bandos cloak | 12273 | N, M | L: clue reward | B | - |
| Bandos stole | 12269 | N, M | L: clue reward | B | - |
| Bandos crozier | 12275 | N, M | L: clue reward | B | - |
| Red boater | 7319 | N, M | L: clue reward | B | - |
| Green boater | 7323 | N, M | L: clue reward | B | - |
| Orange boater | 7321 | N, M | L: clue reward | B | - |
| Black boater | 7327 | N, M | L: clue reward | B | - |
| Blue boater | 7325 | N, M | L: clue reward | B | - |
| Pink boater | 12309 | N, M | L: clue reward | B | - |
| Purple boater | 12311 | N, M | L: clue reward | B | - |
| White boater | 12313 | N, M | L: clue reward | B | - |
| Red headband | 2645 | N, M | L: clue reward | B | - |
| Black headband | 2647 | N, M | L: clue reward | B | - |
| Brown headband | 2649 | N, M | L: clue reward | B | - |
| White headband | 12299 | N, M | L: clue reward | B | - |
| Blue headband | 12301 | N, M | L: clue reward | B | - |
| Gold headband | 12303 | N, M | L: clue reward | B | - |
| Pink headband | 12305 | N, M | L: clue reward | B | - |
| Green headband | 12307 | N, M | L: clue reward | B | - |
| Crier hat | 12319 | N, M | L: clue reward | B | - |
| Crier coat | 20240 | N, M | L: clue reward | B | - |
| Crier bell | 20243 | N, M | L: clue reward | B | - |
| Adamant cane | 12377 | N, M | L: clue reward | B | - |
| Arceuus banner | 20251 | N, M | L: clue reward | B | - |
| Piscarilius banner | 20260 | N, M | L: clue reward | B | - |
| Hosidius banner | 20254 | N, M | L: clue reward | B | - |
| Shayzien banner | 20263 | N, M | L: clue reward | B | - |
| Lovakengj banner | 20257 | N, M | L: clue reward | B | - |
| Cabbage round shield | 20272 | N, M | L: clue reward | B | - |
| Black unicorn mask | 20266 | N, M | L: clue reward | B | - |
| White unicorn mask | 20269 | N, M | L: clue reward | B | - |
| Cat mask | 12361 | N, M | L: clue reward | B | - |
| Penguin mask | 12428 | N, M | L: clue reward | B | - |
| Leprechaun hat | 12359 | N, M | L: clue reward | B | - |
| Black leprechaun hat | 20246 | N, M | L: clue reward | B | - |
| Wolf mask | 23407 | N, M | L: clue reward | B | - |
| Wolf cloak | 23410 | N, M | L: clue reward | B | - |
| Purple elegant shirt | 10416 | N, M | L: clue reward | B | - |
| Purple elegant blouse | 10436 | N, M | L: clue reward | B | - |
| Purple elegant legs | 10418 | N, M | L: clue reward | B | - |
| Purple elegant skirt | 10438 | N, M | L: clue reward | B | - |
| Black elegant shirt | 10400 | N, M | L: clue reward | B | - |
| White elegant blouse | 10420 | N, M | L: clue reward | B | - |
| Black elegant legs | 10402 | N, M | L: clue reward | B | - |
| White elegant skirt | 10422 | N, M | L: clue reward | B | - |
| Pink elegant shirt | 12315 | N, M | L: clue reward | B | - |
| Pink elegant blouse | 12339 | N, M | L: clue reward | B | - |
| Pink elegant legs | 12317 | N, M | L: clue reward | B | - |
| Pink elegant skirt | 12341 | N, M | L: clue reward | B | - |
| Gold elegant shirt | 12347 | N, M | L: clue reward | B | - |
| Gold elegant blouse | 12343 | N, M | L: clue reward | B | - |
| Gold elegant legs | 12349 | N, M | L: clue reward | B | - |
| Gold elegant skirt | 12345 | N, M | L: clue reward | B | - |
| Gnomish firelighter | 20275 | N, M | L: clue reward | B | - |
| Strength amulet (t) | 10364 | N, M | L: clue reward | B | - |
| Yew comp bow | 10282 | N, M | L: clue reward | B | - |

<a id="hard-treasure-trails"></a>

### Hard Treasure Trails

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Robin Hood hat | 2581 | N, M | L: clue reward | B | - |
| Dragon boots ornament kit | 22231 | N, M | L: clue reward | B | - |
| Rune defender ornament kit | 23227 | N, M | L: clue reward | B | - |
| Tzhaar-ket-om ornament kit | 23232 | N, M | L: clue reward | B | - |
| Berserker necklace ornament kit | 23237 | N, M | L: clue reward | B | - |
| Rune full helm (t) | 2627 | N, M | L: clue reward | B | - |
| Rune platebody (t) | 2623 | N, M | L: clue reward | B | - |
| Rune platelegs (t) | 2625 | N, M | L: clue reward | B | - |
| Rune plateskirt (t) | 3477 | N, M | L: clue reward | B | - |
| Rune kiteshield (t) | 2629 | N, M | L: clue reward | B | - |
| Rune full helm (g) | 2619 | N, M | L: clue reward | B | - |
| Rune platebody (g) | 2615 | N, M | L: clue reward | B | - |
| Rune platelegs (g) | 2617 | N, M | L: clue reward | B | - |
| Rune plateskirt (g) | 3476 | N, M | L: clue reward | B | - |
| Rune kiteshield (g) | 2621 | N, M | L: clue reward | B | - |
| Zamorak full helm | 2657 | N, M | L: clue reward | B | - |
| Zamorak platebody | 2653 | N, M | L: clue reward | B | - |
| Zamorak platelegs | 2655 | N, M | L: clue reward | B | - |
| Zamorak plateskirt | 3478 | N, M | L: clue reward | B | - |
| Zamorak kiteshield | 2659 | N, M | L: clue reward | B | - |
| Guthix full helm | 2673 | N, M | L: clue reward | B | - |
| Guthix platebody | 2669 | N, M | L: clue reward | B | - |
| Guthix platelegs | 2671 | N, M | L: clue reward | B | - |
| Guthix plateskirt | 3480 | N, M | L: clue reward | B | - |
| Guthix kiteshield | 2675 | N, M | L: clue reward | B | - |
| Saradomin full helm | 2665 | N, M | L: clue reward | B | - |
| Saradomin platebody | 2661 | N, M | L: clue reward | B | - |
| Saradomin platelegs | 2663 | N, M | L: clue reward | B | - |
| Saradomin plateskirt | 3479 | N, M | L: clue reward | B | - |
| Saradomin kiteshield | 2667 | N, M | L: clue reward | B | - |
| Ancient full helm | 12466 | N, M | L: clue reward | B | - |
| Ancient platebody | 12460 | N, M | L: clue reward | B | - |
| Ancient platelegs | 12462 | N, M | L: clue reward | B | - |
| Ancient plateskirt | 12464 | N, M | L: clue reward | B | - |
| Ancient kiteshield | 12468 | N, M | L: clue reward | B | - |
| Armadyl full helm | 12476 | N, M | L: clue reward | B | - |
| Armadyl platebody | 12470 | N, M | L: clue reward | B | - |
| Armadyl platelegs | 12472 | N, M | L: clue reward | B | - |
| Armadyl plateskirt | 12474 | N, M | L: clue reward | B | - |
| Armadyl kiteshield | 12478 | N, M | L: clue reward | B | - |
| Bandos full helm | 12486 | N, M | L: clue reward | B | - |
| Bandos platebody | 12480 | N, M | L: clue reward | B | - |
| Bandos platelegs | 12482 | N, M | L: clue reward | B | - |
| Bandos plateskirt | 12484 | N, M | L: clue reward | B | - |
| Bandos kiteshield | 12488 | N, M | L: clue reward | B | - |
| Rune shield (h1) | 7336 | N, M | L: clue reward | B | - |
| Rune shield (h2) | 7342 | N, M | L: clue reward | B | - |
| Rune shield (h3) | 7348 | N, M | L: clue reward | B | - |
| Rune shield (h4) | 7354 | N, M | L: clue reward | B | - |
| Rune shield (h5) | 7360 | N, M | L: clue reward | B | - |
| Rune helm (h1) | 10286 | N, M | L: clue reward | B | - |
| Rune helm (h2) | 10288 | N, M | L: clue reward | B | - |
| Rune helm (h3) | 10290 | N, M | L: clue reward | B | - |
| Rune helm (h4) | 10292 | N, M | L: clue reward | B | - |
| Rune helm (h5) | 10294 | N, M | L: clue reward | B | - |
| Rune platebody (h1) | 23209 | N, M | L: clue reward | B | - |
| Rune platebody (h2) | 23212 | N, M | L: clue reward | B | - |
| Rune platebody (h3) | 23215 | N, M | L: clue reward | B | - |
| Rune platebody (h4) | 23218 | N, M | L: clue reward | B | - |
| Rune platebody (h5) | 23221 | N, M | L: clue reward | B | - |
| Saradomin coif | 10390 | N, M | L: clue reward | B | - |
| Saradomin d'hide body | 10386 | N, M | L: clue reward | B | - |
| Saradomin chaps | 10388 | N, M | L: clue reward | B | - |
| Saradomin bracers | 10384 | N, M | L: clue reward | B | - |
| Saradomin d'hide boots | 19933 | N, M | L: clue reward | B | - |
| Saradomin d'hide shield | 23191 | N, M | L: clue reward | B | - |
| Guthix coif | 10382 | N, M | L: clue reward | B | - |
| Guthix d'hide body | 10378 | N, M | L: clue reward | B | - |
| Guthix chaps | 10380 | N, M | L: clue reward | B | - |
| Guthix bracers | 10376 | N, M | L: clue reward | B | - |
| Guthix d'hide boots | 19927 | N, M | L: clue reward | B | - |
| Guthix d'hide shield | 23188 | N, M | L: clue reward | B | - |
| Zamorak coif | 10374 | N, M | L: clue reward | B | - |
| Zamorak d'hide body | 10370 | N, M | L: clue reward | B | - |
| Zamorak chaps | 10372 | N, M | L: clue reward | B | - |
| Zamorak bracers | 10368 | N, M | L: clue reward | B | - |
| Zamorak d'hide boots | 19936 | N, M | L: clue reward | B | - |
| Zamorak d'hide shield | 23194 | N, M | L: clue reward | B | - |
| Bandos coif | 12504 | N, M | L: clue reward | B | - |
| Bandos d'hide body | 12500 | N, M | L: clue reward | B | - |
| Bandos chaps | 12502 | N, M | L: clue reward | B | - |
| Bandos bracers | 12498 | N, M | L: clue reward | B | - |
| Bandos d'hide boots | 19924 | N, M | L: clue reward | B | - |
| Bandos d'hide shield | 23203 | N, M | L: clue reward | B | - |
| Armadyl coif | 12512 | N, M | L: clue reward | B | - |
| Armadyl d'hide body | 12508 | N, M | L: clue reward | B | - |
| Armadyl chaps | 12510 | N, M | L: clue reward | B | - |
| Armadyl bracers | 12506 | N, M | L: clue reward | B | - |
| Armadyl d'hide boots | 19930 | N, M | L: clue reward | B | - |
| Armadyl d'hide shield | 23200 | N, M | L: clue reward | B | - |
| Ancient coif | 12496 | N, M | L: clue reward | B | - |
| Ancient d'hide body | 12492 | N, M | L: clue reward | B | - |
| Ancient chaps | 12494 | N, M | L: clue reward | B | - |
| Ancient bracers | 12490 | N, M | L: clue reward | B | - |
| Ancient d'hide boots | 19921 | N, M | L: clue reward | B | - |
| Ancient d'hide shield | 23197 | N, M | L: clue reward | B | - |
| Red d'hide body (t) | 12331 | N, M | L: clue reward | B | - |
| Red d'hide chaps (t) | 12333 | N, M | L: clue reward | B | - |
| Red d'hide body (g) | 12327 | N, M | L: clue reward | B | - |
| Red d'hide chaps (g) | 12329 | N, M | L: clue reward | B | - |
| Blue d'hide body (t) | 7376 | N, M | L: clue reward | B | - |
| Blue d'hide chaps (t) | 7384 | N, M | L: clue reward | B | - |
| Blue d'hide body (g) | 7374 | N, M | L: clue reward | B | - |
| Blue d'hide chaps (g) | 7382 | N, M | L: clue reward | B | - |
| Enchanted hat | 7400 | N, M | L: clue reward | B | - |
| Enchanted top | 7399 | N, M | L: clue reward | B | - |
| Enchanted robe | 7398 | N, M | L: clue reward | B | - |
| Saradomin stole | 10470 | N, M | L: clue reward | B | - |
| Saradomin crozier | 10440 | N, M | L: clue reward | B | - |
| Guthix stole | 10472 | N, M | L: clue reward | B | - |
| Guthix crozier | 10442 | N, M | L: clue reward | B | - |
| Zamorak stole | 10474 | N, M | L: clue reward | B | - |
| Zamorak crozier | 10444 | N, M | L: clue reward | B | - |
| Zombie head | 19912 | N, M | L: clue reward | B | - |
| Cyclops head | 19915 | N, M | L: clue reward | B | - |
| Pirate's hat | 2651 | N, M | L: clue reward | B | - |
| Red cavalier | 12323 | N, M | L: clue reward | B | - |
| White cavalier | 12321 | N, M | L: clue reward | B | - |
| Navy cavalier | 12325 | N, M | L: clue reward | B | - |
| Tan cavalier | 2639 | N, M | L: clue reward | B | - |
| Dark cavalier | 2641 | N, M | L: clue reward | B | - |
| Black cavalier | 2643 | N, M | L: clue reward | B | - |
| Pith helmet | 12516 | N, M | L: clue reward | B | - |
| Explorer backpack | 12514 | N, M | L: clue reward | B | - |
| Thieving bag | 23224 | N, M | L: clue reward | B | - |
| Green dragon mask | 12518 | N, M | L: clue reward | B | - |
| Blue dragon mask | 12520 | N, M | L: clue reward | B | - |
| Red dragon mask | 12522 | N, M | L: clue reward | B | - |
| Black dragon mask | 12524 | N, M | L: clue reward | B | - |
| Nunchaku | 19918 | N, M | L: clue reward | B | - |
| Dual sai | 23206 | N, M | L: clue reward | B | - |
| Rune cane | 12379 | N, M | L: clue reward | B | - |
| Amulet of glory (t4) | 10354 | N, M | L: clue reward | B | - |
| Magic comp bow | 10284 | N, M | L: clue reward | B | - |

<a id="hard-treasure-trails-rare"></a>

### Hard Treasure Trails (Rare)

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Gilded platebody | 3481 | N, M | L: clue reward | B | - |
| Gilded platelegs | 3483 | N, M | L: clue reward | B | - |
| Gilded plateskirt | 3485 | N, M | L: clue reward | B | - |
| Gilded full helm | 3486 | N, M | L: clue reward | B | - |
| Gilded kiteshield | 3488 | N, M | L: clue reward | B | - |
| 3rd Age range top | 10330 | N, M | L: clue reward | B | - |
| 3rd Age range legs | 10332 | N, M | L: clue reward | B | - |
| 3rd Age range coif | 10334 | N, M | L: clue reward | B | - |
| 3rd Age vambraces | 10336 | N, M | L: clue reward | B | - |
| 3rd Age robe top | 10338 | N, M | L: clue reward | B | - |
| 3rd Age robe | 10340 | N, M | L: clue reward | B | - |
| 3rd Age mage hat | 10342 | N, M | L: clue reward | B | - |
| 3rd Age amulet | 10344 | N, M | L: clue reward | B | - |
| 3rd Age platelegs | 10346 | N, M | L: clue reward | B | - |
| 3rd Age platebody | 10348 | N, M | L: clue reward | B | - |
| 3rd Age full helmet | 10350 | N, M | L: clue reward | B | - |
| 3rd Age kiteshield | 10352 | N, M | L: clue reward | B | - |
| Gilded med helm | 20146 | N, M | L: clue reward | B | - |
| Gilded chainbody | 20149 | N, M | L: clue reward | B | - |
| Gilded sq shield | 20152 | N, M | L: clue reward | B | - |
| Gilded 2h sword | 20155 | N, M | L: clue reward | B | - |
| Gilded spear | 20158 | N, M | L: clue reward | B | - |
| Gilded hasta | 20161 | N, M | L: clue reward | B | - |
| 3rd Age plateskirt | 23242 | N, M | L: clue reward | B | - |

<a id="elite-treasure-trails"></a>

### Elite Treasure Trails

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Fury ornament kit | 12526 | N, M | L: clue reward | B | - |
| Dragon chainbody ornament kit | 12534 | N, M | L: clue reward | B | - |
| Dragon legs/skirt ornament kit | 12536 | N, M | L: clue reward | B | - |
| Dragon sq shield ornament kit | 12532 | N, M | L: clue reward | B | - |
| Dragon full helm ornament kit | 12538 | N, M | L: clue reward | B | - |
| Dragon scimitar ornament kit | 20002 | N, M | L: clue reward | B | - |
| Light infinity colour kit | 12530 | N, M | L: clue reward | B | - |
| Dark infinity colour kit | 12528 | N, M | L: clue reward | B | - |
| Holy wraps | 19997 | N, M | L: clue reward | B | - |
| Ranger gloves | 19994 | N, M | L: clue reward | B | - |
| Rangers' tunic | 12596 | N, M | L: clue reward | B | - |
| Rangers' tights | 23249 | N, M | L: clue reward | B | - |
| Black d'hide body (g) | 12381 | N, M | L: clue reward | B | - |
| Black d'hide chaps (g) | 12383 | N, M | L: clue reward | B | - |
| Black d'hide body (t) | 12385 | N, M | L: clue reward | B | - |
| Black d'hide chaps (t) | 12387 | N, M | L: clue reward | B | - |
| Royal crown | 12397 | N, M | L: clue reward | B | - |
| Royal sceptre | 12439 | N, M | L: clue reward | B | - |
| Royal gown top | 12393 | N, M | L: clue reward | B | - |
| Royal gown bottom | 12395 | N, M | L: clue reward | B | - |
| Musketeer hat | 12351 | N, M | L: clue reward | B | - |
| Musketeer tabard | 12441 | N, M | L: clue reward | B | - |
| Musketeer pants | 12443 | N, M | L: clue reward | B | - |
| Dark tuxedo jacket | 19958 | N, M | L: clue reward | B | - |
| Dark trousers | 19964 | N, M | L: clue reward | B | - |
| Dark tuxedo shoes | 19967 | N, M | L: clue reward | B | - |
| Dark tuxedo cuffs | 19961 | N, M | L: clue reward | B | - |
| Dark bow tie | 19970 | N, M | L: clue reward | B | - |
| Light tuxedo jacket | 19973 | N, M | L: clue reward | B | - |
| Light trousers | 19979 | N, M | L: clue reward | B | - |
| Light tuxedo shoes | 19982 | N, M | L: clue reward | B | - |
| Light tuxedo cuffs | 19976 | N, M | L: clue reward | B | - |
| Light bow tie | 19985 | N, M | L: clue reward | B | - |
| Arceuus scarf | 19943 | N, M | L: clue reward | B | - |
| Hosidius scarf | 19946 | N, M | L: clue reward | B | - |
| Piscarilius scarf | 19952 | N, M | L: clue reward | B | - |
| Shayzien scarf | 19955 | N, M | L: clue reward | B | - |
| Lovakengj scarf | 19949 | N, M | L: clue reward | B | - |
| Bronze dragon mask | 12363 | N, M | L: clue reward | B | - |
| Iron dragon mask | 12365 | N, M | L: clue reward | B | - |
| Steel dragon mask | 12367 | N, M | L: clue reward | B | - |
| Mithril dragon mask | 12369 | N, M | L: clue reward | B | - |
| Adamant dragon mask | 23270 | N, M | L: clue reward | B | - |
| Rune dragon mask | 23273 | N, M | L: clue reward | B | - |
| Katana | 12357 | N, M | L: clue reward | B | - |
| Dragon cane | 12373 | N, M | L: clue reward | B | - |
| Briefcase | 12335 | N, M | L: clue reward | B | - |
| Bucket helm | 19991 | N, M | L: clue reward | B | - |
| Blacksmith's helm | 19988 | N, M | L: clue reward | B | - |
| Deerstalker | 12540 | N, M | L: clue reward | B | - |
| Afro | 12430 | N, M | L: clue reward | B | - |
| Big pirate hat | 12355 | N, M | L: clue reward | B | - |
| Top hat | 12432 | N, M | L: clue reward | B | - |
| Monocle | 12353 | N, M | L: clue reward | B | - |
| Sagacious spectacles | 12337 | N, M | L: clue reward | B | - |
| Fremennik kilt | 23246 | N, M | L: clue reward | B | - |
| Giant boot | 23252 | N, M | L: clue reward | B | - |
| Uri's hat | 23255 | N, M | L: clue reward | B | - |
| Ring of 3rd Age | 23185 | N, M | L: clue reward | B | - |

<a id="elite-treasure-trails-rare"></a>

### Elite Treasure Trails (Rare)

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Gilded platebody | 3481 | N, M | L: clue reward | B | - |
| Gilded platelegs | 3483 | N, M | L: clue reward | B | - |
| Gilded plateskirt | 3485 | N, M | L: clue reward | B | - |
| Gilded full helm | 3486 | N, M | L: clue reward | B | - |
| Gilded kiteshield | 3488 | N, M | L: clue reward | B | - |
| 3rd Age range top | 10330 | N, M | L: clue reward | B | - |
| 3rd Age range legs | 10332 | N, M | L: clue reward | B | - |
| 3rd Age range coif | 10334 | N, M | L: clue reward | B | - |
| 3rd Age vambraces | 10336 | N, M | L: clue reward | B | - |
| 3rd Age robe top | 10338 | N, M | L: clue reward | B | - |
| 3rd Age robe | 10340 | N, M | L: clue reward | B | - |
| 3rd Age mage hat | 10342 | N, M | L: clue reward | B | - |
| 3rd Age amulet | 10344 | N, M | L: clue reward | B | - |
| 3rd Age platelegs | 10346 | N, M | L: clue reward | B | - |
| 3rd Age platebody | 10348 | N, M | L: clue reward | B | - |
| 3rd Age full helmet | 10350 | N, M | L: clue reward | B | - |
| 3rd Age kiteshield | 10352 | N, M | L: clue reward | B | - |
| Lava dragon mask | 12371 | N, M | L: clue reward | B | - |
| Gilded scimitar | 12389 | N, M | L: clue reward | B | - |
| Gilded boots | 12391 | N, M | L: clue reward | B | - |
| 3rd Age wand | 12422 | N, M | L: clue reward | B | - |
| 3rd Age bow | 12424 | N, M | L: clue reward | B | - |
| 3rd Age longsword | 12426 | N, M | L: clue reward | B | - |
| 3rd Age cloak | 12437 | N, M | L: clue reward | B | - |
| Ring of nature | 20005 | N, M | L: clue reward | B | - |
| Gilded med helm | 20146 | N, M | L: clue reward | B | - |
| Gilded chainbody | 20149 | N, M | L: clue reward | B | - |
| Gilded sq shield | 20152 | N, M | L: clue reward | B | - |
| Gilded 2h sword | 20155 | N, M | L: clue reward | B | - |
| Gilded spear | 20158 | N, M | L: clue reward | B | - |
| Gilded hasta | 20161 | N, M | L: clue reward | B | - |
| 3rd Age plateskirt | 23242 | N, M | L: clue reward | B | - |
| Gilded coif | 23258 | N, M | L: clue reward | B | - |
| Gilded d'hide vambraces | 23261 | N, M | L: clue reward | B | - |
| Gilded d'hide body | 23264 | N, M | L: clue reward | B | - |
| Gilded d'hide chaps | 23267 | N, M | L: clue reward | B | - |
| Gilded pickaxe | 23276 | N, M | L: clue reward | B | - |
| Gilded axe | 23279 | N, M | L: clue reward | B | - |
| Gilded spade | 23282 | N, M | L: clue reward | B | - |

<a id="master-treasure-trails"></a>

### Master Treasure Trails

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Bloodhound | 19730 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Armadyl godsword ornament kit | 20068 | N, M | L: clue reward | B | - |
| Bandos godsword ornament kit | 20071 | N, M | L: clue reward | B | - |
| Saradomin godsword ornament kit | 20074 | N, M | L: clue reward | B | - |
| Zamorak godsword ornament kit | 20077 | N, M | L: clue reward | B | - |
| Occult ornament kit | 20065 | N, M | L: clue reward | B | - |
| Torture ornament kit | 20062 | N, M | L: clue reward | B | - |
| Anguish ornament kit | 22246 | N, M | L: clue reward | B | - |
| Dragon defender ornament kit | 20143 | N, M | L: clue reward | B | - |
| Dragon kiteshield ornament kit | 22239 | N, M | L: clue reward | B | - |
| Dragon platebody ornament kit | 22236 | N, M | L: clue reward | B | - |
| Tormented ornament kit | 23348 | N, M | L: clue reward | B | - |
| Hood of darkness | 20128 | N, M | L: clue reward | B | - |
| Robe top of darkness | 20131 | N, M | L: clue reward | B | - |
| Robe bottom of darkness | 20137 | N, M | L: clue reward | B | - |
| Gloves of darkness | 20134 | N, M | L: clue reward | B | - |
| Boots of darkness | 20140 | N, M | L: clue reward | B | - |
| Samurai kasa | 20035 | N, M | L: clue reward | B | - |
| Samurai shirt | 20038 | N, M | L: clue reward | B | - |
| Samurai greaves | 20044 | N, M | L: clue reward | B | - |
| Samurai boots | 20047 | N, M | L: clue reward | B | - |
| Samurai gloves | 20041 | N, M | L: clue reward | B | - |
| Ankou mask | 20095 | N, M | L: clue reward | B | - |
| Ankou top | 20098 | N, M | L: clue reward | B | - |
| Ankou gloves | 20101 | N, M | L: clue reward | B | - |
| Ankou socks | 20107 | N, M | L: clue reward | B | - |
| Ankou's leggings | 20104 | N, M | L: clue reward | B | - |
| Mummy's head | 20080 | N, M | L: clue reward | B | - |
| Mummy's feet | 20092 | N, M | L: clue reward | B | - |
| Mummy's hands | 20086 | N, M | L: clue reward | B | - |
| Mummy's legs | 20089 | N, M | L: clue reward | B | - |
| Mummy's body | 20083 | N, M | L: clue reward | B | - |
| Shayzien hood | 20125 | N, M | L: clue reward | B | - |
| Hosidius hood | 20116 | N, M | L: clue reward | B | - |
| Arceuus hood | 20113 | N, M | L: clue reward | B | - |
| Piscarilius hood | 20122 | N, M | L: clue reward | B | - |
| Lovakengj hood | 20119 | N, M | L: clue reward | B | - |
| Lesser demon mask | 20020 | N, M | L: clue reward | B | - |
| Greater demon mask | 20023 | N, M | L: clue reward | B | - |
| Black demon mask | 20026 | N, M | L: clue reward | B | - |
| Jungle demon mask | 20032 | N, M | L: clue reward | B | - |
| Old demon mask | 20029 | N, M | L: clue reward | B | - |
| Left eye patch | 19724 | N, M | L: clue reward | B | - |
| Bowl wig | 20110 | N, M | L: clue reward | B | - |
| Ale of the gods | 20056 | N, M | L: clue reward | B | - |
| Obsidian cape (r) | 20050 | N, M | L: clue reward | B | - |
| Half moon spectacles | 20053 | N, M | L: clue reward | B | - |
| Fancy tiara | 20008 | N, M | L: clue reward | B | - |
| Ring of 3rd Age | 23185 | N, M | L: clue reward | B | - |

<a id="master-treasure-trails-rare"></a>

### Master Treasure Trails (Rare)

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Gilded platebody | 3481 | N, M | L: clue reward | B | - |
| Gilded platelegs | 3483 | N, M | L: clue reward | B | - |
| Gilded plateskirt | 3485 | N, M | L: clue reward | B | - |
| Gilded full helm | 3486 | N, M | L: clue reward | B | - |
| Gilded kiteshield | 3488 | N, M | L: clue reward | B | - |
| 3rd Age range top | 10330 | N, M | L: clue reward | B | - |
| 3rd Age range legs | 10332 | N, M | L: clue reward | B | - |
| 3rd Age range coif | 10334 | N, M | L: clue reward | B | - |
| 3rd Age vambraces | 10336 | N, M | L: clue reward | B | - |
| 3rd Age robe top | 10338 | N, M | L: clue reward | B | - |
| 3rd Age robe | 10340 | N, M | L: clue reward | B | - |
| 3rd Age mage hat | 10342 | N, M | L: clue reward | B | - |
| 3rd Age amulet | 10344 | N, M | L: clue reward | B | - |
| 3rd Age platelegs | 10346 | N, M | L: clue reward | B | - |
| 3rd Age platebody | 10348 | N, M | L: clue reward | B | - |
| 3rd Age full helmet | 10350 | N, M | L: clue reward | B | - |
| 3rd Age kiteshield | 10352 | N, M | L: clue reward | B | - |
| Gilded scimitar | 12389 | N, M | L: clue reward | B | - |
| Gilded boots | 12391 | N, M | L: clue reward | B | - |
| 3rd Age wand | 12422 | N, M | L: clue reward | B | - |
| 3rd Age bow | 12424 | N, M | L: clue reward | B | - |
| 3rd Age longsword | 12426 | N, M | L: clue reward | B | - |
| 3rd Age cloak | 12437 | N, M | L: clue reward | B | - |
| 3rd Age axe | 20011 | N, M | L: clue reward | B | - |
| 3rd Age pickaxe | 20014 | N, M | L: clue reward | B | - |
| Ring of coins | 20017 | N, M | L: clue reward | B | - |
| Bucket helm (g) | 20059 | N, M | L: clue reward | B | - |
| Gilded med helm | 20146 | N, M | L: clue reward | B | - |
| Gilded chainbody | 20149 | N, M | L: clue reward | B | - |
| Gilded sq shield | 20152 | N, M | L: clue reward | B | - |
| Gilded 2h sword | 20155 | N, M | L: clue reward | B | - |
| Gilded spear | 20158 | N, M | L: clue reward | B | - |
| Gilded hasta | 20161 | N, M | L: clue reward | B | - |
| 3rd Age plateskirt | 23242 | N, M | L: clue reward | B | - |
| Gilded coif | 23258 | N, M | L: clue reward | B | - |
| Gilded d'hide vambraces | 23261 | N, M | L: clue reward | B | - |
| Gilded d'hide body | 23264 | N, M | L: clue reward | B | - |
| Gilded d'hide chaps | 23267 | N, M | L: clue reward | B | - |
| Gilded pickaxe | 23276 | N, M | L: clue reward | B | - |
| Gilded axe | 23279 | N, M | L: clue reward | B | - |
| Gilded spade | 23282 | N, M | L: clue reward | B | - |
| 3rd Age druidic robe top | 23336 | N, M | L: clue reward | B | - |
| 3rd Age druidic robe bottoms | 23339 | N, M | L: clue reward | B | - |
| 3rd Age druidic staff | 23342 | N, M | L: clue reward | B | - |
| 3rd Age druidic cloak | 23345 | N, M | L: clue reward | B | - |

<a id="shared-treasure-trail-rewards"></a>

### Shared Treasure Trail Rewards

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Saradomin page 1 | 3827 | N, M | L: clue reward | B | - |
| Zamorak page 1 | 3831 | N, M | L: clue reward | B | - |
| Guthix page 1 | 3835 | N, M | L: clue reward | B | - |
| Bandos page 1 | 12613 | N, M | L: clue reward | B | - |
| Armadyl page 1 | 12617 | N, M | L: clue reward | B | - |
| Ancient page 1 | 12621 | N, M | L: clue reward | B | - |
| Saradomin page 2 | 3828 | N, M | L: clue reward | B | - |
| Zamorak page 2 | 3832 | N, M | L: clue reward | B | - |
| Guthix page 2 | 3836 | N, M | L: clue reward | B | - |
| Bandos page 2 | 12614 | N, M | L: clue reward | B | - |
| Armadyl page 2 | 12618 | N, M | L: clue reward | B | - |
| Ancient page 2 | 12622 | N, M | L: clue reward | B | - |
| Saradomin page 3 | 3829 | N, M | L: clue reward | B | - |
| Zamorak page 3 | 3833 | N, M | L: clue reward | B | - |
| Guthix page 3 | 3837 | N, M | L: clue reward | B | - |
| Bandos page 3 | 12615 | N, M | L: clue reward | B | - |
| Armadyl page 3 | 12619 | N, M | L: clue reward | B | - |
| Ancient page 3 | 12623 | N, M | L: clue reward | B | - |
| Saradomin page 4 | 3830 | N, M | L: clue reward | B | - |
| Zamorak page 4 | 3834 | N, M | L: clue reward | B | - |
| Guthix page 4 | 3838 | N, M | L: clue reward | B | - |
| Bandos page 4 | 12616 | N, M | L: clue reward | B | - |
| Armadyl page 4 | 12620 | N, M | L: clue reward | B | - |
| Ancient page 4 | 12624 | N, M | L: clue reward | B | - |
| Holy blessing | 20220 | N, M | L: clue reward | B | - |
| Unholy blessing | 20223 | N, M | L: clue reward | B | - |
| Peaceful blessing | 20226 | N, M | L: clue reward | B | - |
| War blessing | 20232 | N, M | L: clue reward | B | - |
| Honourable blessing | 20229 | N, M | L: clue reward | B | - |
| Ancient blessing | 20235 | N, M | L: clue reward | B | - |
| Nardah teleport | 12402 | N, M | L: clue reward | B | - |
| Mos le'harmless teleport | 12411 | N, M | L: clue reward | B | - |
| Mort'ton teleport | 12406 | N, M | L: clue reward | B | - |
| Feldip hills teleport | 12404 | N, M | L: clue reward | B | - |
| Lunar isle teleport | 12405 | N, M | L: clue reward | B | - |
| Digsite teleport | 12403 | N, M | L: clue reward | B | - |
| Piscatoris teleport | 12408 | N, M | L: clue reward | B | - |
| Pest control teleport | 12407 | N, M | L: clue reward | B | - |
| Tai bwo wannai teleport | 12409 | N, M | L: clue reward | B | - |
| Lumberyard teleport | 12642 | N, M | L: clue reward | B | - |
| Iorwerth camp teleport | 12410 | N, M | L: clue reward | B | - |
| Master scroll book (empty) | 21387 | N, M | L: clue reward | B | - |
| Red firelighter | 7329 | N, M | L: clue reward | B | - |
| Green firelighter | 7330 | N, M | L: clue reward | B | - |
| Blue firelighter | 7331 | N, M | L: clue reward | B | - |
| Purple firelighter | 10326 | N, M | L: clue reward | B | - |
| White firelighter | 10327 | N, M | L: clue reward | B | - |
| Charge dragonstone jewellery scroll | 20238 | N, M | L: clue reward | B | - |
| Purple sweets | 10476 | N, M | L: clue reward | B | - |

<a id="scroll-cases"></a>

### Scroll Cases

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Minor beginner scroll case | 30902 | N | L:  clue reward | B | - |
| Major beginner scroll case | 30904 | N | L:  clue reward | B | - |
| Minor easy scroll case | 30906 | N | L:  clue reward | B | - |
| Major easy scroll case | 30908 | N | L:  clue reward | B | - |
| Minor medium scroll case | 30910 | N | L:  clue reward | B | - |
| Major medium scroll case | 30912 | N | L:  clue reward | B | - |
| Minor hard scroll case | 30914 | N | L:  clue reward | B | - |
| Major hard scroll case | 30916 | N | L:  clue reward | B | - |
| Minor elite scroll case | 30918 | N | L:  clue reward | B | - |
| Major elite scroll case | 30920 | N | L:  clue reward | B | - |
| Minor master scroll case | 30922 | N | L:  clue reward | B | - |
| Major master scroll case | 30924 | N | L:  clue reward | B | - |
| Mimic scroll case | 30926 | N | L:  clue reward | B | - |

## Minigames

<a id="barbarian-assault"></a>

### Barbarian Assault

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Pet Penance Queen | 12703 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Fighter hat | 10548 | N, M | No purchase hook | B | Shop reward, a silent purchase is not detected. |
| Ranger hat | 10550 | N, M | No purchase hook | B | Shop reward, a silent purchase is not detected. |
| Runner hat | 10549 | N, M | No purchase hook | B | Shop reward, a silent purchase is not detected. |
| Healer hat | 10547 | N, M | No purchase hook | B | Shop reward, a silent purchase is not detected. |
| Fighter torso | 10551 | N, M | No purchase hook | B | Shop reward, a silent purchase is not detected. |
| Penance skirt | 10555 | N, M | No purchase hook | B | Shop reward, a silent purchase is not detected. |
| Runner boots | 10552 | N, M | No purchase hook | B | Shop reward, a silent purchase is not detected. |
| Penance gloves | 10553 | N, M | No purchase hook | B | Shop reward, a silent purchase is not detected. |
| Granite helm | 10589 | N, M | L: high gamble / terror dogs | B | - |
| Granite body | 10564 | N, M | No purchase hook | B | Shop reward, a silent purchase is not detected. |

<a id="barracuda-trials"></a>

### Barracuda Trials

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Stormy key | 31732 | N | None | B | - |
| Barrel stand | 31733 | N | None | B | - |
| Ralph's fabric roll | 31734 | N | None | B | - |
| Fetid key | 31744 | N | None | B | - |
| Captured wind mote | 31745 | N | None | B | - |
| Gurtob's fabric roll | 31746 | N | None | B | - |
| Serrated key | 31756 | N | None | B | - |
| Heart of ithell | 31757 | N | None | B | - |
| Gwyna's fabric roll | 31758 | N | None | B | - |

<a id="brimhaven-agility-arena"></a>

### Brimhaven Agility Arena

Graceful items share names between regular, Brimhaven agility, and Colossal Wrym instances

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Agility arena ticket | 2996 | N, M, C | No loot hook | B | Lap message plus matching ticket/voucher gain, the lap count alone does not submit. |
| Brimhaven voucher | 29482 | N, M, C | No loot hook | B | Lap message plus matching ticket/voucher gain, the lap count alone does not submit. |
| Pirate's hook | 2997 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Graceful hood | 21061 | N+ID, M+ID | No purchase hook | Skip shared name | Shared name, one recent matching ID required for N/M. Silent purchase/unlock is not detected. |
| Graceful top | 21067 | N+ID, M+ID | No purchase hook | Skip shared name | Shared name, one recent matching ID required for N/M. Silent purchase/unlock is not detected. |
| Graceful legs | 21070 | N+ID, M+ID | No purchase hook | Skip shared name | Shared name, one recent matching ID required for N/M. Silent purchase/unlock is not detected. |
| Graceful gloves | 21073 | N+ID, M+ID | No purchase hook | Skip shared name | Shared name, one recent matching ID required for N/M. Silent purchase/unlock is not detected. |
| Graceful boots | 21076 | N+ID, M+ID | No purchase hook | Skip shared name | Shared name, one recent matching ID required for N/M. Silent purchase/unlock is not detected. |
| Graceful cape | 21064 | N+ID, M+ID | No purchase hook | Skip shared name | Shared name, one recent matching ID required for N/M. Silent purchase/unlock is not detected. |

<a id="castle-wars"></a>

### Castle Wars

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Decorative helm | 4071 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative full helm | 25165 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative armour | 4069 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative sword | 4068 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative shield | 4072 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative armour | 4070 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative armour | 11893 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative boots | 25163 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative helm | 4506 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative full helm | 25169 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative armour | 4504 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative sword | 4503 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative shield | 4507 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative armour | 4505 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative armour | 11894 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative boots | 25167 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative helm | 4511 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative full helm | 25174 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative armour | 4509 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative sword | 4508 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative shield | 4512 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative armour | 4510 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative armour | 11895 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative boots | 25171 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Castlewars hood | 4513 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Castlewars cloak | 4514 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Castlewars hood | 4515 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Castlewars cloak | 4516 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Saradomin banner | 11891 | N, M | No purchase hook | B | - |
| Zamorak banner | 11892 | N, M | No purchase hook | B | - |
| Decorative armour | 11898 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative armour | 11896 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative armour | 11897 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative armour | 11899 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative armour | 11900 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Decorative armour | 11901 | N+ID, M+ID | No purchase hook | Skip shared name | Shared names need exact-ID evidence with a personal notification. |
| Saradomin halo | 12637 | N, M | No purchase hook | B | Shared names need exact-ID evidence with a personal notification. |
| Zamorak halo | 12638 | N, M | No purchase hook | B | Shared names need exact-ID evidence with a personal notification. |
| Guthix halo | 12639 | N, M | No purchase hook | B | Shared names need exact-ID evidence with a personal notification. |

<a id="fishing-trawler"></a>

### Fishing Trawler

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Angler hat | 13258 | N, M | L: Trawler reward interface | B | - |
| Angler top | 13259 | N, M | L: Trawler reward interface | B | - |
| Angler waders | 13260 | N, M | L: Trawler reward interface | B | - |
| Angler boots | 13261 | N, M | L: Trawler reward interface | B | - |

<a id="giants-foundry"></a>

### Giants' Foundry

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Smiths tunic | 27023 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Smiths trousers | 27025 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Smiths boots | 27027 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Smiths gloves | 27029 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Colossal blade | 27021 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Double ammo mould | 27012 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Kovac's grog | 27014 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Smithing catalyst | 27017 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Ore pack | 27019 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |

<a id="gnome-restaurant"></a>

### Gnome Restaurant

Each item has a specific reward dialog, including repeat obtains.

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Grand seed pod | 9469 | N, M, D | No loot hook | B | Specific tip dialog covers first and repeat obtains. |
| Gnome scarf | 9470 | N, M, D | No loot hook | B | Specific tip dialog covers first and repeat obtains. |
| Gnome goggles | 9472 | N, M, D | No loot hook | B | Specific tip dialog covers first and repeat obtains. |
| Mint cake | 9475 | N, M, D | No loot hook | B | Specific tip dialog covers first and repeat obtains. |

<a id="guardians-of-the-rift"></a>

### Guardians of the Rift

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Abyssal protector | 26901 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Abyssal pearls | 26792 | N, M | L: reward search / pouch | B | - |
| Catalytic talisman | 26798 | N, M | L: reward search | B | - |
| Abyssal needle | 26813 | N, M | L: reward search | B | - |
| Abyssal green dye | 26807 | N, M | L: reward search | B | - |
| Abyssal blue dye | 26809 | N, M | L: reward search  | B | - |
| Abyssal red dye | 26811 | N, M | L: reward search | B | - |
| Hat of the Eye | 26850 | N, M | No purchase hook | B | - |
| Robe top of the Eye | 26852 | N, M | No purchase hook | B | - |
| Robe bottoms of the Eye | 26854 | N, M | No purchase hook | B | - |
| Boots of the Eye | 26856 | N, M | No purchase hook | B | - |
| Ring of the elements | 26815 | N, M | No purchase hook | B | - |
| Abyssal lantern | 26822 | N, M | L: reward search | B | - |
| Guardian's eye | 26820 | N, M | No purchase hook | B | - |
| Intricate pouch | 26908 | N, M | L: reward search | B | - |
| Lost bag | 26912 | N, M | L: pouch | B | - |
| Tarnished locket | 26910 | N, M | L: pouch | B | - |

<a id="hallowed-sepulchre"></a>

### Hallowed Sepulchre

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Hallowed mark | 24711 | N, M | L: hallowed coffin | B | - |
| Hallowed token | 24719 | N, M | No purchase hook | B | Shop only. |
| Hallowed grapple | 24721 | N, M | No purchase hook | B | Shop only. |
| Hallowed focus | 24723 | N, M | No purchase hook | B | Shop only. |
| Hallowed symbol | 24725 | N, M | No purchase hook | B | Shop only. |
| Hallowed hammer | 24727 | N, M | No purchase hook | B | Shop only. |
| Hallowed ring | 24731 | N, M | No purchase hook | B | Shop only. |
| Dark dye | 24729 | N, M | No purchase hook | B | Shop only. |
| Dark acorn | 24733 | N, M | No purchase hook | B | Shop only. |
| Strange old lockpick (full) | 24740 | N, M | L: hallowed coffin | B | - |
| Ring of endurance (uncharged) | 24844 | N, M | L: hallowed coffin | B | - |
| Mysterious page | 24763 | N+ID | L: hallowed coffin | Skip shared name | -|
| Mysterious page | 24765 | N+ID | L: hallowed coffin | Skip shared name | -|
| Mysterious page | 24767 | N+ID | L: hallowed coffin | Skip shared name | -|
| Mysterious page | 24769 | N+ID | L: hallowed coffin | Skip shared name | -|
| Mysterious page | 24771 | N+ID | L: hallowed coffin | Skip shared name | -|

<a id="last-man-standing"></a>

### Last Man Standing

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Deadman's chest | 24189 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Deadman's legs | 24190 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Deadman's cape | 24191 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Armadyl halo | 24192 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Bandos halo | 24195 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Seren halo | 24198 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Ancient halo | 24201 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Brassica halo | 24204 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Golden armadyl special attack | 24868 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Golden bandos special attack | 24869 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Golden saradomin special attack | 24870 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Golden zamorak special attack | 24871 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Victor's cape (1) | 24207 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Victor's cape (10) | 24209 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Victor's cape (50) | 24211 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Victor's cape (100) | 24213 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Victor's cape (500) | 24215 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Victor's cape (1000) | 24520 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Granite clamp | 12849 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Ornate maul handle | 24229 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Steam staff upgrade kit | 12798 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Lava staff upgrade kit | 21202 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Dragon pickaxe upgrade kit | 12800 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Ward upgrade kit | 12802 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Green dark bow paint | 12759 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Yellow dark bow paint | 12761 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| White dark bow paint | 12763 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Blue dark bow paint | 12757 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Volcanic whip mix | 12771 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Frozen whip mix | 12769 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Guthixian icon | 24217 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Swift blade | 24219 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |

<a id="magic-training-arena"></a>

### Magic Training Arena

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Beginner wand | 6908 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Apprentice wand | 6910 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Teacher wand | 6912 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Master wand | 6914 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Infinity hat | 6918 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Infinity top | 6916 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Infinity bottoms | 6924 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Infinity boots | 6920 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Infinity gloves | 6922 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Mage's book | 6889 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Bones to peaches | 6926 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |

<a id="mahogany-homes"></a>

### Mahogany Homes

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Supply crate | 24884 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Carpenter's helmet | 24872 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Carpenter's shirt | 24874 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Carpenter's trousers | 24876 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Carpenter's boots | 24878 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Amy's saw | 24880 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Plank sack | 25629 → 24882 | N, M | No purchase hook | B | Explicit ID alias. Silent purchase/unlock is not detected. |
| Hosidius blueprints | 24885 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |

<a id="pest-control"></a>

### Pest Control

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Void knight mace | 8841 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Void knight top | 8839 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Void knight robe | 8840 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Void knight gloves | 8842 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Void mage helm | 11663 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Void melee helm | 11665 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Void ranger helm | 11664 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Void seal(8) | 11666 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Elite void top | 13072 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Elite void robe | 13073 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |

<a id="mastering-mixology"></a>

### Mastering Mixology

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Prescription goggles | 29974 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Alchemist labcoat | 29978 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Alchemist pants | 29982 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Alchemist gloves | 29986 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Alchemist's amulet | 29992 → 29990 | N, M | No purchase hook | B | Explicit ID alias. Silent purchase/unlock is not detected. |
| Reagent pouch | 29996 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Chugging barrel (disassembled) | 30002 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |

<a id="rogues-den"></a>

### Rogues' Den

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Rogue mask | 5554 | N, M | None | B | Needs a recognized notification. |
| Rogue top | 5553 | N, M | None | B | Needs a recognized notification. |
| Rogue trousers | 5555 | N, M | None | B | Needs a recognized notification. |
| Rogue boots | 5557 | N, M | None | B | Needs a recognized notification. |
| Rogue gloves | 5556 | N, M | None | B | Needs a recognized notification. |

<a id="shades-of-mortton"></a>

### Shades of Mort'ton

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Amulet of the Damned (full) | 12851 | N, M | L: shade chest | B | - |
| Flamtaer bag | 25630 → 12854 | N, M | L: shade chest | B | explicit display-ID alias. |
| Fine cloth | 3470 | N, M | L: shade chest | B | - |
| Bronze locks | 25442 | N, M | L: shade chest | B | - |
| Steel locks | 25445 | N, M | L: shade chest | B | - |
| Black locks | 25448 | N, M | L: shade chest | B | - |
| Silver locks | 25451 | N, M | L: shade chest | B | - |
| Gold locks | 25454 | N, M | L: shade chest | B | - |
| Zealot's helm | 25438 | N, M | L: shade chest | B | - |
| Zealot's robe top | 25434 | N, M | L: shade chest | B | - |
| Zealot's robe bottom | 25436 | N, M | L: shade chest | B | - |
| Zealot's boots | 25440 | N, M | L: shade chest | B | - |
| Tree wizards' journal | 25474 | N, M | L: shade chest | B | - |
| Bloody notes | 25476 | N, M | L: shade chest | B | - |

<a id="soul-wars"></a>

### Soul Wars

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Lil' Creator | 25348 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Soul cape | 25346 | N, M | No purchase hook | B | - |
| Ectoplasmator | 25340 | N, M | No purchase hook | B | - |

<a id="temple-trekking"></a>

### Temple Trekking

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Lumberjack hat | 10941 | N, M | L: NPC | B | - |
| Lumberjack top | 10939 | N, M | L: NPC | B | - |
| Lumberjack legs | 10940 | N, M | L: NPC | B | - |
| Lumberjack boots | 10933 | N, M | L: NPC | B | - |

<a id="tithe-farm"></a>

### Tithe Farm

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Farmer's strawhat | 13646 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Farmer's jacket | 13642 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Farmer's boro trousers | 13640 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Farmer's boots | 13644 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Seed box | 13639 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Gricoller's can | 13353 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Herb sack | 13226 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |

<a id="trouble-brewing"></a>

### Trouble Brewing

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Blue naval shirt | 8952 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Blue tricorn hat | 8959 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Blue navy slacks | 8991 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Green naval shirt | 8953 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Green tricorn hat | 8960 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Green navy slacks | 8992 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Red naval shirt | 8954 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Red tricorn hat | 8961 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Red navy slacks | 8993 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Brown naval shirt | 8955 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Brown tricorn hat | 8962 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Brown navy slacks | 8994 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Black naval shirt | 8956 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Black tricorn hat | 8963 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Black navy slacks | 8995 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Purple naval shirt | 8957 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Purple tricorn hat | 8964 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Purple navy slacks | 8996 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Grey naval shirt | 8958 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Grey tricorn hat | 8965 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Grey navy slacks | 8997 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Cutthroat flag | 8966 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Gilded smile flag | 8967 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Bronze fist flag | 8968 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Lucky shot flag | 8969 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Treasure flag | 8970 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Phasmatys flag | 8971 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| The stuff | 8988 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Rum | 8940 | N+ID, M+ID | No purchase hook | Skip shared name | Shared name, one recent matching ID required for N/M. Silent purchase/unlock is not detected. |
| Rum | 8941 | N+ID, M+ID | No purchase hook | Skip shared name | Shared name, one recent matching ID required for N/M. Silent purchase/unlock is not detected. |

<a id="vale-totems"></a>

### Vale Totems

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Fletching knife | 31043 | N, M | L: offerings | B | - |
| Bow string spool | 31052 | N, M | L: offerings | B | - |
| Ent branch | 31032 | N, M | L: offerings | B | - |
| Greenman mask | 31034 | N, M | L: offerings | B | - |

<a id="volcanic-mine"></a>

### Volcanic Mine

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Ash covered tome | 21697 | N, M | No purchase hook | B | - |
| Large water container | 25615 | N, M | No purchase hook | B | - |
| Volcanic mine teleport | 21541 | N, M | No purchase hook | B | - |
| Dragon pickaxe (broken) | 27695 | N, M | L: ore pack | B | - |
| Prospector helmet | 12013 | N, M | No purchase hook | B | - |
| Prospector jacket | 12014 | N, M | No purchase hook | B | - |
| Prospector legs | 12015 | N, M | No purchase hook | B | - |
| Prospector boots | 12016 | N, M | No purchase hook | B | - |

## Other

<a id="aerial-fishing"></a>

### Aerial Fishing

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Golden tench | 22840 | N, M, U | No loot hook | B | Detected through unique chat message. |
| Pearl fishing rod | 22846 | N, M | No purchase hook | B | - |
| Pearl fly fishing rod | 22844 | N, M | No purchase hook | B | - |
| Pearl barbarian rod | 22842 | N, M | No purchase hook | B | - |
| Fish sack | 22838 | N, M | No purchase hook | B | - |
| Angler hat | 13258 | N, M | No purchase hook | B | - |
| Angler top | 13259 | N, M | No purchase hook | B | - |
| Angler waders | 13260 | N, M | No purchase hook | B | - |
| Angler boots | 13261 | N, M | No purchase hook | B | - |

<a id="all-pets"></a>

### All Pets

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Abyssal orphan | 13262 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Ikkle Hydra | 22746 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Callisto cub | 13178 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Hellpuppy | 13247 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Pet Chaos Elemental | 11995 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Pet Zilyana | 12651 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Pet dark core | 12816 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Pet Dagannoth Prime | 12644 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Pet Dagannoth Supreme | 12643 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Pet Dagannoth Rex | 12645 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Tzrek-Jad | 13225 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Pet general Graardor | 12650 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Baby Mole | 12646 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Noon | 21748 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Jal-Nib-Rek | 21291 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Kalphite Princess | 12647 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Prince Black Dragon | 12653 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Pet Kraken | 12655 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Pet Kree'arra | 12649 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Pet K'ril Tsutsaroth | 12652 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Scorpia's offspring | 13181 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Skotos | 21273 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Pet Smoke Devil | 12648 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Venenatis spiderling | 13177 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Vet'ion Jr. | 13179 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Vorki | 21992 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Phoenix | 20693 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Pet Snakeling | 12921 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Olmlet | 20851 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Lil' Zik | 22473 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Bloodhound | 19730 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Pet Penance Queen | 12703 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Heron | 13320 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Rock golem | 13321 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Beaver | 13322 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Baby chinchompa | 13324 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Giant Squirrel | 20659 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Tangleroot | 20661 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Rocky | 20663 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Rift guardian | 20665 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Herbi | 21509 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Chompy chick | 13071 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Sraracha | 23495 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Smolcano | 23760 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Youngllef | 23757 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Little Nightmare | 24491 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Lil' Creator | 25348 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Tiny tempor | 25602 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Nexling | 26348 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Abyssal protector | 26901 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Tumeken's guardian | 27352 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Muphin | 27590 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Wisp | 28246 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Baron | 28250 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Butch | 28248 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Lil'viathan | 28252 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Scurry | 28801 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Smol Heredit | 28960 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Quetzin | 28962 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Nid | 29836 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Huberte | 30152 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Moxi | 30154 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Bran | 30622 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Yami | 30888 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Dom | 31130 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Soup | 31283 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Gull | 31285 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Beef | 33124 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Maggot marquess | 33642 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Mr McGroot | 34040 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Aggy | 34042 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |

<a id="boat-paints"></a>

### Boat Paints

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Barracuda paint | 32087 | N, M | L: barracude trial | B |- |
| Shark paint | 32090 | N, M | L: port task | B |- |
| Inky paint | 32093 | N, M | L: NPC | B |- |
| Angler's paint | 32096 | N, M | L: trawling | B |- |
| Salvor's paint | 32099 | N, M | L: salvage | B |- |
| Armadylean paint | 32102 | N, M | None | B | Does not appear in Loot Tracker, no obtain handler. |
| Zamorakian paint | 32104 | N, M | None | B | Does not appear in Loot Tracker, no obtain handler. |
| Guthixian paint | 32106 | N, M | None | B | Does not appear in Loot Tracker, no obtain handler. |
| Saradominist paint | 32108 | N, M | None | B | Does not appear in Loot Tracker, no obtain handler. |
| Merchant's paint | 32110 | N, M | None | B | Does not appear in Loot Tracker, no obtain handler. |
| Sandy paint | 32113 | N, M | None | B | Does not appear in Loot Tracker, no obtain handler. |

<a id="camdozaal"></a>

### Camdozaal

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Barronite mace | 25641 | N, M, D | -| B | Assembly only, specific Ramarno dialog. |
| Barronite head | 25635 | N, M, U | - | B | Specific acquisition game message. |
| Barronite handle | 25637 | N, M, U | - | B | Specific acquisition game message. |
| Barronite guard | 25639 | N, M | L: NPC | B | - |
| Ancient globe | 25686 | N, M | L: Barronite deposit | B | - |
| Ancient ledger | 25688 | N, M | L: Barronite deposit | B | - |
| Ancient astroscope | 25690 | N, M | L: Barronite deposit | B | - |
| Ancient treatise | 25692 | N, M | L: Barronite deposit | B | - |
| Ancient carcanet | 25694 | N, M | L: Barronite deposit | B | - |
| Imcando hammer | 25644 | N, M, D | - | B | Assembly only, specific Ramarno dialog. |

<a id="champions-challenge"></a>

### Champion's Challenge

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Earth warrior champion scroll | 6798 | N, M | L: NPC | B | - |
| Ghoul champion scroll | 6799 | N, M | L: NPC | B | - |
| Giant champion scroll | 6800 | N, M | L: NPC | B | - |
| Goblin champion scroll | 6801 | N, M | L: NPC | B | - |
| Hobgoblin champion scroll | 6802 | N, M | L: NPC | B | - |
| Imp champion scroll | 6803 | N, M | L: NPC | B | - |
| Jogre champion scroll | 6804 | N, M | L: NPC | B | - |
| Lesser demon champion scroll | 6805 | N, M | L: NPC | B | - |
| Skeleton champion scroll | 6806 | N, M | L: NPC | B | - |
| Zombie champion scroll | 6807 | N, M | L: NPC | B | - |
| Champion's cape | 21439 | N, M | None | B | - |

<a id="chaos-druids"></a>

### Chaos Druids

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Elder chaos top | 20517 | N, M | L: NPC | B | - |
| Elder chaos robe | 20520 | N, M | L: NPC | B | - |
| Elder chaos hood | 20595 | N, M | L: NPC | B | - |

<a id="chompy-bird-hunting"></a>

### Chompy Bird Hunting

Chompy bird hats share a name. Personal notifications need recent exact-ID evidence.

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Chompy chick | 13071 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Chompy bird hat | 2978 | N+ID, M+ID | No hat-award hook | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Chompy bird hat | 2979 | N+ID, M+ID | No hat-award hook | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Chompy bird hat | 2980 | N+ID, M+ID | No hat-award hook | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Chompy bird hat | 2981 | N+ID, M+ID | No hat-award hook | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Chompy bird hat | 2982 | N+ID, M+ID | No hat-award hook | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Chompy bird hat | 2983 | N+ID, M+ID | No hat-award hook | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Chompy bird hat | 2984 | N+ID, M+ID | No hat-award hook | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Chompy bird hat | 2985 | N+ID, M+ID | No hat-award hook | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Chompy bird hat | 2986 | N+ID, M+ID | No hat-award hook | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Chompy bird hat | 2987 | N+ID, M+ID | No hat-award hook | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Chompy bird hat | 2988 | N+ID, M+ID | No hat-award hook | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Chompy bird hat | 2989 | N+ID, M+ID | No hat-award hook | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Chompy bird hat | 2990 | N+ID, M+ID | No hat-award hook | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Chompy bird hat | 2991 | N+ID, M+ID | No hat-award hook | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Chompy bird hat | 2992 | N+ID, M+ID | No hat-award hook | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Chompy bird hat | 2993 | N+ID, M+ID | No hat-award hook | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Chompy bird hat | 2994 | N+ID, M+ID | No hat-award hook | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Chompy bird hat | 2995 | N+ID, M+ID | No hat-award hook | Skip shared name | Shared name, one recent matching ID required for N/M. |

<a id="colossal-wyrm-agility"></a>

### Colossal Wyrm Agility

Graceful items share names between regular, Brimhaven agility, and Colossal Wrym instances

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Colossal wyrm teleport scroll | 30040 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Calcified acorn | 30042 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Graceful hood | 30045 | N+ID, M+ID | No purchase hook | Skip shared name | Shared name, one recent matching ID required for N/M. Silent purchase/unlock is not detected. |
| Graceful cape | 30048 | N+ID, M+ID | No purchase hook | Skip shared name | Shared name, one recent matching ID required for N/M. Silent purchase/unlock is not detected. |
| Graceful top | 30051 | N+ID, M+ID | No purchase hook | Skip shared name | Shared name, one recent matching ID required for N/M. Silent purchase/unlock is not detected. |
| Graceful legs | 30054 | N+ID, M+ID | No purchase hook | Skip shared name | Shared name, one recent matching ID required for N/M. Silent purchase/unlock is not detected. |
| Graceful gloves | 30057 | N+ID, M+ID | No purchase hook | Skip shared name | Shared name, one recent matching ID required for N/M. Silent purchase/unlock is not detected. |
| Graceful boots | 30060 | N+ID, M+ID | No purchase hook | Skip shared name | Shared name, one recent matching ID required for N/M. Silent purchase/unlock is not detected. |

<a id="creature-creation"></a>

### Creature Creation

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Tea flask | 25617 → 10859 | N, M | L: NPC | B | Explicit ID alias. |
| Plain satchel | 25618 → 10877 | N, M | L: NPC | B | Explicit ID alias. |
| Green satchel | 25619 → 10878 | N, M | L: NPC | B | Explicit ID alias. |
| Red satchel | 25620 → 10879 | N, M | L: NPC | B | Explicit ID alias. |
| Black satchel | 25621 → 10880 | N, M | L: NPC | B | Explicit ID alias. |
| Gold satchel | 25622 → 10881 | N, M | L: NPC | B | Explicit ID alias. |
| Rune satchel | 25623 → 10882 | N, M | L: NPC | B | Explicit ID alias. |

<a id="cyclopes"></a>

### Cyclopes

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Bronze defender | 8844 | N, M | L: NPC | B | - |
| Iron defender | 8845 | N, M | L: NPC | B | - |
| Steel defender | 8846 | N, M | L: NPC | B | - |
| Black defender | 8847 | N, M | L: NPC | B | - |
| Mithril defender | 8848 | N, M | L: NPC | B | - |
| Adamant defender | 8849 | N, M | L: NPC | B | - |
| Rune defender | 8850 | N, M | L: NPC | B | - |
| Dragon defender | 12954 | N, M | L: NPC | B | - |

<a id="forestry"></a>

### Forestry

TODO: Investigate forestry items

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Fox whistle | 28626 | N, M | L? (source-dependent) | B | Silent obtains need an item-bearing event. |
| Golden pheasant egg | 28663 | N, M | L? (source-dependent) | B | Silent obtains need an item-bearing event. |
| Lumberjack hat | 10941 | N, M | L? (source-dependent) | B | Silent obtains need an item-bearing event. |
| Lumberjack top | 10939 | N, M | L? (source-dependent) | B | Silent obtains need an item-bearing event. |
| Lumberjack legs | 10940 | N, M | L? (source-dependent) | B | Silent obtains need an item-bearing event. |
| Lumberjack boots | 10933 | N, M | L? (source-dependent) | B | Silent obtains need an item-bearing event. |
| Forestry hat | 28173 | N, M | L? (source-dependent) | B | Silent obtains need an item-bearing event. |
| Forestry top | 28169 | N, M | L? (source-dependent) | B | Silent obtains need an item-bearing event. |
| Forestry legs | 28171 | N, M | L? (source-dependent) | B | Silent obtains need an item-bearing event. |
| Forestry boots | 28175 | N, M | L? (source-dependent) | B | Silent obtains need an item-bearing event. |
| Twitcher's gloves | 28630 | N, M | L? (source-dependent) | B | Silent obtains need an item-bearing event. |
| Funky shaped log | 28138 | N, M | L? (source-dependent) | B | Silent obtains need an item-bearing event. |
| Log basket | 28140 | N, M | L? (source-dependent) | B | Silent obtains need an item-bearing event. |
| Log brace | 28146 | N, M | L? (source-dependent) | B | Silent obtains need an item-bearing event. |
| Clothes pouch blueprint | 28166 | N, M | L? (source-dependent) | B | Silent obtains need an item-bearing event. |
| Cape pouch | 28613 | N, M | L? (source-dependent) | B | Silent obtains need an item-bearing event. |
| Felling axe handle | 28177 | N, M | L? (source-dependent) | B | Silent obtains need an item-bearing event. |
| Pheasant hat | 28620 | N, M | L? (source-dependent) | B | Silent obtains need an item-bearing event. |
| Pheasant legs | 28622 | N, M | L? (source-dependent) | B | Silent obtains need an item-bearing event. |
| Pheasant boots | 28618 | N, M | L? (source-dependent) | B | Silent obtains need an item-bearing event. |
| Pheasant cape | 28616 | N, M | L? (source-dependent) | B | Silent obtains need an item-bearing event. |
| Petal garland | 28655 | N, M | L? (source-dependent) | B | Silent obtains need an item-bearing event. |
| Sturdy beehive parts | 28674 | N, M | L? (source-dependent) | B | Silent obtains need an item-bearing event. |

<a id="fossil-island-notes"></a>

### Fossil Island Notes

TODO: Check loot tracker claim

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Scribbled note | 21664 | N, M | None | B | Needs a recognized notification. |
| Partial note | 21666 | N, M | None | B | Needs a recognized notification. |
| Ancient note | 21668 | N, M | None | B | Needs a recognized notification. |
| Ancient writings | 21670 | N, M | None | B | Needs a recognized notification. |
| Experimental note | 21672 | N, M | None | B | Needs a recognized notification. |
| Paragraph of text | 21674 | N, M | None | B | Needs a recognized notification. |
| Musty smelling note | 21676 | N, M | None | B | Needs a recognized notification. |
| Hastily scrawled note | 21678 | N, M | None | B | Needs a recognized notification. |
| Old writing | 21680 | N, M | None | B | Needs a recognized notification. |
| Short note | 21682 | N, M | None | B | Needs a recognized notification. |

<a id="gloughs-experiments"></a>

### Glough's Experiments

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Zenyte shard | 19529 | N, M | L: NPC | B | - |
| Light frame | 19586 | N, M | L: NPC | B | - |
| Heavy frame | 19589 | N, M | L: NPC | B | - |
| Ballista limbs | 19592 | N, M | L: NPC | B | - |
| Monkey tail | 19610 | N, M | L: NPC | B | - |
| Ballista spring | 19601 | N, M | L: NPC | B | - |

<a id="hunter-guild"></a>

### Hunter Guild

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Quetzin | 28962 | N, M | L: sack / pet drop | B (pet) | - |
| Huntsman's kit | 29309 | N, M | L: hunter-guild sack | B | - |
| Guild hunter headwear | 29263 | N, M | L: hunter-guild sack | B | - |
| Guild hunter top | 29265 | N, M | L: hunter-guild sack | B | - |
| Guild hunter legs | 29267 | N, M | L: hunter-guild sack | B | - |
| Guild hunter boots | 29269 | N, M | L: hunter-guild sack | B | - |

<a id="lost-schematics"></a>

### Lost Schematics

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Salvaging station schematic | 32401 | N | None | B | - |
| Gale catcher schematic | 32402 | N | None | B | - |
| Eternal brazier schematic | 32403 | N | None | B | - |
| Rosewood cargo hold schematic | 32405 | N | None | B | - |
| Rosewood hull schematic | 32407 | N | None | B | - |
| Rosewood & cotton sails schematic | 32408 | N | None | B | - |
| Dragon helm schematic | 32409 | N | None | B | - |
| Dragon keel schematic | 32410 | N | None | B | - |
| Dragon salvaging hook schematic | 32404 | N | None | B | - |
| Dragon cannon schematic | 32406 | N | None | B | - |
| Ballistic attractor schematic | 33143 | N | None | B | - |
| Bosun's workbench schematic | 33423 | N | None | B | - |

<a id="monkey-backpacks"></a>

### Monkey Backpacks

TODO: may be possible to add a hook to the interface

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Karamjan monkey | 24862 | N | No unlock hook | B | No generic reclaim handler.  |
| Kruk jr | 24866 | N | No unlock hook | B | No generic reclaim handler.  |
| Maniacal monkey | 24864 | N | No unlock hook | B | No generic reclaim handler.  |
| Princely monkey | 24867 | N | No unlock hook | B | No generic reclaim handler.  |
| Skeleton monkey | 24865 | N | No unlock hook | B) | No generic reclaim handler.  |
| Zombie monkey | 24863 | N | No unlock hook | B | No generic reclaim handler.  |

<a id="motherlode-mine"></a>

### Motherlode Mine

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Coal bag | 25627 → 12019 | N, M | No purchase hook | B | Explicit ID alias. Silent purchase/unlock is not detected. |
| Gem bag | 25628 → 12020 | N, M | No purchase hook | B | Explicit ID alias. Silent purchase/unlock is not detected. |
| Prospector helmet | 12013 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Prospector jacket | 12014 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Prospector legs | 12015 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Prospector boots | 12016 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |

<a id="my-notes"></a>

### My Notes

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Ancient page | 11341 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11342 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11343 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11344 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11345 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11346 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11347 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11348 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11349 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11350 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11351 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11352 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11353 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11354 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11355 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11356 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11357 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11358 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11359 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11360 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11361 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11362 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11363 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11364 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11365 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Ancient page | 11366 | N+ID, M+ID | L: NPC | Skip shared name | Shared name, one recent matching ID required for N/M. |

<a id="ocean-encounters"></a>

### Ocean Encounters

These items do not appear in Loot Tracker and have no unique acquisition dialog.

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Tiny pearl | 31770 | N, M | None | B | - |
| Small pearl | 31773 | N, M | None | B | - |
| Shiny pearl | 31776 | N, M | None | B | - |
| Bright pearl | 31779 | N, M | None | B | - |
| Big pearl | 31782 | N, M | None | B | - |
| Huge pearl | 31785 | N, M | None | B | - |
| Enormous pearl | 31788 | N, M | None | B | - |
| Shimmering pearl | 31791 | N, M | None | B | - |
| Glistening pearl | 31794 | N, M | None | B | - |
| Brilliant pearl | 31797 | N, M | None | B | - |
| Radiant pearl | 31800 | N, M | None | B | - |

<a id="random-events"></a>

### Random Events

TODO: Random event items may trigger unique chat or NPC dialog messages

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Camo top | 6654 | N, M | None | B | Needs a recognized notification. |
| Camo bottoms | 6655 | N, M | None | B | Needs a recognized notification. |
| Camo helmet | 6656 | N, M | None | B | Needs a recognized notification. |
| Lederhosen top | 6180 | N, M | None | B | Needs a recognized notification. |
| Lederhosen shorts | 6181 | N, M | None | B | Needs a recognized notification. |
| Lederhosen hat | 6182 | N, M | None | B | Needs a recognized notification. |
| Zombie shirt | 7592 | N, M | None | B | Needs a recognized notification. |
| Zombie trousers | 7593 | N, M | None | B | Needs a recognized notification. |
| Zombie mask | 7594 | N, M | None | B | Needs a recognized notification. |
| Zombie gloves | 7595 | N, M | None | B | Needs a recognized notification. |
| Zombie boots | 7596 | N, M | None | B | Needs a recognized notification. |
| Mime mask | 3057 | N, M | None | B | Needs a recognized notification. |
| Mime top | 3058 | N, M | None | B | Needs a recognized notification. |
| Mime legs | 3059 | N, M | None | B | Needs a recognized notification. |
| Mime gloves | 3060 | N, M | None | B | Needs a recognized notification. |
| Mime boots | 3061 | N, M | None | B | Needs a recognized notification. |
| Frog token | 6183 | N, M | None | B | Needs a recognized notification. |
| Stale baguette | 20590 | N, M | None | B | Needs a recognized notification. |
| Beekeeper's hat | 25129 | N, M | None | B | Needs a recognized notification. |
| Beekeeper's top | 25131 | N, M | None | B | Needs a recognized notification. |
| Beekeeper's legs | 25133 | N, M | None | B | Needs a recognized notification. |
| Beekeeper's gloves | 25135 | N, M | None | B | Needs a recognized notification. |
| Beekeeper's boots | 25137 | N, M | None | B | Needs a recognized notification. |

<a id="revenants"></a>

### Revenants

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Viggora's chainmace (u) | 22542 | N, M | L: NPC | B | - |
| Craw's bow (u) | 22547 | N, M | L: NPC | B | - |
| Thammaron's sceptre (u) | 22552 | N, M | L: NPC | B | - |
| Amulet of avarice | 22557 | N, M | L: NPC | B | - |
| Bracelet of ethereum (uncharged) | 21817 | N, M | L: NPC | B | - |
| Ancient crystal | 21804 | N, M | L: NPC | B | - |
| Ancient relic | 22305 | N, M | L: NPC | B | - |
| Ancient effigy | 22302 | N, M | L: NPC | B | - |
| Ancient medallion | 22299 | N, M | L: NPC | B | - |
| Ancient statuette | 21813 | N, M | L: NPC | B | - |
| Ancient totem | 21810 | N, M | L: NPC | B | - |
| Ancient emblem | 21807 | N, M | L: NPC | B | - |
| Revenant cave teleport | 21802 | N, M | L: NPC | B | - |
| Revenant ether | 21820 | N, M | L: NPC | B | - |

<a id="rooftop-agility"></a>

### Rooftop Agility

Graceful items share names between regular, Brimhaven agility, and Colossal Wrym instances

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Mark of Grace | 11849 | N, M | No ground-pickup hook | B | - |
| Graceful hood | 11850 | N+ID, M+ID | No purchase hook | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Graceful cape | 11852 | N+ID, M+ID | No purchase hook | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Graceful top | 11854 | N+ID, M+ID | No purchase hook | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Graceful legs | 11856 | N+ID, M+ID | No purchase hook | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Graceful gloves | 11858 | N+ID, M+ID | No purchase hook | Skip shared name | Shared name, one recent matching ID required for N/M. |
| Graceful boots | 11860 | N+ID, M+ID | No purchase hook | Skip shared name | Shared name, one recent matching ID required for N/M. |

<a id="sailing-miscellaneous"></a>

### Sailing Miscellaneous

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Dragon metal sheet | 31996 | N, M | L: NPC | B |- |
| Dragon nails | 31406 | N, M | L: NPC | B |- |
| Dragon cannonball | 31916 | N, M | L: NPC | B |- |
| Echo pearl | 31946 | N, M | L: NPC | B |- |
| Swift albatross feather | 31952 | N, M | L: NPC | B |- |
| Narwhal horn | 31954 | N, M | L: NPC | B |- |
| Ray barbs | 31959 | N, M | L: NPC | B |- |
| Broken dragon hook | 31961 | N, M | L: NPC | B |- |
| Bottled storm | 31949 | N, M | L: NPC | B |- |
| Dragon cannon barrel | 32115 | N, M | L: NPC, salvage | B |- |
| Boat bottle (empty) | 31989 | N, M | L: salvage | B |- |
| Facility bottle (empty) | 33074 | N, M | L: salvage | B |- |

<a id="sea-treasures"></a>

### Sea Treasures

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Medallion fragment | 32388 | N+ID | None | Skip shared name | Does not appear in Loot Tracker, N needs one recent exact ID.  |
| Medallion fragment | 32389 | N+ID | None | Skip shared name | Does not appear in Loot Tracker, N needs one recent exact ID.  |
| Medallion fragment | 32390 | N+ID | None | Skip shared name | Does not appear in Loot Tracker, N needs one recent exact ID.  |
| Medallion fragment | 32391 | N+ID | None | Skip shared name | Does not appear in Loot Tracker, N needs one recent exact ID.  |
| Medallion fragment | 32392 | N+ID | None | Skip shared name | Does not appear in Loot Tracker, N needs one recent exact ID.  |
| Medallion fragment | 32393 | N+ID | None | Skip shared name | Does not appear in Loot Tracker, N needs one recent exact ID.  |
| Medallion fragment | 32394 | N+ID | None | Skip shared name | Does not appear in Loot Tracker, N needs one recent exact ID.  |
| Medallion fragment | 32395 | N+ID | None | Skip shared name | Does not appear in Loot Tracker, N needs one recent exact ID.  |
| Sailors' amulet (inert) | 32398 | N, M | L:  salvage | B |- |
| Rusty locket | 32863 | N, M | L:  salvage | B |- |
| Mouldy block | 32864 | N, M | L:  salvage | B |- |
| Dull knife | 32865 | N, M | L:  salvage | B |- |
| Broken compass | 32866 | N, M | L:  salvage | B |- |
| Rusty coin | 32867 | N, M | L:  salvage | B |- |
| Broken sextant | 32868 | N, M | L:  salvage | B |- |
| Mouldy doll | 32869 | N, M | L:  salvage | B |- |
| Smashed mirror | 32870 | N, M | L:  salvage | B |- |

<a id="shayzien-armour"></a>

### Shayzien Armour

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Shayzien gloves (1) | 13357 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |
| Shayzien boots (1) | 13358 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |
| Shayzien helm (1) | 13359 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |
| Shayzien greaves (1) | 13360 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |
| Shayzien platebody (1) | 13361 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |
| Shayzien gloves (2) | 13362 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |
| Shayzien boots (2) | 13363 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |
| Shayzien helm (2) | 13364 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |
| Shayzien greaves (2) | 13365 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |
| Shayzien platebody (2) | 13366 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |
| Shayzien gloves (3) | 13367 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |
| Shayzien boots (3) | 13368 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |
| Shayzien helm (3) | 13369 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |
| Shayzien greaves (3) | 13370 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |
| Shayzien platebody (3) | 13371 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |
| Shayzien gloves (4) | 13372 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |
| Shayzien boots (4) | 13373 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |
| Shayzien helm (4) | 13374 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |
| Shayzien greaves (4) | 13375 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |
| Shayzien platebody (4) | 13376 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |
| Shayzien gloves (5) | 13377 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |
| Shayzien boots (5) | 13378 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |
| Shayzien helm (5) | 13379 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |
| Shayzien greaves (5) | 13380 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |
| Shayzien body (5) | 13381 | N, M | L: combat only | B | Smithing has no loot/unique-message trigger, silent repeat smithing obtains are missed. |

<a id="shooting-stars"></a>

### Shooting Stars

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Celestial ring (uncharged) | 25539 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |
| Star fragment | 25547 | N, M | No purchase hook | B | Silent purchase/unlock is not detected. |

<a id="skilling-pets"></a>

### Skilling Pets

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Heron | 13320 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Rock golem | 13321 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Rock golem | 13321 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Beaver | 13322 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Baby chinchompa | 13324 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Giant Squirrel | 20659 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Tangleroot | 20661 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Rocky | 20663 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Rift guardian | 20665 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Soup | 31283 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |

<a id="slayer"></a>

### Slayer

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Crawling hand | 7975 | N, M | L: NPC | B | - |
| Cockatrice head | 7976 | N, M | L: NPC | B | - |
| Basilisk head | 7977 | N, M | L: NPC | B | - |
| Kurask head | 7978 | N, M | L: NPC | B | - |
| Abyssal head | 7979 | N, M | L: NPC | B | - |
| Imbued heart | 20724 | N, M | L: NPC | B | - |
| Eternal gem | 21270 | N, M | L: NPC | B | - |
| Dust battlestaff | 20736 | N, M | L: NPC | B | - |
| Mist battlestaff | 20730 | N, M | L: NPC | B | - |
| Abyssal whip | 4151 | N, M | L: NPC | B | - |
| Granite maul | 4153 | N, M | L: NPC | B | - |
| Mudskipper hat | 6665 | N, M | L: NPC | B | - |
| Flippers | 6666 | N, M | L: NPC | B | - |
| Brine sabre | 11037 | N, M | L: NPC | B | - |
| Leaf-bladed sword | 11902 | N, M | L: NPC | B | - |
| Necklace of Fangs | 34401 | N, M | L: NPC | B | - |
| Leaf-bladed battleaxe | 20727 | N, M | L: NPC | B | - |
| Black mask (10) | 8901 | N, M | L: NPC | B | - |
| Granite longsword | 21646 | N, M | L: NPC | B | - |
| Granite boots | 21643 | N, M | L: NPC | B | - |
| Wyvern visage | 21637 | N, M | L: NPC | B | - |
| Granite legs | 6809 | N, M | L: NPC | B | - |
| Granite helm | 10589 | N, M | L: NPC | B | - |
| Draconic visage | 11286 | N, M | L: NPC | B | - |
| Bronze boots | 4119 | N, M | L: NPC | B | - |
| Iron boots | 4121 | N, M | L: NPC | B | - |
| Steel boots | 4123 | N, M | L: NPC | B | - |
| Black boots | 4125 | N, M | L: NPC | B | - |
| Mithril boots | 4127 | N, M | L: NPC | B | - |
| Adamant boots | 4129 | N, M | L: NPC | B | - |
| Rune boots | 4131 | N, M | L: NPC | B | - |
| Dragon boots | 11840 | N, M | L: NPC | B | - |
| Abyssal dagger | 13265 | N, M | L: NPC | B | - |
| Uncharged trident | 11908 | N, M | L: NPC | B | - |
| Kraken tentacle | 12004 | N, M | L: NPC | B | - |
| Dark bow | 11235 | N, M | L: NPC | B | - |
| Occult necklace | 12002 | N, M | L: NPC | B | - |
| Dragon chainbody | 3140 | N, M | L: NPC | B | - |
| Dragon thrownaxe | 20849 | N, M | L: NPC | B | - |
| Dragon harpoon | 21028 | N, M | L: NPC | B | - |
| Dragon sword | 21009 | N, M | L: NPC | B | - |
| Dragon knife | 22804 | N, M | L: NPC | B | - |
| Broken dragon hasta | 22963 | N, M | L: NPC | B | - |
| Drake's tooth | 22960 | N, M | L: NPC | B | - |
| Drake's claw | 22957 | N, M | L: NPC | B | - |
| Hydra tail | 22988 | N, M | L: NPC | B | - |
| Hydra's fang | 22971 | N, M | L: NPC | B | - |
| Hydra's eye | 22973 | N, M | L: NPC | B | - |
| Hydra's heart | 22969 | N, M | L: NPC | B | - |
| Mystic hat (light) | 4109 | N, M | L: NPC | B | - |
| Mystic robe top (light) | 4111 | N, M | L: NPC | B | - |
| Mystic robe bottom (light) | 4113 | N, M | L: NPC | B | - |
| Mystic gloves (light) | 4115 | N, M | L: NPC | B | - |
| Mystic boots (light) | 4117 | N, M | L: NPC | B | - |
| Mystic hat (dark) | 4099 | N, M | L: NPC | B | - |
| Mystic robe top (dark) | 4101 | N, M | L: NPC | B | - |
| Mystic robe bottom (dark) | 4103 | N, M | L: NPC | B | - |
| Mystic gloves (dark) | 4105 | N, M | L: NPC | B | - |
| Mystic boots (dark) | 4107 | N, M | L: NPC | B | - |
| Mystic hat (dusk) | 23047 | N, M | L: NPC | B | - |
| Mystic robe top (dusk) | 23050 | N, M | L: NPC | B | - |
| Mystic robe bottom (dusk) | 23053 | N, M | L: NPC | B | - |
| Mystic gloves (dusk) | 23056 | N, M | L: NPC | B | - |
| Mystic boots (dusk) | 23059 | N, M | L: NPC | B | - |
| Basilisk jaw | 24268 | N, M | L: NPC | B | - |
| Aquanite tendon | 32876 | N, M | L: NPC | B | - |
| Dagon'hai hat | 24288 | N, M | L: NPC | B | - |
| Dagon'hai robe top | 24291 | N, M | L: NPC | B | - |
| Dagon'hai robe bottom | 24294 | N, M | L: NPC | B | - |
| Blood shard | 24777 | N, M | L: NPC | B | - |
| Ancient ceremonial mask | 26225 | N, M | L: NPC | B | - |
| Ancient ceremonial top | 26221 | N, M | L: NPC | B | - |
| Ancient ceremonial legs | 26223 | N, M | L: NPC | B | - |
| Ancient ceremonial gloves | 26227 | N, M | L: NPC | B | - |
| Ancient ceremonial boots | 26229 | N, M | L: NPC | B | - |
| Warped sceptre (uncharged) | 28583 | N, M | L: NPC | B | - |
| Sulphur blades | 29084 | N, M | L: NPC | B | - |
| Teleport anchoring scroll | 29455 | N, M | L: NPC | B | - |
| Aranea boots | 29806 | N, M | L: NPC | B | - |
| Glacial temotli | 29889 | N, M | L: NPC | B | - |
| Pendant of Ates (inert) | 29892 | N, M | L: NPC | B | - |
| Frozen tear | 29895 | N, M | L: NPC | B | - |
| Earthbound tecpatl | 30957 | N, M | L: NPC | B | - |
| Antler guard | 31081 | N, M | L: NPC | B | - |
| Alchemist's signet | 31084 | N, M | L: NPC | B | - |
| Broken antler | 31086 | N, M | L: NPC | B | - |
| Dragon metal sheet | 31996 | N, M | L: NPC | B | - |
| Horn of Plenty (empty) | 31243 | N, M | L: NPC | B | - |
| Gryphon feather | 31235 | N, M | L: NPC | B | - |
| Venator tooth | 33663 | N, M | L: NPC | B | - |
| Venator fang | 33661 | N, M | L: NPC | B | - |
| Air diamond | 34404 | N, M | L: NPC | B | - |
| Water sapphire | 34410 | N, M | L: NPC | B | - |
| Earth emerald | 34416 | N, M | L: NPC | B | - |
| Fire ruby | 34422 | N, M | L: NPC | B | - |

<a id="tormented-demons"></a>

### Tormented Demons

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Tormented synapse | 29580 | N, M | L: NPC | B | - |
| Burning claw | 29574 | N, M | L: NPC | B | - |
| Guthixian temple teleport | 29684 | N, M | L: NPC | B | - |

<a id="tzhaar"></a>

### TzHaar

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Obsidian cape | 6568 | N, M | L: NPC | B | - |
| Toktz-ket-xil | 6524 | N, M | L: NPC | B | - |
| Tzhaar-ket-om | 6528 | N, M | L: NPC | B | - |
| Toktz-xil-ak | 6523 | N, M | L: NPC | B | - |
| Toktz-xil-ek | 6525 | N, M | L: NPC | B | - |
| Toktz-mej-tal | 6526 | N, M | L: NPC | B | - |
| Toktz-xil-ul | 6522 | N, M | L: NPC | B | - |
| Obsidian helmet | 21298 | N, M | L: NPC | B | NPC loot grants collection credit, shop purchases do not. |
| Obsidian platebody | 21301 | N, M | L: NPC | B | NPC loot grants collection credit, shop purchases do not. |
| Obsidian platelegs | 21304 | N, M | L: NPC | B | NPC loot grants collection credit, shop purchases do not. |

<a id="miscellaneous"></a>

### Miscellaneous

| Item | Log game ID → submitted ID | Personal notification | Loot event (first or repeat) | Other member broadcast | Notes |
| --- | --- | --- | --- | --- | --- |
| Herbi | 21509 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Chompy chick | 13071 | N, M | L: pet drop | B (pet) | Pet loot first/repeat pet clan broadcasts. |
| Dragon warhammer | 13576 | N, M | L: NPC | B | NPC only. |
| Big swordfish | 7991 | N, M, U | No loot hook | B | Specific fish catch message is parsed, Orangefin accepts a/an. |
| Big shark | 7993 | N, M, U | No loot hook | B | Specific fish catch message is parsed, Orangefin accepts a/an. |
| Big bass | 7989 | N, M, U | No loot hook | B | Specific fish catch message is parsed, Orangefin accepts a/an. |
| Giant blue krill | 31408 | N, M, U | No loot hook | B | Specific fish catch message is parsed, Orangefin accepts a/an. |
| Golden haddock | 31412 | N, M, U | No loot hook | B | Specific fish catch message is parsed, Orangefin accepts a/an. |
| Orangefin | 31416 | N, M, U | No loot hook | B | Specific fish catch message is parsed, Orangefin accepts a/an. |
| Huge halibut | 31420 | N, M, U | No loot hook | B | Specific fish catch message is parsed, Orangefin accepts a/an. |
| Purplefin | 31424 | N, M, U | No loot hook | B | Specific fish catch message is parsed, Orangefin accepts a/an. |
| Swift marlin | 31428 | N, M, U | No loot hook | B | Specific fish catch message is parsed, Orangefin accepts a/an. |
| Long bone | 10976 | N, M | L: item reward | B |- |
| Curved bone | 10977 | N, M | L: item reward | B |- |
| Ecumenical key | 11942 | N, M | L: item reward | B |- |
| Pharaoh's sceptre (uncharged) | 26945 | N, M, D | No loot hook | B | Dialog prefix You find a golden sceptre, continuation may vary. |
| Dark totem base | 19679 | N, M | L: item reward | B |- |
| Dark totem middle | 19681 | N, M | L: item reward | B |- |
| Dark totem top | 19683 | N, M | L: item reward | B |- |
| Chewed bones | 11338 | N, M | L: item reward | B |- |
| Dragon full helm | 11335 | N, M | L: item reward | B |- |
| Shield left half | 2366 | N, M | L: item reward | B |- |
| Dragon metal slice | 22100 | N, M | L: item reward | B |- |
| Dragon metal lump | 22103 | N, M | L: item reward | B |- |
| Dragon limbs | 21918 | N, M | L: item reward | B |- |
| Dragon spear | 1249 | N, M | L: item reward | B |- |
| Amulet of eternal glory | 19707 | N, M | None | B | No Loot Tracker or specific acquisition-message route, needs N/M if emitted. |
| Shaman mask | 21838 | N, M | L: item reward | B |- |
| Evil chicken head | 20439 | N, M | L: item reward | B |- |
| Evil chicken wings | 20436 | N, M | L: item reward | B |- |
| Evil chicken legs | 20442 | N, M | L: item reward | B |- |
| Evil chicken feet | 20433 | N, M | L: item reward | B |- |
| Mining gloves | 21343 | N, M | No purchase hook | B | No Loot Tracker or specific acquisition-message route, needs N/M if emitted. |
| Superior mining gloves | 21345 | N, M | No purchase hook | B | No Loot Tracker or specific acquisition-message route, needs N/M if emitted. |
| Expert mining gloves | 21392 | N, M, D | No loot hook | B | Specific NPC combination dialog. |
| Right skull half | 9007 | N, M | L: item reward | B |- |
| Left skull half | 9008 | N, M | L: item reward | B |- |
| Top of sceptre | 9010 | N, M | L: item reward | B |- |
| Bottom of sceptre | 9011 | N, M | L: item reward | B |- |
| Mossy key | 22374 | N, M | L: item reward | B |- |
| Giant key | 20754 | N, M | L: item reward | B |- |
| Hespori seed | 22875 | N, M, U | No loot hook | B | Specific You find a Hespori seed. game message. |
| Fresh crab claw | 7536 | N, M | L: item reward | B |- |
| Fresh crab shell | 7538 | N, M | L: item reward | B |- |
| Xeric's talisman (inert) | 13392 | N, M | L: item reward | B |- |
| Mask of Ranul | 23522 | N, M | L: item reward | B |- |
| Elven signet | 23943 | N, M | L: item reward | B |- |
| Crystal grail | 24000 | N, M | L: item reward | B |- |
| Enhanced crystal teleport seed | 23959 | N, M | L: item reward | B |- |
| Dragonstone full helm | 24034 | N, M | L: item reward | B |- |
| Dragonstone platebody | 24037 | N, M | L: item reward | B |- |
| Dragonstone platelegs | 24040 | N, M | L: item reward | B |- |
| Dragonstone gauntlets | 24046 | N, M | L: item reward | B |- |
| Dragonstone boots | 24043 | N, M | L: item reward | B |- |
| Uncut onyx | 6571 | N, M | L: item reward | B |- |
| Merfolk trident | 21649 | N, M | None | B | No Loot Tracker or specific acquisition-message route, needs N/M if emitted. |
| Orange egg sac | 25844 | N, M | L: item reward | B |- |
| Blue egg sac | 25846 | N, M | L: item reward | B |- |
| Broken zombie axe | 28813 | N, M | L: item reward | B |- |
| Broken zombie helmet | 30324 | N, M | L: item reward | B |- |
| Helmet of the moon | 30111 | N, M | L: item reward | B |- |
| Squid beak | 31572 | N, M | None | B | No Loot Tracker or specific acquisition-message route, needs N/M if emitted. |
| Jeweller's chisel | 34024 | N, M | L: item reward | B |- |

## Specific acquisition triggers

U is restricted to GAMEMESSAGE/SPAM and the local recipient.
D reads known reward-dialog widgets and trusted DIALOG/MESBOX events.
Widget text is checked on load, after onload scripts on the next client cycle, and on game ticks.

| Item or reward | Accepted triggering text or context |
| --- | --- |
| Golden tench | The cormorant has brought you a very strange tench. / The cormorant is about to eat a strange tench before you quickly yank it out of its mouth. |
| Barronite head | There's a Barronite Head inside! |
| Barronite handle | You fished out a Barronite Handle! |
| Barronite mace | Ramarno hands you the Barronite Mace. |
| Imcando hammer | Ramarno hands you the Imcando Hammer. |
| Gnome scarf | You are given a gnome pilot's scarf as a tip. |
| Gnome goggles | You are given a pair of pilot's goggles as a tip. |
| Mint cake | You are given a mint cake as a tip. |
| Grand seed pod | You are given some seed pods as a tip. |
| Big shark / bass / swordfish | You catch an enormous shark! / You catch an enormous bass! / You catch an enormous swordfish! |
| Giant blue krill / Golden haddock | You catch a giant blue krill! / You catch a golden haddock! |
| Orangefin | You catch a orangefin! / You catch an orangefin! |
| Huge halibut / Purplefin / Swift marlin | You catch a huge halibut! / You catch a purplefin! / You catch a swift marlin! |
| Hespori seed | You find a Hespori seed. |
| Pharaoh's sceptre (uncharged) | Dialog starts with You find a golden sceptre, no particular continuation is required. |
| Expert mining gloves | There you go. I've combined your gloves into some Expert mining gloves. Enjoy. |
| Agility arena tickets / Brimhaven vouchers | Your Agility Arena lap count is: &lt,number&gt |
| Spirit Angler pieces | Gita Prymes wants 1,200 spirit flakes to upgrade your &lt,outfit piece&gt,. Spirit Angler kit gives the same Fishing XP bonus as the regular Angler kit. If you change it back later, you will not get your spirit flakes back. Requires the completed conversion's inventory changes. |
| Gauntlet | &lt,local player&gt, has received a drop: &lt,number&gt, x &lt,item name&gt,. |
| First pet clan broadcast | Optional [clan name] prefix, then &lt,player&gt, has/have a funny feeling like he's/she's/they're being followed: &lt,pet name&gt, at &lt,number&gt, killcount. |
| Repeat pet clan broadcast | Optional [clan name] prefix, then &lt,player&gt, has/have a funny feeling like he/she/they would have been followed: &lt,pet name&gt, at &lt,number&gt, killcount. |

## Explicit ID normalization for display only IDs

Display ID → obtainable item ID.
This table requires maintenance as new items are added to the colleciton log.

| Alias/display ID | Submitted ID | Submitted item name |
| --- | --- | --- |
| 29472 | 12013 | Prospector helmet |
| 29474 | 12014 | Prospector jacket |
| 29476 | 12015 | Prospector legs |
| 29478 | 12016 | Prospector boots |
| 25617 | 10859 | Tea flask |
| 25618 | 10877 | Plain satchel |
| 25619 | 10878 | Green satchel |
| 25620 | 10879 | Red satchel |
| 25621 | 10880 | Black satchel |
| 25622 | 10881 | Gold satchel |
| 25623 | 10882 | Rune satchel |
| 25624 | 13273 | Unsired |
| 25627 | 12019 | Coal bag |
| 25628 | 12020 | Gem bag |
| 25629 | 24882 | Plank sack |
| 25630 | 12854 | Flamtaer bag |
| 29992 | 29990 | Alchemist's amulet |
| 30805 | 30803 | Dossier |
