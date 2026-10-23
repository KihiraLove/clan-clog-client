package com.clanclogclient;

import com.google.inject.Guice;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.time.Clock;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.ScriptID;
import net.runelite.api.World;
import net.runelite.api.WorldType;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanID;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemStack;
import net.runelite.client.game.WorldService;
import net.runelite.client.events.WorldsFetch;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.DrawManager;
import net.runelite.http.api.loottracker.LootRecordType;
import okhttp3.HttpUrl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ClanClogDetectionTest
{
	private static final String RAID_MESSAGE = "Nerd\u00a0Stark received special loot from a raid: Scythe of Vitur (uncharged) (1,188,452,005 coins).";
	private static final String[] PRIORITY_MESSAGES = {
		" New item added to your collection log: <col=ff0000>Abyssal\u00a0whip</col> ",
		"<col=ff0000>New item: Abyssal whip</col>",
		"Valuable drop: Abyssal whip (1,000,000 coins)",
		"Untradeable drop: Abyssal whip",
		"RuneLite Loot Tracker event: Local Player obtained 1 x Abyssal whip (item ID: 4151), source: Abyssal demon, type: NPC.",
		"Local Player has received a drop: 1 x Abyssal whip."
	};
	@Mock private Client client;
	@Mock private ClientThread clientThread;
	@Mock private DrawManager drawManager;
	@Mock private ClanClogConfig config;
	@Mock private CollectionLogCatalog catalog;
	@Mock private SubmissionClient submissions;
	@Mock private ClanChannel clan;
	@Mock private ClanChannelMember raidMember;
	@Mock private World recipientWorld;
	@Mock private Clock clock;
	@Mock private WorldService worldService;
	@Mock private ScheduledExecutorService executor;
	private ClanClogPlugin plugin;
	private AutoCloseable mocks;

	@Before
	public void setUp()
	{
		mocks = MockitoAnnotations.openMocks(this);
		plugin = new ClanClogPlugin(clock);
		Guice.createInjector(binder ->
		{
			binder.bind(Client.class).toInstance(client);
			binder.bind(ClientThread.class).toInstance(clientThread);
			binder.bind(DrawManager.class).toInstance(drawManager);
			binder.bind(ClanClogConfig.class).toInstance(config);
			binder.bind(CollectionLogCatalog.class).toInstance(catalog);
			binder.bind(SubmissionClient.class).toInstance(submissions);
			binder.bind(WorldService.class).toInstance(worldService);
			binder.bind(ScheduledExecutorService.class).toInstance(executor);
		}).injectMembers(plugin);
		when(clock.millis()).thenReturn(1_000_000L);
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		Player player = mock(Player.class);
		when(player.getName()).thenReturn("Local Player");
		when(client.getLocalPlayer()).thenReturn(player);
		when(clan.getName()).thenReturn("Our Clan");
		when(client.getClanChannel(ClanID.CLAN)).thenReturn(clan);
		when(raidMember.getName()).thenReturn("Nerd\u00a0Stark");
		when(raidMember.getWorld()).thenReturn(302);
		when(clan.findMember("Nerd Stark")).thenReturn(raidMember);
		ClanChannelMember otherMember = mock(ClanChannelMember.class);
		when(otherMember.getName()).thenReturn("Other Player");
		when(otherMember.getWorld()).thenReturn(302);
		when(clan.findMember("Other Player")).thenReturn(otherMember);
		when(clan.getMembers()).thenReturn(Arrays.asList(raidMember, otherMember));
		when(recipientWorld.getId()).thenReturn(302);
		when(recipientWorld.getTypes()).thenReturn(EnumSet.of(WorldType.MEMBERS));
		World observerWorld = mock(World.class);
		when(observerWorld.getId()).thenReturn(301);
		when(observerWorld.getTypes()).thenReturn(EnumSet.of(WorldType.MEMBERS));
		when(client.getWorld()).thenReturn(301);
		when(client.getWorldList()).thenReturn(new World[]{observerWorld, recipientWorld});
		ClanSettings membership = mock(ClanSettings.class);
		when(membership.getName()).thenReturn("Our Clan");
		when(client.getClanSettings(ClanID.CLAN)).thenReturn(membership);
		when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.MEMBERS));
		when(config.submissionUrl()).thenReturn("https://clan.example/api/v1/runelite/submissions");
		when(config.clanApiKey()).thenReturn("test-key");
		when(config.submissionChatNotification()).thenReturn(true);
		when(catalog.isLoaded()).thenReturn(true);
		when(catalog.find(anyString())).thenReturn(null);
		when(catalog.find(anyString(), anySet())).thenReturn(null);
		when(catalog.collectionItem(anyInt())).thenReturn(null);
		when(catalog.find("Abyssal whip")).thenReturn(4151);
		when(catalog.find("Scythe of Vitur (uncharged)")).thenReturn(22486);
		when(catalog.itemName(4151)).thenReturn("Abyssal whip");
		when(catalog.itemName(102)).thenReturn("Ancient page");
		doAnswer(call -> { ((Runnable) call.getArgument(0)).run(); return null; })
			.when(clientThread).invoke(any(Runnable.class));
		plugin.startUp();
	}

	@After
	public void tearDown() throws Exception
	{
		plugin.shutDown();
		mocks.close();
	}

	@Test
	public void petBroadcastUsesRecipientWorldAndNeverAttachesScreenshot()
	{
		when(catalog.find("Yami")).thenReturn(ItemID.YAMAPET);
		String text = "[Our Clan] Other Player have a funny feeling like they would have been followed: Yami at 1,234 killcount.";
		message(ChatMessageType.CLAN_MESSAGE, text);
		Submission receipt = submitBroadcastAndCapture();
		assertEquals("Other Player", receipt.getUsername());
		assertEquals(text, receipt.getChatMessage());
		assertEquals(ItemID.YAMAPET, receipt.getItemId());
	}

	@Test
	public void localPetLootAndOwnPetBroadcastCreateOnePersonalClaim()
	{
		when(catalog.itemName(ItemID.YAMAPET)).thenReturn("Yami");
		message(ChatMessageType.CLAN_MESSAGE, "[Our Clan] Local Player has a funny feeling like he's being followed: Yami at 1 killcount.");
		loot(ItemID.YAMAPET);
		assertTrue(submitPersonalAndCapture().isPersonal());
	}

	@Test
	public void lootTrackerOutranksUniqueFishingMessageAndMergesBothTriggers()
	{
		when(catalog.collectionItem(ItemID.GOLDEN_TENCH)).thenReturn(ItemID.GOLDEN_TENCH);
		when(catalog.itemName(ItemID.GOLDEN_TENCH)).thenReturn("Golden tench");
		loot(ItemID.GOLDEN_TENCH);
		String text = "The cormorant has brought you a very strange tench.";
		message(ChatMessageType.SPAM, text);
		Submission receipt = submitPersonalAndCapture();
		assertTrue(receipt.getChatMessage().startsWith("RuneLite Loot Tracker event:"));
		assertTrue(receipt.isLootEvent());
	}

	@Test
	public void uniqueRewardTextInPlayerChatCannotSubmit()
	{
		message(ChatMessageType.PUBLICCHAT, "You find a Hespori seed.");
		message(ChatMessageType.CLAN_CHAT, "You find a Hespori seed.");
		plugin.onGameTick(new GameTick());
		verifyNoInteractions(drawManager);
	}

	@Test
	public void rewardDialogsSubmitOnceWhileHeldOpenAndAgainWhenReopened()
	{
		when(catalog.collectionItem(ItemID.ALUFT_GNOME_SCARF)).thenReturn(ItemID.ALUFT_GNOME_SCARF);
		when(catalog.itemName(ItemID.ALUFT_GNOME_SCARF)).thenReturn("Gnome scarf");
		String raw = "You are given a gnome pilot's scarf as a tip.";
		dialog(InterfaceID.Messagebox.TEXT, raw);
		assertEquals(raw, submitPersonalAndCapture().getChatMessage());
		when(clock.millis()).thenReturn(1_006_000L);
		plugin.onGameTick(new GameTick());
		verify(drawManager).requestNextFrameListener(any());
		loaded(InterfaceID.MESSAGEBOX);
		plugin.onGameTick(new GameTick());
		ArgumentCaptor<Consumer<Image>> capture = ArgumentCaptor.forClass(Consumer.class);
		verify(drawManager, times(2)).requestNextFrameListener(capture.capture());
		capture.getAllValues().get(1).accept(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB));
		verify(submissions, times(2)).submit(any(), any(BufferedImage.class), any(), any(), anyLong());
	}

	@Test
	public void arenaLapRequiresTicketOrVoucherGainsAndCanClaimBoth()
	{
		inventory(InventoryID.INV);
		when(catalog.collectionItem(ItemID.AGILITYARENA_TICKET_NEW)).thenReturn(ItemID.AGILITYARENA_TICKET_NEW);
		when(catalog.collectionItem(ItemID.AGILITYARENA_VOUCHER)).thenReturn(ItemID.AGILITYARENA_VOUCHER);
		when(catalog.itemName(ItemID.AGILITYARENA_TICKET_NEW)).thenReturn("Agility arena ticket");
		when(catalog.itemName(ItemID.AGILITYARENA_VOUCHER)).thenReturn("Brimhaven voucher");
		message(ChatMessageType.GAMEMESSAGE, "Your Agility Arena lap count is: 123");
		plugin.onGameTick(new GameTick());
		verifyNoInteractions(drawManager);
		inventory(InventoryID.INV, new Item(ItemID.AGILITYARENA_TICKET_NEW, 1), new Item(ItemID.AGILITYARENA_VOUCHER, 1));
		plugin.onGameTick(new GameTick());
		drawNextFrame();
		verify(submissions, times(2)).submit(any(), any(BufferedImage.class), any(), any(), anyLong());
	}

	@Test
	public void arenaRewardCanArriveBeforeItsMessageButDoesNotUseExpiredGains()
	{
		inventory(InventoryID.INV);
		when(catalog.collectionItem(ItemID.AGILITYARENA_VOUCHER)).thenReturn(ItemID.AGILITYARENA_VOUCHER);
		when(catalog.itemName(ItemID.AGILITYARENA_VOUCHER)).thenReturn("Brimhaven voucher");
		inventory(InventoryID.INV, new Item(ItemID.AGILITYARENA_VOUCHER, 1));
		for (int i = 0; i < 4; i++) plugin.onGameTick(new GameTick());
		message(ChatMessageType.GAMEMESSAGE, "Your Agility Arena lap count is: 123");
		plugin.onGameTick(new GameTick());
		verifyNoInteractions(drawManager);
		inventory(InventoryID.INV, new Item(ItemID.AGILITYARENA_VOUCHER, 2));
		message(ChatMessageType.GAMEMESSAGE, "Your Agility Arena lap count is: 124");
		assertEquals(ItemID.AGILITYARENA_VOUCHER, submitPersonalAndCapture().getItemId());
	}

	@Test
	public void anglerConfirmationAndCancelNeverSubmitWithoutCompletedConversion()
	{
		when(catalog.collectionItem(ItemID.SPIRIT_ANGLER_HAT)).thenReturn(ItemID.SPIRIT_ANGLER_HAT);
		when(catalog.itemName(ItemID.SPIRIT_ANGLER_HAT)).thenReturn("Spirit angler headband");
		inventory(InventoryID.INV, new Item(ItemID.TRAWLER_REWARD_HAT, 1), new Item(ItemID.SPIRIT_FLAKES, 2_400));
		dialog(InterfaceID.ObjectboxDouble.TEXT, RewardMessageParserTest.confirmation("angler hat"));
		for (int i = 0; i < 8; i++) plugin.onGameTick(new GameTick());
		when(client.getWidget(InterfaceID.ObjectboxDouble.TEXT)).thenReturn(null);
		for (int i = 0; i < 4; i++) plugin.onGameTick(new GameTick());
		inventory(InventoryID.INV, new Item(ItemID.SPIRIT_ANGLER_HAT, 1), new Item(ItemID.SPIRIT_FLAKES, 1_200));
		plugin.onGameTick(new GameTick());
		verifyNoInteractions(drawManager);
	}

	@Test
	public void anglerUpgradeWaitsForOutputInputAndSpiritFlakeChanges()
	{
		when(catalog.collectionItem(ItemID.SPIRIT_ANGLER_HAT)).thenReturn(ItemID.SPIRIT_ANGLER_HAT);
		when(catalog.itemName(ItemID.SPIRIT_ANGLER_HAT)).thenReturn("Spirit angler headband");
		inventory(InventoryID.INV, new Item(ItemID.TRAWLER_REWARD_HAT, 1), new Item(ItemID.SPIRIT_FLAKES, 2_400));
		String raw = RewardMessageParserTest.confirmation("angler hat");
		dialog(InterfaceID.ObjectboxDouble.TEXT, raw);
		for (int i = 0; i < 5; i++) plugin.onGameTick(new GameTick());
		// An unrelated gain of an owned Spirit Angler item is insufficient.
		inventory(InventoryID.INV, new Item(ItemID.TRAWLER_REWARD_HAT, 1), new Item(ItemID.SPIRIT_ANGLER_HAT, 1), new Item(ItemID.SPIRIT_FLAKES, 2_400));
		plugin.onGameTick(new GameTick());
		verifyNoInteractions(drawManager);
		inventory(InventoryID.INV, new Item(ItemID.SPIRIT_ANGLER_HAT, 1), new Item(ItemID.SPIRIT_FLAKES, 1_200));
		Submission receipt = submitPersonalAndCapture();
		assertEquals(ItemID.SPIRIT_ANGLER_HAT, receipt.getItemId());
		assertEquals(raw, receipt.getChatMessage());
	}

	@Test
	public void conversionHintsAreClearedOnWorldHop()
	{
		when(catalog.collectionItem(ItemID.SPIRIT_ANGLER_HAT)).thenReturn(ItemID.SPIRIT_ANGLER_HAT);
		inventory(InventoryID.INV, new Item(ItemID.TRAWLER_REWARD_HAT, 1), new Item(ItemID.SPIRIT_FLAKES, 2_400));
		dialog(InterfaceID.ObjectboxDouble.TEXT, RewardMessageParserTest.confirmation("angler hat"));
		GameStateChanged changed = mock(GameStateChanged.class);
		when(changed.getGameState()).thenReturn(GameState.HOPPING);
		plugin.onGameStateChanged(changed);
		when(client.getWidget(InterfaceID.ObjectboxDouble.TEXT)).thenReturn(null);
		inventory(InventoryID.INV, new Item(ItemID.SPIRIT_ANGLER_HAT, 1));
		plugin.onGameTick(new GameTick());
		verifyNoInteractions(drawManager);
	}

	private void dialog(int component, String raw)
	{
		Widget widget = mock(Widget.class);
		when(widget.getText()).thenReturn(raw);
		when(client.getWidget(component)).thenReturn(widget);
		loaded(component >>> 16);
	}

	private void loaded(int group)
	{
		WidgetLoaded event = new WidgetLoaded();
		event.setGroupId(group);
		plugin.onWidgetLoaded(event);
	}

	@Test
	public void dialogMessageEventsAndWidgetsCoalesceIntoOneClaim()
	{
		when(catalog.collectionItem(ItemID.IMCANDO_HAMMER)).thenReturn(ItemID.IMCANDO_HAMMER);
		when(catalog.itemName(ItemID.IMCANDO_HAMMER)).thenReturn("Imcando hammer");
		String raw = "Ramarno hands you the Imcando Hammer.";
		message(ChatMessageType.DIALOG, raw);
		message(ChatMessageType.MESBOX, raw);
		dialog(InterfaceID.ChatLeft.TEXT, raw);
		assertEquals(raw, submitPersonalAndCapture().getChatMessage());
	}

	@Test
	public void dialogTextPopulatedAfterWidgetLoadedIsReadOnTheNextClientCycle()
	{
		when(catalog.collectionItem(ItemID.ALUFT_SEED_POD)).thenReturn(ItemID.ALUFT_SEED_POD);
		when(catalog.itemName(ItemID.ALUFT_SEED_POD)).thenReturn("Grand seed pod");
		loaded(InterfaceID.OBJECTBOX);
		Widget widget = mock(Widget.class);
		when(widget.getText()).thenReturn("You are given some seed pods as a tip.");
		when(client.getWidget(InterfaceID.Objectbox.TEXT)).thenReturn(widget);
		ArgumentCaptor<Runnable> callback = ArgumentCaptor.forClass(Runnable.class);
		verify(clientThread).invokeLater(callback.capture());
		callback.getValue().run();
		assertEquals(ItemID.ALUFT_SEED_POD, submitPersonalAndCapture().getItemId());
	}

	@Test
	public void hiddenOrPreexistingRewardDialogsDoNotCreateClaims()
	{
		when(catalog.collectionItem(ItemID.ALUFT_GNOME_SCARF)).thenReturn(ItemID.ALUFT_GNOME_SCARF);
		Widget widget = mock(Widget.class);
		when(widget.getText()).thenReturn("You are given a gnome pilot's scarf as a tip.");
		when(widget.isHidden()).thenReturn(true);
		when(client.getWidget(InterfaceID.Messagebox.TEXT)).thenReturn(widget);
		loaded(InterfaceID.MESSAGEBOX);
		plugin.onGameTick(new GameTick());
		when(widget.isHidden()).thenReturn(false);
		ConfigChanged configChange = mock(ConfigChanged.class);
		when(configChange.getGroup()).thenReturn(ClanClogConfig.GROUP);
		when(configChange.getKey()).thenReturn("submissionUrl");
		plugin.onConfigChanged(configChange);
		plugin.onGameTick(new GameTick());
		verifyNoInteractions(drawManager);
	}

	@Test
	public void everyAnglerPieceCanBeConfirmedFromTheChoiceDialogTitle()
	{
		int[] outputs = {ItemID.SPIRIT_ANGLER_HAT, ItemID.SPIRIT_ANGLER_TOP, ItemID.SPIRIT_ANGLER_LEGS, ItemID.SPIRIT_ANGLER_BOOTS};
		String[] pieces = {"angler hat", "angler top", "angler waders", "angler boots"};
		for (int i = 0; i < outputs.length; i++)
		{
			int item = outputs[i];
			when(catalog.collectionItem(item)).thenReturn(item);
			when(catalog.itemName(item)).thenReturn(pieces[i]);
			inventory(InventoryID.INV, new Item(RewardMessageParser.anglerInput(item), 1), new Item(ItemID.SPIRIT_FLAKES, 1_200));
			Widget options = mock(Widget.class);
			Widget title = mock(Widget.class);
			when(options.getChildren()).thenReturn(new Widget[]{title});
			when(title.getText()).thenReturn(RewardMessageParserTest.confirmation(pieces[i]));
			when(client.getWidget(InterfaceID.Chatmenu.OPTIONS)).thenReturn(options);
			loaded(InterfaceID.CHATMENU);
			inventory(InventoryID.INV, new Item(item, 1));
		}
		plugin.onGameTick(new GameTick());
		drawNextFrame();
		verify(submissions, times(4)).submit(any(), any(BufferedImage.class), any(), any(), anyLong());
	}

	@Test
	public void submissionChatNotificationsDefaultToEnabled()
	{
		assertTrue(new ClanClogConfig() {}.submissionChatNotification());
	}

	@Test
	public void acceptedSubmissionNotificationUsesRequestedPrefixAndFormat()
	{
		notification("Abyssal whip submission for Local Player accepted by API.");
		verify(client).addChatMessage(eq(ChatMessageType.GAMEMESSAGE), eq(""),
			eq("[Clan clog] Abyssal whip submission for Local Player accepted by API."), isNull());
	}

	@Test
	public void mutingNotificationsDoesNotCancelUploadsOrShowErrors()
	{
		message(ChatMessageType.GAMEMESSAGE, "New item added to your collection log: Abyssal whip");
		plugin.onGameTick(new GameTick());
		when(config.submissionChatNotification()).thenReturn(false);
		ConfigChanged changed = mock(ConfigChanged.class);
		when(changed.getGroup()).thenReturn(ClanClogConfig.GROUP);
		when(changed.getKey()).thenReturn(ClanClogConfig.SUBMISSION_CHAT_NOTIFICATION);
		plugin.onConfigChanged(changed);
		drawNextFrame();
		verify(submissions).submit(any(), any(BufferedImage.class), any(), any(), anyLong());
		verify(submissions).start(any());
		verify(submissions, never()).stop();
		notification("Abyssal whip submission for Local Player accepted by API.");
		notification("API rejected the clan key (401); check Clan API key.");
		verify(client, never()).addChatMessage(any(), anyString(), anyString(), any());
	}

	@Test
	public void notificationsCanBeReenabledWithoutRestartingPlugin()
	{
		String text = "Abyssal whip submission for Local Player accepted by API.";
		when(config.submissionChatNotification()).thenReturn(false);
		notification(text);
		when(config.submissionChatNotification()).thenReturn(true);
		notification(text);
		verify(client).addChatMessage(eq(ChatMessageType.GAMEMESSAGE), eq(""), eq("[Clan clog] " + text), isNull());
	}

	@Test
	public void loginScreenInvalidatesCatalogForFreshGameData()
	{
		GameStateChanged changed = mock(GameStateChanged.class);
		when(changed.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
		plugin.onGameStateChanged(changed);
		verify(catalog).clear();
		message(ChatMessageType.GAMEMESSAGE, "New item added to your collection log: Abyssal whip");
		verify(catalog).load();
	}

	@Test
	public void serializesWorldWithoutFlagsAsEmptyJsonArray()
	{
		when(client.getWorldType()).thenReturn(EnumSet.noneOf(WorldType.class));
		message(ChatMessageType.GAMEMESSAGE, "New item added to your collection log: Abyssal whip");
		plugin.onGameTick(new GameTick());
		drawNextFrame();
		ArgumentCaptor<Submission> receipt = ArgumentCaptor.forClass(Submission.class);
		verify(submissions).submit(receipt.capture(), any(), any(), any(), anyLong());
		assertEquals("[]", receipt.getValue().getWorldType());
	}

	@Test
	public void serializesAllWorldFlagsAsQuotedJsonNames()
	{
		when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.MEMBERS, WorldType.HARDCORE_MAIN));
		message(ChatMessageType.GAMEMESSAGE, "New item added to your collection log: Abyssal whip");
		plugin.onGameTick(new GameTick());
		drawNextFrame();
		ArgumentCaptor<Submission> receipt = ArgumentCaptor.forClass(Submission.class);
		verify(submissions).submit(receipt.capture(), any(), any(), any(), anyLong());
		assertTrue(Set.of("[\"MEMBERS\",\"HARDCORE_MAIN\"]", "[\"HARDCORE_MAIN\",\"MEMBERS\"]")
			.contains(receipt.getValue().getWorldType()));
	}

	@Test
	public void raidBroadcastFromUserLogSubmitsExactMessageWithoutScreenshot()
	{
		when(client.getClanSettings(ClanID.CLAN)).thenReturn(null);
		message(ChatMessageType.CLAN_MESSAGE, RAID_MESSAGE);
		plugin.onGameTick(new GameTick());
		ArgumentCaptor<Submission> receipt = ArgumentCaptor.forClass(Submission.class);
		verify(submissions).submit(receipt.capture(), isNull(), any(HttpUrl.class), eq("test-key"), anyLong());
		assertEquals("Nerd Stark", receipt.getValue().getUsername());
		assertEquals("Our Clan", receipt.getValue().getClanName());
		assertEquals(22486, receipt.getValue().getItemId());
		assertEquals("Scythe of Vitur (uncharged)", receipt.getValue().getItemName());
		assertEquals(RAID_MESSAGE, receipt.getValue().getChatMessage());
		assertFalse(receipt.getValue().isPersonal());
		verifyNoInteractions(drawManager);
	}

	@Test
	public void clanCollectionBroadcastPreservesMarkupAndWhitespace()
	{
		ClanSettings membership = mock(ClanSettings.class);
		when(membership.getName()).thenReturn("Different Membership Clan");
		when(client.getClanSettings(ClanID.CLAN)).thenReturn(membership);
		String raw = " <img=2>Other\u00a0Player received a new collection log item: <col=ff0000>Abyssal whip</col> (10/1717) ";
		message(ChatMessageType.CLAN_MESSAGE, raw);
		plugin.onGameTick(new GameTick());
		ArgumentCaptor<Submission> receipt = ArgumentCaptor.forClass(Submission.class);
		verify(submissions).submit(receipt.capture(), isNull(), any(), any(), anyLong());
		assertEquals("Other Player", receipt.getValue().getUsername());
		assertEquals("Our Clan", receipt.getValue().getClanName());
		assertEquals(raw, receipt.getValue().getChatMessage());
		assertFalse(receipt.getValue().isPersonal());
		verifyNoInteractions(drawManager);
	}

	@Test
	public void broadcastOnWorld470UsesWorldServiceWhenClientWorldListIsNull()
	{
		when(client.getWorldList()).thenReturn(null);
		when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.PVP));
		when(raidMember.getWorld()).thenReturn(470);
		EnumSet<net.runelite.http.api.worlds.WorldType> types = EnumSet.of(net.runelite.http.api.worlds.WorldType.MEMBERS,
			net.runelite.http.api.worlds.WorldType.SEASONAL);
		net.runelite.http.api.worlds.World world = net.runelite.http.api.worlds.World.builder().id(470).types(types).build();
		WorldResult result = new WorldResult();
		result.setWorlds(java.util.Collections.singletonList(world));
		plugin.onWorldsFetch(new WorldsFetch(result));
		message(ChatMessageType.CLAN_MESSAGE, RAID_MESSAGE);
		types.clear();
		types.add(net.runelite.http.api.worlds.WorldType.PVP);
		plugin.onWorldsFetch(new WorldsFetch(result));
		Submission receipt = submitBroadcastAndCapture();
		assertEquals("[\"MEMBERS\",\"SEASONAL\"]", receipt.getWorldType());
		assertEquals(RAID_MESSAGE, receipt.getChatMessage());
		assertEquals("Nerd Stark", receipt.getUsername());
		assertFalse(receipt.isPersonal());
		verify(worldService, never()).getWorlds();
	}

	@Test
	public void serviceSnapshotCopiesFlagsAndConvertsHttpWorldTypeNames()
	{
		EnumSet<net.runelite.http.api.worlds.WorldType> types = EnumSet.of(
			net.runelite.http.api.worlds.WorldType.MEMBERS, net.runelite.http.api.worlds.WorldType.TOURNAMENT);
		plugin.onWorldsFetch(new WorldsFetch(serviceResult(302, types)));
		types.clear();
		message(ChatMessageType.CLAN_MESSAGE, RAID_MESSAGE);
		assertEquals("[\"MEMBERS\",\"TOURNAMENT_WORLD\"]", submitBroadcastAndCapture().getWorldType());
	}

	@Test
	public void refreshedWorldServiceDataTakesPriorityOverClientWorldList()
	{
		plugin.onWorldsFetch(new WorldsFetch(serviceResult(302,
			EnumSet.of(net.runelite.http.api.worlds.WorldType.MEMBERS))));
		plugin.onWorldsFetch(new WorldsFetch(serviceResult(302,
			EnumSet.of(net.runelite.http.api.worlds.WorldType.PVP))));
		message(ChatMessageType.CLAN_MESSAGE, RAID_MESSAGE);
		assertEquals("[\"PVP\"]", submitBroadcastAndCapture().getWorldType());
	}

	@Test
	public void emptyWorldServiceFlagsAreValidAndDoNotFallBackToOtherSources()
	{
		plugin.onWorldsFetch(new WorldsFetch(serviceResult(302,
			EnumSet.noneOf(net.runelite.http.api.worlds.WorldType.class))));
		message(ChatMessageType.CLAN_MESSAGE, RAID_MESSAGE);
		assertEquals("[]", submitBroadcastAndCapture().getWorldType());
	}

	@Test
	public void unavailableWorldServiceFlagsCanUseTheMatchingClientWorld()
	{
		plugin.onWorldsFetch(new WorldsFetch(serviceResult(302, null)));
		message(ChatMessageType.CLAN_MESSAGE, RAID_MESSAGE);
		assertEquals("[\"MEMBERS\"]", submitBroadcastAndCapture().getWorldType());
	}

	@Test
	public void worldServiceBootstrapRunsInExecutorWithoutBlockingBroadcastLookup()
	{
		when(client.getWorldList()).thenReturn(null);
		when(worldService.getWorlds()).thenReturn(serviceResult(302,
			EnumSet.of(net.runelite.http.api.worlds.WorldType.MEMBERS)));
		ArgumentCaptor<Runnable> bootstrap = ArgumentCaptor.forClass(Runnable.class);
		verify(executor).submit(bootstrap.capture());
		verify(worldService, never()).getWorlds();
		bootstrap.getValue().run();
		message(ChatMessageType.CLAN_MESSAGE, RAID_MESSAGE);
		assertEquals("[\"MEMBERS\"]", submitBroadcastAndCapture().getWorldType());
		verify(worldService).getWorlds();
	}

	@Test
	public void lateWorldServiceBootstrapDoesNotOverwriteNewerWorldsFetchData()
	{
		when(worldService.getWorlds()).thenReturn(serviceResult(302,
			EnumSet.of(net.runelite.http.api.worlds.WorldType.MEMBERS)));
		ArgumentCaptor<Runnable> bootstrap = ArgumentCaptor.forClass(Runnable.class);
		verify(executor).submit(bootstrap.capture());
		plugin.onWorldsFetch(new WorldsFetch(serviceResult(302,
			EnumSet.of(net.runelite.http.api.worlds.WorldType.PVP))));
		bootstrap.getValue().run();
		message(ChatMessageType.CLAN_MESSAGE, RAID_MESSAGE);
		assertEquals("[\"PVP\"]", submitBroadcastAndCapture().getWorldType());
	}

	@Test
	public void worldServiceBootstrapFromPreviousSessionCannotPopulateNewSession()
	{
		when(client.getWorldList()).thenReturn(null);
		when(worldService.getWorlds()).thenReturn(serviceResult(302,
			EnumSet.of(net.runelite.http.api.worlds.WorldType.MEMBERS)));
		ArgumentCaptor<Runnable> bootstrap = ArgumentCaptor.forClass(Runnable.class);
		verify(executor).submit(bootstrap.capture());
		plugin.shutDown();
		plugin.startUp();
		bootstrap.getValue().run();
		message(ChatMessageType.CLAN_MESSAGE, RAID_MESSAGE);
		plugin.onGameTick(new GameTick());
		verify(submissions, never()).submit(any(), any(), any(), any(), anyLong());
		plugin.onWorldsFetch(new WorldsFetch(serviceResult(302,
			EnumSet.of(net.runelite.http.api.worlds.WorldType.SEASONAL))));
		message(ChatMessageType.CLAN_MESSAGE, RAID_MESSAGE);
		assertEquals("[\"SEASONAL\"]", submitBroadcastAndCapture().getWorldType());
	}

	@Test
	public void shutdownCancelsWorldServiceBootstrapWithoutStoppingSharedExecutor()
	{
		plugin.shutDown();
		Future<?> bootstrap = mock(Future.class);
		doReturn(bootstrap).when(executor).submit(any(Runnable.class));
		plugin.startUp();
		plugin.shutDown();
		verify(bootstrap).cancel(false);
		verify(executor, never()).shutdown();
		verify(executor, never()).shutdownNow();
	}

	private WorldResult serviceResult(int id, EnumSet<net.runelite.http.api.worlds.WorldType> types)
	{
		net.runelite.http.api.worlds.World world = net.runelite.http.api.worlds.World.builder().id(id).types(types).build();
		WorldResult result = new WorldResult();
		result.setWorlds(java.util.Collections.singletonList(world));
		return result;
	}

	@Test
	public void broadcastFreezesRecipientWorldFlagsBeforeTheyHop()
	{
		EnumSet<WorldType> recipientTypes = EnumSet.of(WorldType.MEMBERS, WorldType.HARDCORE_MAIN);
		when(recipientWorld.getTypes()).thenReturn(recipientTypes);
		when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.PVP));
		message(ChatMessageType.CLAN_MESSAGE, RAID_MESSAGE);
		when(raidMember.getWorld()).thenReturn(303);
		recipientTypes.clear();
		recipientTypes.add(WorldType.SEASONAL);
		when(client.getWorldList()).thenReturn(null);
		Submission receipt = submitBroadcastAndCapture();
		assertEquals("[\"MEMBERS\",\"HARDCORE_MAIN\"]", receipt.getWorldType());
		assertEquals(RAID_MESSAGE, receipt.getChatMessage());
		assertFalse(receipt.isPersonal());
	}

	@Test
	public void broadcastMemberLookupNormalizesNamesWithoutChangingChatProof()
	{
		when(recipientWorld.getTypes()).thenReturn(EnumSet.of(WorldType.PVP));
		String raw = " <img=2>NERD_\u00a0STARK received special loot from a raid: Scythe of Vitur (uncharged) (1,188,452,005 coins). ";
		message(ChatMessageType.CLAN_MESSAGE, raw);
		Submission receipt = submitBroadcastAndCapture();
		assertEquals("[\"PVP\"]", receipt.getWorldType());
		assertEquals(raw, receipt.getChatMessage());
		verify(clan).findMember("NERD_ STARK");
		verify(clan).getMembers();
	}

	@Test
	public void broadcastFromKnownWorldWithNoFlagsSubmitsEmptyArray()
	{
		when(recipientWorld.getTypes()).thenReturn(EnumSet.noneOf(WorldType.class));
		message(ChatMessageType.CLAN_MESSAGE, RAID_MESSAGE);
		assertEquals("[]", submitBroadcastAndCapture().getWorldType());
	}

	@Test
	public void sameWorldBroadcastCanUseLocalFlagsWithoutWorldList()
	{
		when(raidMember.getWorld()).thenReturn(301);
		when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.MEMBERS, WorldType.SEASONAL));
		when(client.getWorldList()).thenReturn(null);
		plugin.onWorldsFetch(new WorldsFetch(serviceResult(301,
			EnumSet.of(net.runelite.http.api.worlds.WorldType.MEMBERS))));
		message(ChatMessageType.CLAN_MESSAGE, RAID_MESSAGE);
		assertEquals("[\"MEMBERS\",\"SEASONAL\"]", submitBroadcastAndCapture().getWorldType());
	}

	@Test
	public void missingMemberDoesNotCreateAClaimOrSuppressALaterResolvableBroadcast()
	{
		when(clan.findMember("Nerd Stark")).thenReturn(null);
		when(clan.getMembers()).thenReturn(java.util.Collections.emptyList());
		message(ChatMessageType.CLAN_MESSAGE, RAID_MESSAGE);
		plugin.onGameTick(new GameTick());
		verify(submissions, never()).submit(any(), any(), any(), any(), anyLong());
		verifyNoInteractions(drawManager);
		when(clan.findMember("Nerd Stark")).thenReturn(raidMember);
		message(ChatMessageType.CLAN_MESSAGE, RAID_MESSAGE);
		assertEquals("[\"MEMBERS\"]", submitBroadcastAndCapture().getWorldType());
	}

	@Test
	public void offlineOrUnlistedRecipientWorldDoesNotUseObserverFlags()
	{
		for (int worldId : new int[]{0, -1, 999})
		{
			when(raidMember.getWorld()).thenReturn(worldId);
			message(ChatMessageType.CLAN_MESSAGE, RAID_MESSAGE);
			plugin.onGameTick(new GameTick());
		}
		verify(submissions, never()).submit(any(), any(), any(), any(), anyLong());
		verifyNoInteractions(drawManager);
		verify(client).addChatMessage(eq(ChatMessageType.GAMEMESSAGE), eq(""),
			eq("[Clan clog] Recipient world data unavailable; please submit this drop manually."), isNull());
	}

	@Test
	public void missingWorldListOrWorldFlagsDoesNotUseObserverFlags()
	{
		when(recipientWorld.getTypes()).thenReturn(null);
		for (World[] worlds : new World[][]{null, new World[0], new World[]{null}, new World[]{recipientWorld}})
		{
			when(client.getWorldList()).thenReturn(worlds);
			message(ChatMessageType.CLAN_MESSAGE, RAID_MESSAGE);
			plugin.onGameTick(new GameTick());
		}
		verify(submissions, never()).submit(any(), any(), any(), any(), anyLong());
		verifyNoInteractions(drawManager);
	}

	@Test
	public void missingBroadcastWorldDataDoesNotPreventPersonalLootSubmission()
	{
		when(client.getWorldList()).thenReturn(null);
		message(ChatMessageType.CLAN_MESSAGE, RAID_MESSAGE);
		loot(4151);
		Submission receipt = submitPersonalAndCapture();
		assertEquals("Local Player", receipt.getUsername());
		assertEquals("[\"MEMBERS\"]", receipt.getWorldType());
		assertTrue(receipt.isPersonal());
		assertTrue(receipt.isLootEvent());
		verify(submissions, never()).submit(any(), isNull(), any(), any(), anyLong());
	}

	@Test
	public void ownBroadcastAloneIsIgnored()
	{
		message(ChatMessageType.CLAN_MESSAGE, "Local Player received a drop: Abyssal whip (1,000,000 coins).");
		plugin.onGameTick(new GameTick());
		verify(submissions, never()).submit(any(), any(), any(), any(), anyLong());
		verifyNoInteractions(catalog, drawManager);
	}

	@Test
	public void ownBroadcastNamesAreMatchedAcrossAllFormatsAndNormalized()
	{
		message(ChatMessageType.CLAN_MESSAGE, " <img=2>LOCAL\u00a0PLAYER received a new collection log item: Abyssal whip (10/1717) ");
		message(ChatMessageType.CLAN_MESSAGE, "local_player received a drop: Abyssal whip (1,000,000 coins).");
		message(ChatMessageType.CLAN_MESSAGE, "Local_  Player received special loot from a raid: Scythe of Vitur (uncharged) (1,188,452,005 coins).");
		plugin.onGameTick(new GameTick());
		verify(submissions, never()).submit(any(), any(), any(), any(), anyLong());
		verifyNoInteractions(catalog, drawManager);
	}

	@Test
	public void localDropIncludesExactMessageScreenshotAndFrozenMetadata()
	{
		String raw = " New item added to your collection log: <col=ff0000>Abyssal\u00a0whip</col> ";
		message(ChatMessageType.GAMEMESSAGE, "Valuable drop: Abyssal whip (1,000,000 coins)");
		message(ChatMessageType.GAMEMESSAGE, raw);
		message(ChatMessageType.CLAN_MESSAGE, "Local_Player received a new collection log item: Abyssal whip (10/1717)");
		popup("New item: Abyssal whip");
		when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.PVP));
		plugin.onGameTick(new GameTick());
		verify(submissions, never()).submit(any(), any(), any(), any(), anyLong());
		drawNextFrame();
		ArgumentCaptor<Submission> receipt = ArgumentCaptor.forClass(Submission.class);
		verify(submissions).submit(receipt.capture(), any(BufferedImage.class), any(), eq("test-key"), anyLong());
		assertEquals("Local Player", receipt.getValue().getUsername());
		assertEquals("Our Clan", receipt.getValue().getClanName());
		assertEquals(4151, receipt.getValue().getItemId());
		assertEquals("Abyssal whip", receipt.getValue().getItemName());
		assertEquals("[\"MEMBERS\"]", receipt.getValue().getWorldType());
		assertEquals(raw, receipt.getValue().getChatMessage());
		assertTrue(receipt.getValue().isPersonal());
	}

	@Test
	public void everyPriorityPairSelectsTheHigherMessageInEitherArrivalOrder()
	{
		for (int higher = 0; higher < PRIORITY_MESSAGES.length; higher++)
		{
			for (int lower = higher + 1; lower < PRIORITY_MESSAGES.length; lower++)
			{
				assertSelectedPriority(new int[]{higher, lower}, higher);
				assertSelectedPriority(new int[]{lower, higher}, higher);
			}
		}
	}

	@Test
	public void allSixTriggersSelectCollectionLogTextRegardlessOfArrivalOrder()
	{
		assertSelectedPriority(new int[]{0, 1, 2, 3, 4, 5}, 0);
		assertSelectedPriority(new int[]{5, 4, 3, 2, 1, 0}, 0);
		assertSelectedPriority(new int[]{4, 2, 5, 0, 3, 1}, 0);
	}

	@Test
	public void equalPriorityKeepsTheFirstOriginalMessage()
	{
		String first = PRIORITY_MESSAGES[2];
		message(ChatMessageType.GAMEMESSAGE, first);
		message(ChatMessageType.GAMEMESSAGE, " Valuable drop: <col=ff0000>Abyssal whip</col> (2,000,000 coins) ");
		assertEquals(first, submitPersonalAndCapture().getChatMessage());
	}

	@Test
	public void collectionBroadcastOutranksEarlierValuableBroadcastWithoutScreenshot()
	{
		message(ChatMessageType.CLAN_MESSAGE, "Other Player received a drop: Abyssal whip (1,000,000 coins).");
		String raw = "Other Player received a new collection log item: <col=ff0000>Abyssal whip</col> (10/1717)";
		message(ChatMessageType.CLAN_MESSAGE, raw);
		Submission receipt = submitBroadcastAndCapture();
		assertEquals(raw, receipt.getChatMessage());
		assertEquals(DetectionSource.COLLECTION_LOG_MESSAGE, receipt.getSource());
		assertFalse(receipt.isPersonal());
	}

	@Test
	public void higherPriorityCannotReplaceEvidenceAfterTheScreenshotBatchIsTaken()
	{
		loot(4151);
		plugin.onGameTick(new GameTick());
		message(ChatMessageType.GAMEMESSAGE, PRIORITY_MESSAGES[0]);
		drawNextFrame();
		plugin.onGameTick(new GameTick());
		ArgumentCaptor<Submission> receipt = ArgumentCaptor.forClass(Submission.class);
		verify(submissions).submit(receipt.capture(), any(BufferedImage.class), any(), any(), anyLong());
		assertEquals(PRIORITY_MESSAGES[4], receipt.getValue().getChatMessage());
		assertTrue(receipt.getValue().isLootEvent());
		verify(drawManager).requestNextFrameListener(any());
	}

	@Test
	public void priorityUpgradeDoesNotExtendTheFiveSecondWindow()
	{
		message(ChatMessageType.GAMEMESSAGE, PRIORITY_MESSAGES[2]);
		when(clock.millis()).thenReturn(1_004_000L);
		message(ChatMessageType.GAMEMESSAGE, PRIORITY_MESSAGES[0]);
		Submission first = submitPersonalAndCapture();
		when(clock.millis()).thenReturn(1_005_000L);
		message(ChatMessageType.GAMEMESSAGE, PRIORITY_MESSAGES[2]);
		plugin.onGameTick(new GameTick());
		ArgumentCaptor<Consumer<Image>> frames = ArgumentCaptor.forClass(Consumer.class);
		verify(drawManager, times(2)).requestNextFrameListener(frames.capture());
		frames.getAllValues().get(1).accept(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB));
		ArgumentCaptor<Submission> receipts = ArgumentCaptor.forClass(Submission.class);
		verify(submissions, times(2)).submit(receipts.capture(), any(BufferedImage.class), any(), any(), anyLong());
		Submission second = receipts.getAllValues().get(1);
		assertEquals(PRIORITY_MESSAGES[0], first.getChatMessage());
		assertEquals(PRIORITY_MESSAGES[2], second.getChatMessage());
		assertNotEquals(first.getIdempotencyKey(), second.getIdempotencyKey());
	}

	private void assertSelectedPriority(int[] arrivalOrder, int expected)
	{
		for (int source : arrivalOrder)
		{
			if (source == 1)
			{
				popup(PRIORITY_MESSAGES[source]);
			}
			else if (source == 4)
			{
				loot(4151);
			}
			else
			{
				message(ChatMessageType.GAMEMESSAGE, PRIORITY_MESSAGES[source]);
			}
		}
		Submission receipt = submitPersonalAndCapture();
		assertEquals(Arrays.toString(arrivalOrder), PRIORITY_MESSAGES[expected], receipt.getChatMessage());
		assertEquals(DetectionSource.values()[expected], receipt.getSource());
		// Each order is a separate acquisition/session, with exactly one claim and one frame.
		verify(drawManager).requestNextFrameListener(any());
		plugin.shutDown();
		plugin.startUp();
		clearInvocations(submissions, drawManager);
	}

	@Test
	public void ownBroadcastBeforePersonalMessageInAnotherTickIsIgnored()
	{
		message(ChatMessageType.CLAN_MESSAGE, "Local_Player received a drop: Abyssal whip (1,000,000 coins).");
		plugin.onGameTick(new GameTick());
		verify(submissions, never()).submit(any(), any(), any(), any(), anyLong());
		verifyNoInteractions(drawManager);
		String raw = "New item added to your collection log: Abyssal whip";
		message(ChatMessageType.GAMEMESSAGE, raw);
		plugin.onGameTick(new GameTick());
		drawNextFrame();
		ArgumentCaptor<Submission> receipt = ArgumentCaptor.forClass(Submission.class);
		verify(submissions).submit(receipt.capture(), any(BufferedImage.class), any(), any(), anyLong());
		assertEquals(raw, receipt.getValue().getChatMessage());
		assertTrue(receipt.getValue().isPersonal());
	}

	@Test
	public void ownBroadcastAfterPersonalSubmissionIsIgnored()
	{
		String raw = "New item added to your collection log: Abyssal whip";
		message(ChatMessageType.GAMEMESSAGE, raw);
		plugin.onGameTick(new GameTick());
		drawNextFrame();
		message(ChatMessageType.CLAN_MESSAGE, "Local_Player received a new collection log item: Abyssal whip (10/1717)");
		plugin.onGameTick(new GameTick());
		ArgumentCaptor<Submission> receipt = ArgumentCaptor.forClass(Submission.class);
		verify(submissions).submit(receipt.capture(), any(BufferedImage.class), any(), any(), anyLong());
		verify(submissions, never()).submit(any(), isNull(), any(), any(), anyLong());
		verify(drawManager).requestNextFrameListener(any());
		assertEquals(raw, receipt.getValue().getChatMessage());
		assertTrue(receipt.getValue().isPersonal());
	}

	@Test
	public void broadcastSendsWhilePersonalFrameIsStillPending()
	{
		message(ChatMessageType.GAMEMESSAGE, "New item added to your collection log: Abyssal whip");
		plugin.onGameTick(new GameTick());
		message(ChatMessageType.CLAN_MESSAGE, RAID_MESSAGE);
		plugin.onGameTick(new GameTick());
		verify(drawManager).requestNextFrameListener(any());
		verify(submissions).submit(any(), isNull(), any(), any(), anyLong());
		drawNextFrame();
		verify(submissions).submit(any(), any(BufferedImage.class), any(), any(), anyLong());
	}

	@Test
	public void untradeableAndPopupOnlyMessagesRetainTheirActualTriggerText()
	{
		when(catalog.find("Abyssal blue dye")).thenReturn(1234);
		message(ChatMessageType.SPAM, "Untradeable drop: Abyssal blue dye");
		String popupText = "<col=ff0000>New item: Abyssal whip</col>";
		popup(popupText);
		plugin.onGameTick(new GameTick());
		drawNextFrame();
		ArgumentCaptor<Submission> receipts = ArgumentCaptor.forClass(Submission.class);
		verify(submissions, times(2)).submit(receipts.capture(), any(BufferedImage.class), any(), any(), anyLong());
		assertEquals("Untradeable drop: Abyssal blue dye", receipts.getAllValues().get(0).getChatMessage());
		assertEquals(popupText, receipts.getAllValues().get(1).getChatMessage());
	}

	@Test
	public void silentLootSubmitsWithoutAnyChatMessageOrPriceThreshold()
	{
		loot(4151);
		when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.PVP));
		plugin.onGameTick(new GameTick());
		drawNextFrame();
		ArgumentCaptor<Submission> receipt = ArgumentCaptor.forClass(Submission.class);
		verify(submissions).submit(receipt.capture(), any(BufferedImage.class), any(), any(), anyLong());
		assertEquals("Local Player", receipt.getValue().getUsername());
		assertEquals("Our Clan", receipt.getValue().getClanName());
		assertEquals(4151, receipt.getValue().getItemId());
		assertEquals("[\"MEMBERS\"]", receipt.getValue().getWorldType());
		assertTrue(receipt.getValue().isPersonal());
		assertTrue(receipt.getValue().isLootEvent());
		assertEquals("RuneLite Loot Tracker event: Local Player obtained 1 x Abyssal whip (item ID: 4151), source: Abyssal demon, type: NPC.",
			receipt.getValue().getChatMessage());
	}

	@Test
	public void genuineMessageBeforeLootTakesPrecedenceWithoutDuplicate()
	{
		assertLootAndChat(false);
	}

	@Test
	public void genuineMessageAfterLootTakesPrecedenceWithoutDuplicate()
	{
		assertLootAndChat(true);
	}

	@Test
	public void silentLootEventsAndPickpocketsAreIncluded()
	{
		when(catalog.collectionItem(4151)).thenReturn(4151);
		when(catalog.collectionItem(102)).thenReturn(102);
		emitLoot(LootRecordType.EVENT, "Clue Scroll (easy)", new ItemStack(4151, 1));
		emitLoot(LootRecordType.PICKPOCKET, "H.A.M. Member", new ItemStack(102, 2));
		plugin.onGameTick(new GameTick());
		drawNextFrame();
		ArgumentCaptor<Submission> receipts = ArgumentCaptor.forClass(Submission.class);
		verify(submissions, times(2)).submit(receipts.capture(), any(BufferedImage.class), any(), any(), anyLong());
		assertTrue(receipts.getAllValues().get(0).getChatMessage().contains("source: Clue Scroll (easy), type: EVENT"));
		assertTrue(receipts.getAllValues().get(1).getChatMessage().contains("2 x Ancient page"));
		assertTrue(receipts.getAllValues().get(1).getChatMessage().contains("type: PICKPOCKET"));
	}

	@Test
	public void combinesNotedAndUnnotedStacksAndSuppressesRepeatedLootEvents()
	{
		when(catalog.collectionItem(4151)).thenReturn(4151);
		when(catalog.collectionItem(4152)).thenReturn(4151);
		for (int i = 0; i < 2; i++)
		{
			emitLoot(LootRecordType.NPC, "Abyssal demon", new ItemStack(4151, 2), new ItemStack(4152, 3));
		}
		plugin.onGameTick(new GameTick());
		drawNextFrame();
		ArgumentCaptor<Submission> receipt = ArgumentCaptor.forClass(Submission.class);
		verify(submissions).submit(receipt.capture(), any(BufferedImage.class), any(), any(), anyLong());
		assertEquals(4151, receipt.getValue().getItemId());
		assertTrue(receipt.getValue().getChatMessage().contains("5 x Abyssal whip"));
	}

	@Test
	public void nonCollectionItemsZeroQuantitiesAndPlayerLootAreExcluded()
	{
		when(catalog.collectionItem(4151)).thenReturn(4151);
		emitLoot(LootRecordType.NPC, "Abyssal demon", new ItemStack(999, 20), new ItemStack(4151, 0));
		emitLoot(LootRecordType.PLAYER, "Other Player", new ItemStack(4151, 1));
		plugin.onGameTick(new GameTick());
		verifyNoInteractions(drawManager);
		verify(submissions, never()).submit(any(), any(), any(), any(), anyLong());
	}

	@Test
	public void ownBroadcastBeforeSilentLootInAnotherTickIsIgnored()
	{
		message(ChatMessageType.CLAN_MESSAGE, "Local_Player received a drop: Abyssal whip (1,000,000 coins).");
		plugin.onGameTick(new GameTick());
		verify(submissions, never()).submit(any(), any(), any(), any(), anyLong());
		verifyNoInteractions(drawManager);
		loot(4151);
		plugin.onGameTick(new GameTick());
		drawNextFrame();
		ArgumentCaptor<Submission> receipt = ArgumentCaptor.forClass(Submission.class);
		verify(submissions).submit(receipt.capture(), any(BufferedImage.class), any(), any(), anyLong());
		assertTrue(receipt.getValue().isPersonal());
		assertTrue(receipt.getValue().isLootEvent());
	}

	@Test
	public void exactLootDisambiguatesEarlierLocalMessage()
	{
		assertAmbiguousLocalDrop(false);
	}

	@Test
	public void exactLootDisambiguatesLaterLocalMessage()
	{
		assertAmbiguousLocalDrop(true);
	}

	@Test
	public void expiredAmbiguousMessageIsNotReusedForLaterSilentLoot()
	{
		when(catalog.find(eq("Ancient page"), anySet())).thenAnswer(call ->
			((Set<Integer>) call.getArgument(1)).contains(102) ? 102 : null);
		message(ChatMessageType.GAMEMESSAGE, "Untradeable drop: Ancient page");
		for (int i = 0; i < 4; i++)
		{
			plugin.onGameTick(new GameTick());
		}
		loot(102);
		plugin.onGameTick(new GameTick());
		drawNextFrame();
		ArgumentCaptor<Submission> receipt = ArgumentCaptor.forClass(Submission.class);
		verify(submissions).submit(receipt.capture(), any(BufferedImage.class), any(), any(), anyLong());
		assertTrue(receipt.getValue().isLootEvent());
		assertTrue(receipt.getValue().getChatMessage().startsWith("RuneLite Loot Tracker event:"));
	}

	@Test
	public void observerLootCannotDisambiguateOtherPlayersBroadcast()
	{
		when(catalog.find(eq("Ancient page"), anySet())).thenReturn(102);
		loot(102);
		message(ChatMessageType.CLAN_MESSAGE, "Other Player received a new collection log item: Ancient page (10/1717)");
		plugin.onGameTick(new GameTick());
		drawNextFrame();
		ArgumentCaptor<Submission> receipt = ArgumentCaptor.forClass(Submission.class);
		verify(submissions).submit(receipt.capture(), any(BufferedImage.class), any(), any(), anyLong());
		assertEquals("Local Player", receipt.getValue().getUsername());
		assertTrue(receipt.getValue().isLootEvent());
	}

	@Test
	public void doesNotSubmitWhileLoggedOutOrForPlayerWrittenChat()
	{
		message(ChatMessageType.CLAN_CHAT, RAID_MESSAGE);
		when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
		message(ChatMessageType.GAMEMESSAGE, "New item added to your collection log: Abyssal whip");
		message(ChatMessageType.CLAN_MESSAGE, RAID_MESSAGE);
		plugin.onGameTick(new GameTick());
		verifyNoInteractions(drawManager);
		verify(submissions, never()).submit(any(), any(), any(), any(), anyLong());
	}

	@Test
	public void doesNotSubmitUnknownItemsOrWithoutAClan()
	{
		message(ChatMessageType.GAMEMESSAGE, "Valuable drop: Coins (1,000,000 coins)");
		when(client.getClanChannel(ClanID.CLAN)).thenReturn(null);
		when(client.getClanSettings(ClanID.CLAN)).thenReturn(null);
		message(ChatMessageType.GAMEMESSAGE, "Valuable drop: Abyssal whip (1,000,000 coins)");
		message(ChatMessageType.CLAN_MESSAGE, RAID_MESSAGE);
		plugin.onGameTick(new GameTick());
		verifyNoInteractions(drawManager);
		verify(submissions, never()).submit(any(), any(), any(), any(), anyLong());
	}

	@Test
	public void personalChatSubmitsForMembershipWhileOutsideClanChannel()
	{
		when(client.getClanChannel(ClanID.CLAN)).thenReturn(null);
		String raw = "New item added to your collection log: Abyssal whip";
		message(ChatMessageType.GAMEMESSAGE, raw);
		Submission receipt = submitPersonalAndCapture();
		assertEquals("Our Clan", receipt.getClanName());
		assertEquals(raw, receipt.getChatMessage());
		assertTrue(receipt.isPersonal());
	}

	@Test
	public void personalPopupSubmitsForMembershipWhileOutsideClanChannel()
	{
		when(client.getClanChannel(ClanID.CLAN)).thenReturn(null);
		popup("New item: Abyssal whip");
		Submission receipt = submitPersonalAndCapture();
		assertEquals("Our Clan", receipt.getClanName());
		assertEquals("New item: Abyssal whip", receipt.getChatMessage());
		assertTrue(receipt.isPersonal());
	}

	@Test
	public void silentLootSubmitsForMembershipWhileOutsideClanChannel()
	{
		when(client.getClanChannel(ClanID.CLAN)).thenReturn(null);
		loot(4151);
		Submission receipt = submitPersonalAndCapture();
		assertEquals("Our Clan", receipt.getClanName());
		assertEquals("Local Player", receipt.getUsername());
		assertTrue(receipt.isPersonal());
		assertTrue(receipt.isLootEvent());
	}

	@Test
	public void personalClanComesFromMembershipAndIsFrozenAtDetection()
	{
		ClanChannel channel = mock(ClanChannel.class);
		when(channel.getName()).thenReturn("Different Channel Clan");
		when(client.getClanChannel(ClanID.CLAN)).thenReturn(channel);
		message(ChatMessageType.GAMEMESSAGE, "New item added to your collection log: Abyssal whip");
		ClanSettings newMembership = mock(ClanSettings.class);
		when(newMembership.getName()).thenReturn("New Membership Clan");
		when(client.getClanSettings(ClanID.CLAN)).thenReturn(newMembership);
		assertEquals("Our Clan", submitPersonalAndCapture().getClanName());
	}

	@Test
	public void missingOrBlankMembershipDoesNotFallBackToChannelGuestOrGroup()
	{
		ClanSettings guest = mock(ClanSettings.class);
		when(guest.getName()).thenReturn("Guest Clan");
		when(client.getGuestClanSettings()).thenReturn(guest);
		ClanSettings group = mock(ClanSettings.class);
		when(group.getName()).thenReturn("Ironman Group");
		when(client.getClanSettings(ClanID.GROUP_IRONMAN)).thenReturn(group);
		ClanSettings blankMembership = mock(ClanSettings.class);
		when(blankMembership.getName()).thenReturn(" \u00a0<col=ff0000></col> ");
		for (ClanSettings membership : new ClanSettings[]{null, blankMembership})
		{
			when(client.getClanSettings(ClanID.CLAN)).thenReturn(membership);
			message(ChatMessageType.GAMEMESSAGE, "New item added to your collection log: Abyssal whip");
			plugin.onGameTick(new GameTick());
		}
		verify(submissions, never()).submit(any(), any(), any(), any(), anyLong());
		verifyNoInteractions(drawManager);
		verify(client).addChatMessage(eq(ChatMessageType.GAMEMESSAGE), eq(""),
			eq("[Clan clog] Clan membership data unavailable; please submit this drop manually."), isNull());
	}

	@Test
	public void broadcastsRequirePrimaryChannelEvenWhenMembershipIsAvailable()
	{
		ClanChannel blankChannel = mock(ClanChannel.class);
		when(blankChannel.getName()).thenReturn(" \u00a0 ");
		for (ClanChannel channel : new ClanChannel[]{null, blankChannel})
		{
			when(client.getClanChannel(ClanID.CLAN)).thenReturn(channel);
			message(ChatMessageType.CLAN_MESSAGE, RAID_MESSAGE);
			plugin.onGameTick(new GameTick());
		}
		verify(submissions, never()).submit(any(), any(), any(), any(), anyLong());
		verifyNoInteractions(drawManager);
	}

	@Test
	public void shutdownInvalidatesPendingFrame()
	{
		message(ChatMessageType.GAMEMESSAGE, "New item added to your collection log: Abyssal whip");
		plugin.onGameTick(new GameTick());
		plugin.shutDown();
		drawNextFrame();
		verify(submissions, never()).submit(any(), any(), any(), any(), anyLong());
	}

	@Test
	public void configChangeCancelsBothPendingFrameAndBroadcast()
	{
		message(ChatMessageType.GAMEMESSAGE, "New item added to your collection log: Abyssal whip");
		plugin.onGameTick(new GameTick());
		message(ChatMessageType.CLAN_MESSAGE, RAID_MESSAGE);
		ConfigChanged changed = mock(ConfigChanged.class);
		when(changed.getGroup()).thenReturn(ClanClogConfig.GROUP);
		plugin.onConfigChanged(changed);
		plugin.onGameTick(new GameTick());
		drawNextFrame();
		verify(submissions, never()).submit(any(), any(), any(), any(), anyLong());
		verify(submissions, times(2)).start(any());
	}

	@Test
	public void boundsFrameRequestsWhileRenderingIsPaused()
	{
		when(catalog.find("Abyssal blue dye")).thenReturn(1234);
		message(ChatMessageType.GAMEMESSAGE, "New item added to your collection log: Abyssal whip");
		plugin.onGameTick(new GameTick());
		message(ChatMessageType.SPAM, "Untradeable drop: Abyssal blue dye");
		plugin.onGameTick(new GameTick());
		plugin.onGameTick(new GameTick());
		verify(drawManager).requestNextFrameListener(any());
		drawNextFrame();
		plugin.onGameTick(new GameTick());
		verify(drawManager, times(2)).requestNextFrameListener(any());
	}

	@Test
	public void hoppingInvalidatesPendingFrameAndBroadcast()
	{
		message(ChatMessageType.GAMEMESSAGE, "New item added to your collection log: Abyssal whip");
		plugin.onGameTick(new GameTick());
		message(ChatMessageType.CLAN_MESSAGE, RAID_MESSAGE);
		GameStateChanged changed = mock(GameStateChanged.class);
		when(changed.getGameState()).thenReturn(GameState.HOPPING);
		plugin.onGameStateChanged(changed);
		plugin.onGameTick(new GameTick());
		drawNextFrame();
		verify(submissions, never()).submit(any(), any(), any(), any(), anyLong());
	}

	@Test
	public void valuableRepeatDropsAreSeparateAtFiveSeconds()
	{
		assertFiveSecondWindow("Valuable drop: Abyssal whip (1,000,000 coins)");
	}

	@Test
	public void untradeableRepeatDropsAreSeparateAtFiveSeconds()
	{
		when(catalog.find("Abyssal blue dye")).thenReturn(1234);
		assertFiveSecondWindow("Untradeable drop: Abyssal blue dye");
	}

	@Test
	public void silentRepeatLootEventsAreSeparateAtFiveSeconds()
	{
		loot(4151);
		when(clock.millis()).thenReturn(1_005_000L);
		loot(4151);
		plugin.onGameTick(new GameTick());
		drawNextFrame();
		verify(submissions, times(2)).submit(any(), any(BufferedImage.class), any(), any(), anyLong());
	}

	@Test
	public void decorativeArmourMessageResolvesFromLaterInventoryGainWithoutLootTracker()
	{
		ambiguousItems("Decorative armour", ItemID.CASTLEWARS_ARMOUR_BODY, ItemID.CASTLEWARS_ARMOUR_BODY_2);
		inventory(InventoryID.INV);
		String raw = "New item added to your collection log: Decorative armour";
		message(ChatMessageType.GAMEMESSAGE, raw);
		inventory(InventoryID.INV, new Item(ItemID.CASTLEWARS_ARMOUR_BODY_2, 1));
		Submission receipt = submitPersonalAndCapture();
		assertEquals(ItemID.CASTLEWARS_ARMOUR_BODY_2, receipt.getItemId());
		assertEquals(raw, receipt.getChatMessage());
		assertFalse(receipt.isLootEvent());
	}

	@Test
	public void gracefulPopupUsesNewGainRatherThanAlreadyOwnedVariant()
	{
		ambiguousItems("Graceful top", ItemID.GRACEFUL_TOP, ItemID.GRACEFUL_TOP_WYRM);
		inventory(InventoryID.INV, new Item(ItemID.GRACEFUL_TOP, 1));
		inventory(InventoryID.INV, new Item(ItemID.GRACEFUL_TOP, 1), new Item(ItemID.GRACEFUL_TOP_WYRM, 1));
		popup("New item: Graceful top");
		Submission receipt = submitPersonalAndCapture();
		assertEquals(ItemID.GRACEFUL_TOP_WYRM, receipt.getItemId());
		assertEquals("New item: Graceful top", receipt.getChatMessage());
	}

	@Test
	public void equipmentGainCanResolveAGenuineAmbiguousNotification()
	{
		ambiguousItems("Decorative armour", ItemID.CASTLEWARS_ARMOUR_BODY, ItemID.CASTLEWARS_ARMOUR_BODY_2);
		inventory(InventoryID.WORN);
		inventory(InventoryID.WORN, new Item(ItemID.CASTLEWARS_ARMOUR_BODY, 1));
		message(ChatMessageType.SPAM, "Untradeable drop: Decorative armour");
		assertEquals(ItemID.CASTLEWARS_ARMOUR_BODY, submitPersonalAndCapture().getItemId());
	}

	@Test
	public void equipmentGainsUseCanonicalCollectionIds()
	{
		ambiguousItems("Graceful top", ItemID.GRACEFUL_TOP, ItemID.GRACEFUL_TOP_WYRM);
		when(catalog.collectionItem(ItemID.GRACEFUL_TOP_WORN)).thenReturn(ItemID.GRACEFUL_TOP);
		inventory(InventoryID.WORN);
		inventory(InventoryID.WORN, new Item(ItemID.GRACEFUL_TOP_WORN, 1));
		popup("New item: Graceful top");
		assertEquals(ItemID.GRACEFUL_TOP, submitPersonalAndCapture().getItemId());
	}

	@Test
	public void startupSeedsInventoryBeforeTrackingNewAcquisitions()
	{
		ambiguousItems("Graceful top", ItemID.GRACEFUL_TOP, ItemID.GRACEFUL_TOP_WYRM);
		ItemContainer ownedItems = mock(ItemContainer.class);
		when(ownedItems.getItems()).thenReturn(new Item[]{new Item(ItemID.GRACEFUL_TOP, 1)});
		when(client.getItemContainer(InventoryID.INV)).thenReturn(ownedItems);
		plugin.shutDown();
		plugin.startUp();
		inventory(InventoryID.INV, new Item(ItemID.GRACEFUL_TOP, 1), new Item(ItemID.GRACEFUL_TOP_WYRM, 1));
		popup("New item: Graceful top");
		assertEquals(ItemID.GRACEFUL_TOP_WYRM, submitPersonalAndCapture().getItemId());
	}

	@Test
	public void worldHopClearsInventoryEvidence()
	{
		ambiguousItems("Graceful top", ItemID.GRACEFUL_TOP, ItemID.GRACEFUL_TOP_WYRM);
		inventory(InventoryID.INV);
		inventory(InventoryID.INV, new Item(ItemID.GRACEFUL_TOP, 1));
		GameStateChanged changed = mock(GameStateChanged.class);
		when(changed.getGameState()).thenReturn(GameState.HOPPING);
		plugin.onGameStateChanged(changed);
		popup("New item: Graceful top");
		plugin.onGameTick(new GameTick());
		verifyNoInteractions(drawManager);
		verify(submissions, never()).submit(any(), any(), any(), any(), anyLong());
	}

	@Test
	public void inventoryAndEquipmentChangesAloneNeverCreateClaims()
	{
		ambiguousItems("Decorative armour", ItemID.CASTLEWARS_ARMOUR_BODY, ItemID.CASTLEWARS_ARMOUR_BODY_2);
		inventory(InventoryID.INV);
		inventory(InventoryID.WORN);
		inventory(InventoryID.INV, new Item(ItemID.CASTLEWARS_ARMOUR_BODY, 1));
		inventory(InventoryID.WORN, new Item(ItemID.CASTLEWARS_ARMOUR_BODY_2, 1));
		plugin.onGameTick(new GameTick());
		verifyNoInteractions(drawManager);
		verify(submissions, never()).submit(any(), any(), any(), any(), anyLong());
	}

	@Test
	public void firstInventorySnapshotDoesNotResolveNotificationWithExistingItems()
	{
		ambiguousItems("Decorative armour", ItemID.CASTLEWARS_ARMOUR_BODY, ItemID.CASTLEWARS_ARMOUR_BODY_2);
		message(ChatMessageType.GAMEMESSAGE, "New item added to your collection log: Decorative armour");
		inventory(InventoryID.INV, new Item(ItemID.CASTLEWARS_ARMOUR_BODY, 1));
		plugin.onGameTick(new GameTick());
		verifyNoInteractions(drawManager);
		verify(submissions, never()).submit(any(), any(), any(), any(), anyLong());
	}

	@Test
	public void expiredOrConflictingInventoryEvidenceIsNotGuessed()
	{
		ambiguousItems("Decorative armour", ItemID.CASTLEWARS_ARMOUR_BODY, ItemID.CASTLEWARS_ARMOUR_BODY_2);
		inventory(InventoryID.INV);
		inventory(InventoryID.INV, new Item(ItemID.CASTLEWARS_ARMOUR_BODY, 1));
		for (int i = 0; i < 4; i++)
		{
			plugin.onGameTick(new GameTick());
		}
		message(ChatMessageType.GAMEMESSAGE, "New item added to your collection log: Decorative armour");
		plugin.onGameTick(new GameTick());
		inventory(InventoryID.INV, new Item(ItemID.CASTLEWARS_ARMOUR_BODY, 2), new Item(ItemID.CASTLEWARS_ARMOUR_BODY_2, 1));
		plugin.onGameTick(new GameTick());
		verifyNoInteractions(drawManager);
		verify(submissions, never()).submit(any(), any(), any(), any(), anyLong());
	}

	@Test
	public void localInventoryCannotIdentifyAnotherPlayersAmbiguousBroadcast()
	{
		ambiguousItems("Decorative armour", ItemID.CASTLEWARS_ARMOUR_BODY, ItemID.CASTLEWARS_ARMOUR_BODY_2);
		inventory(InventoryID.INV);
		inventory(InventoryID.INV, new Item(ItemID.CASTLEWARS_ARMOUR_BODY, 1));
		message(ChatMessageType.CLAN_MESSAGE, "Other Player received a new collection log item: Decorative armour (10/1717)");
		plugin.onGameTick(new GameTick());
		verifyNoInteractions(drawManager);
		verify(submissions, never()).submit(any(), any(), any(), any(), anyLong());
	}

	private void assertFiveSecondWindow(String raw)
	{
		message(ChatMessageType.GAMEMESSAGE, raw);
		when(clock.millis()).thenReturn(1_004_999L);
		message(ChatMessageType.GAMEMESSAGE, raw);
		when(clock.millis()).thenReturn(1_005_000L);
		message(ChatMessageType.GAMEMESSAGE, raw);
		plugin.onGameTick(new GameTick());
		drawNextFrame();
		ArgumentCaptor<Submission> receipts = ArgumentCaptor.forClass(Submission.class);
		verify(submissions, times(2)).submit(receipts.capture(), any(BufferedImage.class), any(), any(), anyLong());
		assertNotEquals(receipts.getAllValues().get(0).getIdempotencyKey(), receipts.getAllValues().get(1).getIdempotencyKey());
		assertEquals(raw, receipts.getAllValues().get(0).getChatMessage());
		assertEquals(raw, receipts.getAllValues().get(1).getChatMessage());
	}

	private void ambiguousItems(String name, int first, int second)
	{
		when(catalog.collectionItem(first)).thenReturn(first);
		when(catalog.collectionItem(second)).thenReturn(second);
		when(catalog.find(eq(name), anySet())).thenAnswer(call ->
		{
			Set<Integer> matches = new HashSet<>((Set<Integer>) call.getArgument(1));
			matches.retainAll(Set.of(first, second));
			return matches.size() == 1 ? matches.iterator().next() : null;
		});
	}

	private void inventory(int containerId, Item... items)
	{
		ItemContainer container = mock(ItemContainer.class);
		when(container.getItems()).thenReturn(items);
		plugin.onItemContainerChanged(new ItemContainerChanged(containerId, container));
	}

	private Submission submitBroadcastAndCapture()
	{
		plugin.onGameTick(new GameTick());
		ArgumentCaptor<Submission> receipt = ArgumentCaptor.forClass(Submission.class);
		verify(submissions).submit(receipt.capture(), isNull(), any(), any(), anyLong());
		verifyNoInteractions(drawManager);
		return receipt.getValue();
	}

	private Submission submitPersonalAndCapture()
	{
		plugin.onGameTick(new GameTick());
		drawNextFrame();
		ArgumentCaptor<Submission> receipt = ArgumentCaptor.forClass(Submission.class);
		verify(submissions).submit(receipt.capture(), any(BufferedImage.class), any(), any(), anyLong());
		return receipt.getValue();
	}

	private void assertAmbiguousLocalDrop(boolean lootFirst)
	{
		when(catalog.find(eq("Ancient page"), anySet())).thenAnswer(call ->
			((Set<Integer>) call.getArgument(1)).contains(102) ? 102 : null);
		String raw = "Untradeable drop: Ancient page";
		if (lootFirst)
		{
			loot(102);
		}
		message(ChatMessageType.GAMEMESSAGE, raw);
		if (!lootFirst)
		{
			loot(102);
		}
		plugin.onGameTick(new GameTick());
		drawNextFrame();
		ArgumentCaptor<Submission> receipt = ArgumentCaptor.forClass(Submission.class);
		verify(submissions).submit(receipt.capture(), any(BufferedImage.class), any(), any(), anyLong());
		assertEquals(102, receipt.getValue().getItemId());
		assertEquals(raw, receipt.getValue().getChatMessage());
		assertFalse(receipt.getValue().isLootEvent());
	}

	private void assertLootAndChat(boolean lootFirst)
	{
		String raw = "New item added to your collection log: <col=ff0000>Abyssal whip</col>";
		if (lootFirst)
		{
			loot(4151);
		}
		message(ChatMessageType.GAMEMESSAGE, raw);
		if (!lootFirst)
		{
			loot(4151);
		}
		plugin.onGameTick(new GameTick());
		drawNextFrame();
		ArgumentCaptor<Submission> receipt = ArgumentCaptor.forClass(Submission.class);
		verify(submissions).submit(receipt.capture(), any(BufferedImage.class), any(), any(), anyLong());
		assertEquals(raw, receipt.getValue().getChatMessage());
		assertFalse(receipt.getValue().isLootEvent());
	}

	private void loot(int itemId)
	{
		when(catalog.collectionItem(itemId)).thenReturn(itemId);
		emitLoot(LootRecordType.NPC, "Abyssal demon", new ItemStack(itemId, 1));
	}

	private void emitLoot(LootRecordType type, String source, ItemStack... items)
	{
		LootReceived event = mock(LootReceived.class);
		when(event.getType()).thenReturn(type);
		when(event.getName()).thenReturn(source);
		when(event.getItems()).thenReturn(Arrays.asList(items));
		plugin.onLootReceived(event);
	}

	private void message(ChatMessageType type, String value)
	{
		ChatMessage message = mock(ChatMessage.class);
		when(message.getType()).thenReturn(type);
		when(message.getMessage()).thenReturn(value);
		plugin.onChatMessage(message);
	}

	private void notification(String text)
	{
		ArgumentCaptor<Consumer<String>> notifications = ArgumentCaptor.forClass(Consumer.class);
		verify(submissions).start(notifications.capture());
		notifications.getValue().accept(text);
	}

	private void popup(String text)
	{
		ScriptPreFired start = mock(ScriptPreFired.class);
		when(start.getScriptId()).thenReturn(ScriptID.NOTIFICATION_START);
		plugin.onScriptPreFired(start);
		when(client.getVarcStrValue(VarClientID.NOTIFICATION_TITLE)).thenReturn("Collection log");
		when(client.getVarcStrValue(VarClientID.NOTIFICATION_MAIN)).thenReturn(text);
		ScriptPreFired delay = mock(ScriptPreFired.class);
		when(delay.getScriptId()).thenReturn(ScriptID.NOTIFICATION_DELAY);
		plugin.onScriptPreFired(delay);
	}

	private void drawNextFrame()
	{
		ArgumentCaptor<Consumer<Image>> capture = ArgumentCaptor.forClass(Consumer.class);
		verify(drawManager).requestNextFrameListener(capture.capture());
		capture.getValue().accept(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB));
	}
}
