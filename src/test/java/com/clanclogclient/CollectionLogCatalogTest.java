package com.clanclogclient;

import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.ItemComposition;
import net.runelite.api.StructComposition;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CollectionLogCatalogTest
{
	private final Client client = mock(Client.class);
	private final ItemManager items = mock(ItemManager.class);
	private final CollectionLogCatalog catalog = new CollectionLogCatalog(client, items);

	@Before
	public void setUp()
	{
		when(items.canonicalize(anyInt())).thenAnswer(call -> call.getArgument(0));
		enumValues(CollectionLogCatalog.COLLECTION_TABS, 1);
		struct(1, CollectionLogCatalog.TAB_PAGES, 2);
		enumValues(2, 3, 4);
		struct(3, CollectionLogCatalog.PAGE_ITEMS, 5);
		struct(4, CollectionLogCatalog.PAGE_ITEMS, 6);
		enumValues(5, 100, 101, 102);
		enumValues(6, 100, 103);
		item(100, "Abyssal whip");
		item(101, "Untradeable dye");
		item(102, "Ancient page");
		item(103, "Ancient page");
	}

	@Test
	public void walksAllPagesAndResolvesTradeableAndUntradeableItems()
	{
		catalog.load();
		assertTrue(catalog.isLoaded());
		assertEquals(Integer.valueOf(100), catalog.find("ABYSSAL WHIP"));
		assertEquals("Abyssal whip", catalog.itemName(100));
		assertEquals(Integer.valueOf(101), catalog.find("Untradeable dye"));
		assertNull(catalog.find("Coins"));
		verify(client, times(1)).getEnum(CollectionLogCatalog.COLLECTION_TABS);
		catalog.load();
		verify(client, times(1)).getEnum(CollectionLogCatalog.COLLECTION_TABS);
	}

	@Test
	public void ambiguousNamesRequireExactLootIds()
	{
		catalog.load();
		assertNull(catalog.find("Ancient page"));
		assertEquals(Integer.valueOf(102), catalog.collectionItem(102));
		assertEquals(Integer.valueOf(103), catalog.collectionItem(103));
		assertNull(catalog.collectionItem(999));
		assertEquals(Integer.valueOf(102), catalog.find("Ancient page", Set.of(102)));
		assertNull(catalog.find("Ancient page", Set.of(102, 103)));
		assertNull(catalog.find("Ancient page", Set.of(100)));
	}

	@Test
	public void canonicalizesNotesAndCanReload()
	{
		catalog.load();
		when(items.canonicalize(200)).thenReturn(100);
		assertEquals(Integer.valueOf(100), catalog.collectionItem(200));
		catalog.clear();
		assertFalse(catalog.isLoaded());
		assertNull(catalog.find("Abyssal whip"));
		catalog.load();
		assertEquals(Integer.valueOf(100), catalog.find("Abyssal whip"));
	}

	@Test
	public void resolvesCollectionLogDisplayIdsToObtainableItems()
	{
		enumValues(6, ItemID.ABYSSALSIRE_UNSIRED_DUMMY);
		item(ItemID.ABYSSALSIRE_UNSIRED, "Unsired");
		catalog.load();
		assertEquals(Integer.valueOf(ItemID.ABYSSALSIRE_UNSIRED), catalog.find("Unsired"));
		assertEquals(Integer.valueOf(ItemID.ABYSSALSIRE_UNSIRED), catalog.collectionItem(ItemID.ABYSSALSIRE_UNSIRED));
		assertEquals(Integer.valueOf(ItemID.ABYSSALSIRE_UNSIRED), catalog.collectionItem(ItemID.ABYSSALSIRE_UNSIRED_DUMMY));
	}

	@Test
	public void bothProspectorVariantsResolveToOriginalMotherlodeIds()
	{
		int[] motherlode = {ItemID.MOTHERLODE_REWARD_HAT, ItemID.MOTHERLODE_REWARD_TOP,
			ItemID.MOTHERLODE_REWARD_LEGS, ItemID.MOTHERLODE_REWARD_BOOTS};
		int[] volcanic = {ItemID.FOSSIL_MOTHERLODE_REWARD_HAT, ItemID.FOSSIL_MOTHERLODE_REWARD_TOP,
			ItemID.FOSSIL_MOTHERLODE_REWARD_LEGS, ItemID.FOSSIL_MOTHERLODE_REWARD_BOOTS};
		String[] names = {"Prospector helmet", "Prospector jacket", "Prospector legs", "Prospector boots"};
		for (int i = 0; i < motherlode.length; i++)
		{
			item(motherlode[i], names[i]);
		}
		// Cache entries and loot from either shop must resolve to the same original item IDs.
		for (int[] cacheIds : new int[][]{motherlode, volcanic})
		{
			enumValues(6, cacheIds);
			catalog.clear();
			catalog.load();
			for (int i = 0; i < motherlode.length; i++)
			{
				Integer expected = motherlode[i];
				assertEquals(expected, catalog.find(names[i]));
				assertEquals(expected, catalog.collectionItem(motherlode[i]));
				assertEquals(expected, catalog.collectionItem(volcanic[i]));
				assertEquals(expected, catalog.find(names[i], Set.of(motherlode[i])));
				assertEquals(names[i], catalog.itemName(expected));
			}
		}
	}

	@Test
	public void resolvesReportedScytheNameWithParenthesesAndCaseDifferences()
	{
		enumValues(6, 22486);
		item(22486, "Scythe of vitur (uncharged)");
		catalog.load();
		assertEquals(Integer.valueOf(22486), catalog.find("Scythe of Vitur (uncharged)"));
	}

	@Test
	public void discoversNewItemsAndPagesFromRefreshedGameCache()
	{
		catalog.load();
		assertNull(catalog.find("Future collection reward"));
		// A new game cache adds a tab/page and an item absent from all plugin constants.
		enumValues(CollectionLogCatalog.COLLECTION_TABS, 1, 20);
		struct(20, CollectionLogCatalog.TAB_PAGES, 21);
		enumValues(21, 22);
		struct(22, CollectionLogCatalog.PAGE_ITEMS, 23);
		enumValues(23, 500001);
		item(500001, "Future collection reward");
		catalog.clear();
		catalog.load();
		assertEquals(Integer.valueOf(500001), catalog.find("Future collection reward"));
		assertEquals(Integer.valueOf(500001), catalog.collectionItem(500001));
		assertEquals(Integer.valueOf(100), catalog.find("Abyssal whip"));
	}

	@Test
	public void failedLoadDoesNotPublishPartialCatalog()
	{
		when(client.getEnum(6)).thenThrow(new IllegalStateException("Cache unavailable"));
		assertThrows(IllegalStateException.class, catalog::load);
		assertFalse(catalog.isLoaded());
		assertNull(catalog.find("Abyssal whip"));
	}

	private void enumValues(int id, int... values)
	{
		EnumComposition composition = mock(EnumComposition.class);
		when(composition.getIntVals()).thenReturn(values);
		when(client.getEnum(id)).thenReturn(composition);
	}

	private void struct(int id, int param, int value)
	{
		StructComposition composition = mock(StructComposition.class);
		when(composition.getIntValue(param)).thenReturn(value);
		when(client.getStructComposition(id)).thenReturn(composition);
	}

	private void item(int id, String name)
	{
		ItemComposition composition = mock(ItemComposition.class);
		when(composition.getName()).thenReturn(name);
		when(items.getItemComposition(id)).thenReturn(composition);
	}
}
