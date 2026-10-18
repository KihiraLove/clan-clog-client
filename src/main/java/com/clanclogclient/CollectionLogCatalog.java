package com.clanclogclient;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.ItemComposition;
import net.runelite.api.StructComposition;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;

class CollectionLogCatalog
{
	static final int COLLECTION_TABS = 2102;
	static final int TAB_PAGES = 683;
	static final int PAGE_ITEMS = 690;
	// Normalize display only and alternate IDs to actual item IDs
	private static final Map<Integer, Integer> ITEM_ID_ALIASES = Map.ofEntries(
		Map.entry(ItemID.FOSSIL_MOTHERLODE_REWARD_HAT, ItemID.MOTHERLODE_REWARD_HAT),
		Map.entry(ItemID.FOSSIL_MOTHERLODE_REWARD_TOP, ItemID.MOTHERLODE_REWARD_TOP),
		Map.entry(ItemID.FOSSIL_MOTHERLODE_REWARD_LEGS, ItemID.MOTHERLODE_REWARD_LEGS),
		Map.entry(ItemID.FOSSIL_MOTHERLODE_REWARD_BOOTS, ItemID.MOTHERLODE_REWARD_BOOTS),
		Map.entry(ItemID.TOL_TEA_DUMMY, ItemID.TOL_TEA),
		Map.entry(ItemID.TOL_PLAIN_SACK_DUMMY, ItemID.TOL_PLAIN_SACK),
		Map.entry(ItemID.TOL_GREEN_SACK_DUMMY, ItemID.TOL_GREEN_SACK),
		Map.entry(ItemID.TOL_RED_SACK_DUMMY, ItemID.TOL_RED_SACK),
		Map.entry(ItemID.TOL_BLACK_SACK_DUMMY, ItemID.TOL_BLACK_SACK),
		Map.entry(ItemID.TOL_GOLD_SACK_DUMMY, ItemID.TOL_GOLD_SACK),
		Map.entry(ItemID.TOL_RUNE_SACK_DUMMY, ItemID.TOL_RUNE_SACK),
		Map.entry(ItemID.ABYSSALSIRE_UNSIRED_DUMMY, ItemID.ABYSSALSIRE_UNSIRED),
		Map.entry(ItemID.COAL_BAG_DUMMY, ItemID.COAL_BAG),
		Map.entry(ItemID.GEM_BAG_DUMMY, ItemID.GEM_BAG),
		Map.entry(ItemID.PLANK_SACK_DUMMY, ItemID.PLANK_SACK),
		Map.entry(ItemID.FLAMTAER_BAG_DUMMY, ItemID.FLAMTAER_BAG),
		Map.entry(ItemID.AMULET_OF_CHEMISTRY_IMBUED_CHARGED_DUMMY, ItemID.AMULET_OF_CHEMISTRY_IMBUED_CHARGED),
		Map.entry(ItemID.YAMA_DOSSIER_DUMMY, ItemID.YAMA_DOSSIER)
	);

	private final Client client;
	private final ItemManager itemManager;
	private final Set<Integer> itemIds = new HashSet<>();
	private final Map<String, Set<Integer>> byName = new HashMap<>();

	@Inject
	CollectionLogCatalog(Client client, ItemManager itemManager)
	{
		this.client = client;
		this.itemManager = itemManager;
	}

	boolean isLoaded()
	{
		return !itemIds.isEmpty();
	}

	void load()
	{
		if (isLoaded())
		{
			return;
		}
		Set<Integer> ids = new HashSet<>();
		for (int tab : values(COLLECTION_TABS))
		{
			StructComposition tabStruct = client.getStructComposition(tab);
			for (int page : values(tabStruct.getIntValue(TAB_PAGES)))
			{
				StructComposition pageStruct = client.getStructComposition(page);
				for (int item : values(pageStruct.getIntValue(PAGE_ITEMS)))
				{
					if (item > 0)
					{
						ids.add(obtainableId(item));
					}
				}
			}
		}
		Map<String, Set<Integer>> names = new HashMap<>();
		for (int id : ids)
		{
			ItemComposition item = itemManager.getItemComposition(id);
			String name = DropMessageParser.identity(item.getName());
			names.computeIfAbsent(name, ignored -> new HashSet<>()).add(id);
		}
		itemIds.addAll(ids);
		byName.putAll(names);
	}

	Integer find(String itemName)
	{
		Set<Integer> ids = byName.get(DropMessageParser.identity(itemName));
		return ids != null && ids.size() == 1 ? ids.iterator().next() : null;
	}

	Integer find(String itemName, Set<Integer> recentLoot)
	{
		Set<Integer> ids = byName.get(DropMessageParser.identity(itemName));
		Integer match = null;
		if (ids != null)
		{
			for (int id : ids)
			{
				if (recentLoot.contains(id))
				{
					if (match != null)
					{
						return null;
					}
					match = id;
				}
			}
		}
		return match;
	}

	Integer collectionItem(int itemId)
	{
		int canonical = obtainableId(itemId);
		return itemIds.contains(canonical) ? canonical : null;
	}

	String itemName(int itemId)
	{
		return DropMessageParser.clean(itemManager.getItemComposition(itemId).getName());
	}

	void clear()
	{
		itemIds.clear();
		byName.clear();
	}

	private int[] values(int enumId)
	{
		EnumComposition composition = client.getEnum(enumId);
		if (composition == null || composition.getIntVals() == null)
		{
			throw new IllegalStateException("Collection log cache is not ready");
		}
		return composition.getIntVals();
	}

	private int obtainableId(int itemId)
	{
		int canonical = itemManager.canonicalize(itemId);
		return ITEM_ID_ALIASES.getOrDefault(canonical, canonical);
	}
}
