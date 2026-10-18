package com.clanclogclient;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.gameval.ItemID;

final class RewardMessageParser
{
	private static final Map<String, Integer> CHAT_ITEMS = Map.ofEntries(
		Map.entry("The cormorant has brought you a very strange tench.", ItemID.GOLDEN_TENCH),
		Map.entry("The cormorant is about to eat a strange tench before you quickly yank it out of its mouth.", ItemID.GOLDEN_TENCH),
		Map.entry("There's a Barronite Head inside!", ItemID.BARRONITE_MACE_1),
		Map.entry("You fished out a Barronite Handle!", ItemID.BARRONITE_MACE_2),
		Map.entry("You catch an enormous shark!", ItemID.POH_TROPHYDROP_SHARK),
		Map.entry("You catch an enormous bass!", ItemID.POH_TROPHYDROP_BASS),
		Map.entry("You catch an enormous swordfish!", ItemID.POH_TROPHYDROP_SWORDFISH),
		Map.entry("You catch a giant blue krill!", ItemID.POH_TROPHYDROP_GIANT_KRILL),
		Map.entry("You catch a golden haddock!", ItemID.POH_TROPHYDROP_HADDOCK),
		Map.entry("You catch a orangefin!", ItemID.POH_TROPHYDROP_YELLOWFIN),
		Map.entry("You catch an orangefin!", ItemID.POH_TROPHYDROP_YELLOWFIN),
		Map.entry("You catch a huge halibut!", ItemID.POH_TROPHYDROP_HALIBUT),
		Map.entry("You catch a purplefin!", ItemID.POH_TROPHYDROP_BLUEFIN),
		Map.entry("You catch a swift marlin!", ItemID.POH_TROPHYDROP_MARLIN),
		Map.entry("You find a Hespori seed.", ItemID.HESPORI_SEED)
	);
	private static final Map<String, Integer> DIALOG_ITEMS = Map.ofEntries(
		Map.entry("You are given a gnome pilot's scarf as a tip.", ItemID.ALUFT_GNOME_SCARF),
		Map.entry("You are given a pair of pilot's goggles as a tip.", ItemID.ALUFT_GNOME_GOGGLES),
		Map.entry("You are given a mint cake as a tip.", ItemID.ALUFT_GNOME_MINT_CAKE),
		Map.entry("You are given some seed pods as a tip.", ItemID.ALUFT_SEED_POD),
		Map.entry("Ramarno hands you the Barronite Mace.", ItemID.BARRONITE_MACE),
		Map.entry("Ramarno hands you the Imcando Hammer.", ItemID.IMCANDO_HAMMER),
		Map.entry("There you go. I've combined your gloves into some Expert mining gloves. Enjoy.", ItemID.MGUILD_GLOVES_EXPERT)
	);
	private static final Pattern ARENA_LAP = Pattern.compile("^Your Agility Arena lap count is: [\\d,]+[.!]?$");
	private static final Pattern ANGLER_UPGRADE = Pattern.compile("^Gita Prymes wants 1,200 spirit flakes to upgrade your (.+?)\\. Spirit Angler kit gives the same Fishing XP bonus as the regular Angler kit\\. If you change it back later, you will not get your spirit flakes back\\.$", Pattern.CASE_INSENSITIVE);

	Integer chatItem(String raw)
	{
		return CHAT_ITEMS.get(text(raw));
	}

	Integer dialogItem(String raw)
	{
		String text = text(raw);
		if (text.matches("^You find a golden sceptre(?:$|[\\s.!,:].*)"))
		{
			return ItemID.PHARAOHS_SCEPTRE;
		}
		return DIALOG_ITEMS.get(text);
	}

	boolean isArenaLap(String raw)
	{
		return ARENA_LAP.matcher(text(raw)).matches();
	}

	Integer anglerUpgrade(String raw)
	{
		Matcher match = ANGLER_UPGRADE.matcher(text(raw));
		if (!match.matches())
		{
			return null;
		}
		switch (match.group(1).toLowerCase(Locale.ROOT).replaceFirst("^angler(?:'s)? ", ""))
		{
			case "hat":
			case "headband": return ItemID.SPIRIT_ANGLER_HAT;
			case "top": return ItemID.SPIRIT_ANGLER_TOP;
			case "waders":
			case "legs": return ItemID.SPIRIT_ANGLER_LEGS;
			case "boots": return ItemID.SPIRIT_ANGLER_BOOTS;
			default: return null;
		}
	}

	static int anglerInput(int spiritItem)
	{
		switch (spiritItem)
		{
			case ItemID.SPIRIT_ANGLER_HAT: return ItemID.TRAWLER_REWARD_HAT;
			case ItemID.SPIRIT_ANGLER_TOP: return ItemID.TRAWLER_REWARD_TOP;
			case ItemID.SPIRIT_ANGLER_LEGS: return ItemID.TRAWLER_REWARD_LEGS;
			case ItemID.SPIRIT_ANGLER_BOOTS: return ItemID.TRAWLER_REWARD_BOOTS;
			default: throw new IllegalArgumentException("Not a Spirit Angler item");
		}
	}

	private static String text(String raw)
	{
		return DropMessageParser.clean(raw == null ? "" : raw.replaceAll("(?i)<br\\s*/?>", " "))
			.replace('’', '\'').replaceAll("\\s+", " ");
	}
}
