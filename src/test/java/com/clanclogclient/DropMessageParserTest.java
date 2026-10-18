package com.clanclogclient;

import net.runelite.api.ChatMessageType;
import org.junit.Test;
import static org.junit.Assert.*;

public class DropMessageParserTest
{
	private final DropMessageParser parser = new DropMessageParser();

	@Test
	public void parsesLocalCollectionAndUntradeableMessages()
	{
		assertDrop(ChatMessageType.GAMEMESSAGE, "New item added to your collection log: <col=ff0000>Abyssal whip</col>", "Abyssal whip", "Local Player", false);
		assertDrop(ChatMessageType.SPAM, "Untradeable drop: Abyssal blue dye", "Abyssal blue dye", "Local Player", false);
		assertDrop(ChatMessageType.GAMEMESSAGE, "Valuable drop: 2 x Abyssal whip (3,456,789 coins)", "Abyssal whip", "Local Player", false);
		assertDrop(ChatMessageType.GAMEMESSAGE, "Untradeable drop: Sailors' amulet (inert)", "Sailors' amulet (inert)", "Local Player", false);
	}

	@Test
	public void extractsBroadcastRecipientInsteadOfLocalPlayer()
	{
		assertDrop(ChatMessageType.CLAN_MESSAGE, "<img=2>Other\u00a0Player received a new collection log item: Abyssal whip (483/1,717)", "Abyssal whip", "Other Player", true);
		assertDrop(ChatMessageType.CLAN_MESSAGE, "Other Player received a drop: Hill giant club (1,117,561) from Obor", "Hill giant club", "Other Player", true);
		assertDrop(ChatMessageType.CLAN_MESSAGE, "Other Player received a drop: 2 x Abyssal whip (3,000,000 coins).", "Abyssal whip", "Other Player", true);
		assertDrop(ChatMessageType.CLAN_MESSAGE, "Other Player received a new collection log item: Sailors' amulet (inert)!", "Sailors' amulet (inert)", "Other Player", true);
	}

	@Test
	public void parsesRaidBroadcastFromUserLog()
	{
		assertDrop(ChatMessageType.CLAN_MESSAGE,
			"Nerd\u00a0Stark received special loot from a raid: Scythe of Vitur (uncharged) (1,188,452,005 coins).",
			"Scythe of Vitur (uncharged)", "Nerd Stark", true);
		assertDrop(ChatMessageType.CLAN_MESSAGE,
			"Other Player received special loot from a raid: 2 x Ancient tablet (0 coins).",
			"Ancient tablet", "Other Player", true);
	}

	@Test
	public void ignoresPlayerChatGuestsAndUnrelatedSystemMessages()
	{
		String spoof = "Other Player received a new collection log item: Abyssal whip";
		for (ChatMessageType type : new ChatMessageType[]{ChatMessageType.CLAN_CHAT, ChatMessageType.PUBLICCHAT,
			ChatMessageType.CLAN_GUEST_CHAT, ChatMessageType.CLAN_GUEST_MESSAGE, ChatMessageType.FRIENDSCHAT})
		{
			assertFalse(parser.parse(type, spoof, "Local Player").isPresent());
		}
		assertFalse(parser.parse(ChatMessageType.GAMEMESSAGE, "You buy an Abyssal whip.", "Local Player").isPresent());
		assertFalse(parser.parse(ChatMessageType.GAMEMESSAGE, "Untradeable drop: Abyssal blue dye", "").isPresent());
		assertFalse(parser.parse(ChatMessageType.CLAN_MESSAGE, "Other Player has reached level 99 in Attack.", "Local Player").isPresent());
	}

	@Test
	public void parsesFirstAndRepeatPetBroadcastsWithAllPronouns()
	{
		for (String pronoun : new String[]{"he's", "she's", "they're", "he’s", "she’s", "they’re"})
		{
			assertDrop(ChatMessageType.CLAN_MESSAGE, "[Our Clan] Other\u00a0Player has a funny feeling like "
				+ pronoun + " being followed: Pet Penance Queen at 1,234 killcount.", "Pet Penance Queen", "Other Player", true);
		}
		for (String pronoun : new String[]{"he", "she", "they"})
		{
			assertDrop(ChatMessageType.CLAN_MESSAGE, "Other Player have a funny feeling like "
				+ pronoun + " would have been followed: Yami at 2 killcount.", "Yami", "Other Player", true);
		}
	}

	@Test
	public void gauntletLobbyMessagesOnlyIdentifyTheLocalRecipient()
	{
		assertDrop(ChatMessageType.GAMEMESSAGE, "Local\u00a0Player has received a drop: 1 x Crystal armour seed.",
			"Crystal armour seed", "Local Player", false);
		assertDrop(ChatMessageType.SPAM, "Local Player has received a drop: 1,234 x Chaos rune.",
			"Chaos rune", "Local Player", false);
		assertFalse(parser.parse(ChatMessageType.GAMEMESSAGE,
			"Other Player has received a drop: 1 x Crystal armour seed.", "Local Player").isPresent());
		assertFalse(parser.parse(ChatMessageType.PUBLICCHAT,
			"Local Player has received a drop: 1 x Crystal armour seed.", "Local Player").isPresent());
		assertFalse(parser.parse(ChatMessageType.CLAN_CHAT,
			"Other Player has a funny feeling like he's being followed: Yami at 1 killcount.", "Local Player").isPresent());
	}

	@Test
	public void normalizesIdentity()
	{
		assertEquals("other player", DropMessageParser.identity(" <img=1>OTHER_\u00a0Player "));
	}

	private void assertDrop(ChatMessageType type, String text, String item, String recipient, boolean broadcast)
	{
		DropMessageParser.Drop drop = parser.parse(type, text, "Local Player").orElseThrow(AssertionError::new);
		assertEquals(item, drop.getItemName());
		assertEquals(recipient, drop.getUsername());
		assertEquals(broadcast, drop.isBroadcast());
		assertEquals(text, drop.getChatMessage());
	}
}
