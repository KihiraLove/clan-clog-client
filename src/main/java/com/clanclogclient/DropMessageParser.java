package com.clanclogclient;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Value;
import net.runelite.api.ChatMessageType;
import net.runelite.client.util.Text;

final class DropMessageParser
{
	private static final Pattern LOCAL_COLLECTION = Pattern.compile("^New item added to your collection log: (.+)$");
	private static final Pattern LOCAL_DROP = Pattern.compile("^(?:Valuable|Untradeable) drop: (.+?)(?: \\([\\d,]+ coins\\))?$");
	private static final Pattern CLAN_COLLECTION = Pattern.compile("^(.+?) received a new collection log item: (.+?)(?: \\([\\d,]+/[\\d,]+\\))?[!.]?$");
	private static final Pattern CLAN_DROP = Pattern.compile("^(.+?) (?:received a (?:valuable )?drop|received special loot from a raid): (.+?) \\([\\d,]+(?: coins)?\\)(?: from .+)?[!.]?$");
	private static final Pattern CLAN_PET = Pattern.compile("^(?:\\[[^\\]]+\\]\\s*)?(.+?) (?:has|have) a funny feeling like (?:(?:he['’]s|she['’]s|they['’]re) being followed|(?:he|she|they) would have been followed): (.+?) at [\\d,]+ killcount[!.]?$");
	private static final Pattern GAUNTLET_DROP = Pattern.compile("^(.+?) has received a drop: ([\\d,]+ x .+?)[!.]?$");
	private static final Pattern QUANTITY = Pattern.compile("^[\\d,]+ x (.+)$");

	Optional<Drop> parse(ChatMessageType type, String message, String localUsername)
	{
		String clean = clean(message);
		if (type == ChatMessageType.CLAN_MESSAGE)
		{
			Matcher pet = CLAN_PET.matcher(clean);
			if (pet.matches())
			{
				return drop(pet.group(1), pet.group(2), true, message);
			}
			Matcher collection = CLAN_COLLECTION.matcher(clean);
			if (collection.matches())
			{
				return drop(collection.group(1), collection.group(2), true, message);
			}
			Matcher valuable = CLAN_DROP.matcher(clean);
			return valuable.matches() ? drop(valuable.group(1), withoutQuantity(valuable.group(2)), true, message) : Optional.empty();
		}
		if (type != ChatMessageType.GAMEMESSAGE && type != ChatMessageType.SPAM)
		{
			return Optional.empty();
		}
		Matcher gauntlet = GAUNTLET_DROP.matcher(clean);
		if (gauntlet.matches())
		{
			return identity(gauntlet.group(1)).equals(identity(localUsername))
				? drop(localUsername, withoutQuantity(gauntlet.group(2)), false, message) : Optional.empty();
		}
		Matcher collection = LOCAL_COLLECTION.matcher(clean);
		if (collection.matches())
		{
			return drop(localUsername, collection.group(1), false, message);
		}
		Matcher loot = LOCAL_DROP.matcher(clean);
		return loot.matches() ? drop(localUsername, withoutQuantity(loot.group(1)), false, message) : Optional.empty();
	}

	static String clean(String value)
	{
		return value == null ? "" : Text.removeTags(value).replace('\u00a0', ' ').trim();
	}

	static String identity(String value)
	{
		return clean(value).replace('_', ' ').replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT);
	}

	private static String withoutQuantity(String item)
	{
		Matcher matcher = QUANTITY.matcher(item);
		return matcher.matches() ? matcher.group(1) : item;
	}

	private static Optional<Drop> drop(String username, String itemName, boolean broadcast, String chatMessage)
	{
		String name = clean(username);
		return name.isEmpty() ? Optional.empty() : Optional.of(new Drop(name, clean(itemName), broadcast, chatMessage));
	}

	@Value
	static class Drop
	{
		String username;
		String itemName;
		boolean broadcast;
		String chatMessage;
	}
}
