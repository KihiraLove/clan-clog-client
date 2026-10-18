package com.clanclogclient;

import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class RewardMessageParserTest
{
	private final RewardMessageParser parser = new RewardMessageParser();

	@Test
	public void detectsBothTenchMessagesAndBarroniteFinds()
	{
		assertEquals(Integer.valueOf(ItemID.GOLDEN_TENCH), parser.chatItem("The cormorant has brought you a very strange tench."));
		assertEquals(Integer.valueOf(ItemID.GOLDEN_TENCH), parser.chatItem("The cormorant is about to eat a strange tench before you quickly yank it out of its mouth."));
		assertEquals(Integer.valueOf(ItemID.BARRONITE_MACE_1), parser.chatItem("There's a Barronite Head inside!"));
		assertEquals(Integer.valueOf(ItemID.BARRONITE_MACE_2), parser.chatItem("You fished out a Barronite Handle!"));
		assertEquals(Integer.valueOf(ItemID.HESPORI_SEED), parser.chatItem("You find a <col=ff0000>Hespori seed</col>."));
	}

	@Test
	public void detectsEveryTrophyFishAndBothOrangefinArticles()
	{
		String[] texts = {"an enormous shark", "an enormous bass", "an enormous swordfish", "a giant blue krill",
			"a golden haddock", "a orangefin", "an orangefin", "a huge halibut", "a purplefin", "a swift marlin"};
		int[] ids = {ItemID.POH_TROPHYDROP_SHARK, ItemID.POH_TROPHYDROP_BASS, ItemID.POH_TROPHYDROP_SWORDFISH,
			ItemID.POH_TROPHYDROP_GIANT_KRILL, ItemID.POH_TROPHYDROP_HADDOCK, ItemID.POH_TROPHYDROP_YELLOWFIN,
			ItemID.POH_TROPHYDROP_YELLOWFIN, ItemID.POH_TROPHYDROP_HALIBUT, ItemID.POH_TROPHYDROP_BLUEFIN, ItemID.POH_TROPHYDROP_MARLIN};
		for (int i = 0; i < texts.length; i++)
		{
			assertEquals(texts[i], Integer.valueOf(ids[i]), parser.chatItem("You catch " + texts[i] + "!"));
		}
		assertNull(parser.chatItem("You catch a shark!"));
	}

	@Test
	public void detectsGnomeTipsAssemblyGlovesAndSceptreDialogPrefixes()
	{
		assertEquals(Integer.valueOf(ItemID.ALUFT_GNOME_SCARF), parser.dialogItem("You are given a gnome pilot's scarf as a tip."));
		assertEquals(Integer.valueOf(ItemID.ALUFT_GNOME_GOGGLES), parser.dialogItem("You are given a pair of pilot's goggles as a tip."));
		assertEquals(Integer.valueOf(ItemID.ALUFT_GNOME_MINT_CAKE), parser.dialogItem("You are given a mint cake as a tip."));
		assertEquals(Integer.valueOf(ItemID.ALUFT_SEED_POD), parser.dialogItem("You are given some seed pods as a tip."));
		assertEquals(Integer.valueOf(ItemID.BARRONITE_MACE), parser.dialogItem("Ramarno hands you the Barronite Mace."));
		assertEquals(Integer.valueOf(ItemID.IMCANDO_HAMMER), parser.dialogItem("Ramarno hands you the Imcando Hammer."));
		assertEquals(Integer.valueOf(ItemID.MGUILD_GLOVES_EXPERT), parser.dialogItem("There you go. I've combined your gloves into some Expert mining gloves. Enjoy."));
		assertEquals(Integer.valueOf(ItemID.PHARAOHS_SCEPTRE), parser.dialogItem("You find a golden sceptre"));
		assertEquals(Integer.valueOf(ItemID.PHARAOHS_SCEPTRE), parser.dialogItem("You find a golden sceptre<br>and an additional line."));
		assertNull(parser.dialogItem("You might find a golden sceptre."));
		assertNull(parser.dialogItem("You find a golden sceptre-shaped stone."));
	}

	@Test
	public void recognizesArenaLapEvidenceButNotAnItemClaim()
	{
		assertTrue(parser.isArenaLap("Your Agility Arena lap count is: 1,234"));
		assertFalse(parser.isArenaLap("Your Agility Arena lap count is: unknown"));
		assertNull(parser.chatItem("Your Agility Arena lap count is: 1,234"));
	}

	@Test
	public void identifiesEachAnglerUpgradeFromTheConfirmation()
	{
		String[] pieces = {"angler hat", "angler top", "angler waders", "angler boots"};
		int[] expected = {ItemID.SPIRIT_ANGLER_HAT, ItemID.SPIRIT_ANGLER_TOP, ItemID.SPIRIT_ANGLER_LEGS, ItemID.SPIRIT_ANGLER_BOOTS};
		for (int i = 0; i < pieces.length; i++)
		{
			assertEquals(Integer.valueOf(expected[i]), parser.anglerUpgrade(confirmation(pieces[i])));
		}
		assertNull(parser.anglerUpgrade("Proceed"));
		assertNull(parser.anglerUpgrade("Cancel"));
		assertNull(parser.anglerUpgrade(confirmation("Abyssal whip")));
	}

	static String confirmation(String piece)
	{
		return "Gita Prymes wants 1,200 spirit flakes to upgrade your " + piece
			+ ". Spirit Angler kit gives the same Fishing XP bonus as the regular Angler kit. If you change it back later, you will not get your spirit flakes back.";
	}
}
