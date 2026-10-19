package com.clanclogclient;

import com.google.inject.Provides;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
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
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.DrawManager;
import net.runelite.http.api.loottracker.LootRecordType;
import okhttp3.HttpUrl;

@Slf4j
@PluginDescriptor(name = "Clan Clog Client", description = "Send obtained item data to Clan Clog Submission API, full functionality requires LootTracker",
	tags = {"clan", "collection", "log", "loot", "drops", "api"})
public class ClanClogPlugin extends Plugin
{
	private static final long DEDUPLICATION_MILLIS = 5_000;
	private static final int ITEM_CORRELATION_TICKS = 3;
	private static final int MAX_PENDING = SubmissionClient.MAX_PENDING;
	private static final int[] REWARD_DIALOGS = {
		InterfaceID.ChatLeft.TEXT, InterfaceID.Messagebox.TEXT,
		InterfaceID.Objectbox.TEXT, InterfaceID.ObjectboxDouble.TEXT, InterfaceID.Chatmenu.OPTIONS
	};
	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private DrawManager drawManager;
	@Inject private ClanClogConfig config;
	@Inject private CollectionLogCatalog catalog;
	@Inject private SubmissionClient submissions;

	private final DropMessageParser parser = new DropMessageParser();
	private final RewardMessageParser rewardParser = new RewardMessageParser();
	private final Map<Integer, String> dialogTexts = new HashMap<>();
	private final List<PendingReward> pendingRewards = new ArrayList<>();
	private final Map<String, Long> recentDrops = new HashMap<>();
	private final Map<String, Submission> pendingDrops = new LinkedHashMap<>();
	private final Map<Integer, Integer> recentLoot = new HashMap<>();
	private final Map<Integer, Integer> recentInventoryItems = new HashMap<>();
	private final Map<Integer, Map<Integer, Long>> containerSnapshots = new HashMap<>();
	private final List<UnresolvedMessage> unresolvedMessages = new ArrayList<>();
	private final Clock clock;
	private volatile boolean running;
	private volatile int generation;
	private volatile long uploadSession;
	private volatile boolean frameRequested;
	private boolean popupStarted;
	private int tickCount;
	private String lastWarning;

	public ClanClogPlugin()
	{
		this(Clock.systemUTC());
	}

	ClanClogPlugin(Clock clock)
	{
		this.clock = clock;
	}

	@Override
	protected void startUp()
	{
		running = true;
		generation++;
		uploadSession = submissions.start(this::notifyPlayer);
		clientThread.invoke(() ->
		{
			if (ready())
			{
				captureInventoryBaseline();
				captureDialogBaseline();
			}
		});
	}

	@Override
	protected void shutDown()
	{
		running = false;
		generation++;
		submissions.stop();
		reset();
		catalog.clear();
	}

	@Provides
	ClanClogConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(ClanClogConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (ClanClogConfig.GROUP.equals(event.getGroup()))
		{
			clientThread.invoke(() ->
			{
				if (running)
				{
					if (ClanClogConfig.SUBMISSION_CHAT_NOTIFICATION.equals(event.getKey()))
					{
						lastWarning = null;
						return;
					}
					generation++;
					reset();
					captureInventoryBaseline();
					captureDialogBaseline();
					uploadSession = submissions.start(this::notifyPlayer);
				}
			});
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
		{
			generation++;
			pendingDrops.clear();
			recentLoot.clear();
			recentInventoryItems.clear();
			containerSnapshots.clear();
			unresolvedMessages.clear();
			pendingRewards.clear();
			dialogTexts.clear();
			frameRequested = false;
			popupStarted = false;
			if (event.getGameState() == GameState.LOGIN_SCREEN)
			{
				catalog.clear();
			}
		}
		else if (event.getGameState() == GameState.LOGGED_IN)
		{
			captureInventoryBaseline();
			captureDialogBaseline();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int containerId = event.getContainerId();
		if (!ready() || (containerId != InventoryID.INV && containerId != InventoryID.WORN))
		{
			return;
		}
		Map<Integer, Long> current = itemQuantities(event.getItemContainer());
		Map<Integer, Long> previous = containerSnapshots.put(containerId, current);
		if (previous == null)
		{
			return;
		}
		for (Map.Entry<Integer, Long> item : current.entrySet())
		{
			if (item.getValue() > previous.getOrDefault(item.getKey(), 0L) && loadCatalog())
			{
				Integer id = catalog.collectionItem(item.getKey());
				if (id != null)
				{
					recentInventoryItems.put(id, tickCount + ITEM_CORRELATION_TICKS);
				}
			}
		}
		resolveMessages();
		resolveRewards();
	}

	private void captureInventoryBaseline()
	{
		for (int containerId : new int[]{InventoryID.INV, InventoryID.WORN})
		{
			ItemContainer container = client.getItemContainer(containerId);
			if (container != null)
			{
				containerSnapshots.put(containerId, itemQuantities(container));
			}
		}
	}

	private static Map<Integer, Long> itemQuantities(ItemContainer container)
	{
		Map<Integer, Long> quantities = new HashMap<>();
		for (Item item : container.getItems())
		{
			if (item.getId() >= 0 && item.getQuantity() > 0)
			{
				quantities.merge(item.getId(), (long) item.getQuantity(), Long::sum);
			}
		}
		return quantities;
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (ready())
		{
			parser.parse(event.getType(), event.getMessage(), localUsername()).ifPresent(this::recordMessage);
			if (event.getType() == ChatMessageType.DIALOG || event.getType() == ChatMessageType.MESBOX)
			{
				recordDialog(event.getMessage(), 0);
			}
			if (event.getType() == ChatMessageType.GAMEMESSAGE || event.getType() == ChatMessageType.SPAM)
			{
				Integer item = rewardParser.chatItem(event.getMessage());
				if (item != null)
				{
					recordReward(event.getMessage(), item);
				}
				else if (rewardParser.isArenaLap(event.getMessage()))
				{
					addPendingReward(new PendingReward(event.getMessage(), Set.of(ItemID.AGILITYARENA_TICKET,
						ItemID.AGILITYARENA_TICKET_NEW, ItemID.AGILITYARENA_VOUCHER), tickCount + ITEM_CORRELATION_TICKS));
				}
			}
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (!ready())
		{
			return;
		}
		boolean rewardDialog = false;
		for (int component : REWARD_DIALOGS)
		{
			if ((component >>> 16) == event.getGroupId())
			{
				dialogTexts.remove(component);
				rewardDialog = true;
			}
		}
		if (rewardDialog)
		{
			observeRewardDialogs();
			int observedGeneration = generation;
			clientThread.invokeLater(() ->
			{
				if (ready() && observedGeneration == generation)
				{
					observeRewardDialogs();
				}
			});
		}
	}

	private String dialogText(int component)
	{
		Widget widget = client.getWidget(component);
		if (widget == null || widget.isHidden())
		{
			return "";
		}
		if (component == InterfaceID.Chatmenu.OPTIONS)
		{
			Widget[] children = widget.getChildren();
			if (children != null && children.length > 0 && children[0] != null && !children[0].isHidden())
			{
				return children[0].getText();
			}
		}
		return widget.getText();
	}

	private void captureDialogBaseline()
	{
		for (int component : REWARD_DIALOGS)
		{
			dialogTexts.put(component, dialogText(component));
		}
	}

	private void observeRewardDialogs()
	{
		for (int component : REWARD_DIALOGS)
		{
			String raw = dialogText(component);
			for (PendingReward reward : pendingRewards)
			{
				if (reward.component == component && reward.message.equals(raw))
				{
					reward.expiresAtTick = tickCount + ITEM_CORRELATION_TICKS;
				}
			}
			String previous = dialogTexts.put(component, raw);
			if (DropMessageParser.clean(raw).isEmpty() || raw.equals(previous))
			{
				continue;
			}
			recordDialog(raw, component);
		}
	}

	private void recordDialog(String raw, int component)
	{
		Integer item = rewardParser.dialogItem(raw);
		if (item != null)
		{
			recordReward(raw, item);
			return;
		}
		Integer upgrade = rewardParser.anglerUpgrade(raw);
		Map<Integer, Long> baseline = containerSnapshots.get(InventoryID.INV);
		if (upgrade != null && baseline != null)
		{
			for (PendingReward pending : pendingRewards)
			{
				if (pending.conversionItem == upgrade && DropMessageParser.identity(pending.message).equals(DropMessageParser.identity(raw)))
				{
					if (component != 0)
					{
						pending.component = component;
					}
					pending.expiresAtTick = tickCount + ITEM_CORRELATION_TICKS;
					return;
				}
			}
			PendingReward reward = new PendingReward(raw, Set.of(upgrade), tickCount + ITEM_CORRELATION_TICKS);
			reward.component = component;
			reward.conversionItem = upgrade;
			reward.baseline = new HashMap<>(baseline);
			addPendingReward(reward);
		}
	}

	private void recordReward(String raw, int rawItemId)
	{
		if (loadCatalog())
		{
			Integer id = catalog.collectionItem(rawItemId);
			if (id != null)
			{
				queueDrop(new DropMessageParser.Drop(localUsername(), catalog.itemName(id), false, raw, DetectionSource.OTHER), id);
			}
		}
	}

	private void addPendingReward(PendingReward reward)
	{
		if (pendingRewards.size() < MAX_PENDING)
		{
			pendingRewards.add(reward);
			resolveRewards();
		}
	}

	private void resolveRewards()
	{
		Iterator<PendingReward> iterator = pendingRewards.iterator();
		while (iterator.hasNext())
		{
			PendingReward reward = iterator.next();
			if (reward.expiresAtTick < tickCount)
			{
				iterator.remove();
				continue;
			}
			if (!loadCatalog())
			{
				continue;
			}
			Iterator<Integer> items = reward.items.iterator();
			while (items.hasNext())
			{
				int rawId = items.next();
				Integer id = catalog.collectionItem(rawId);
				boolean obtained = reward.conversionItem != 0 ? completedConversion(reward)
					: id != null && recentInventoryItems.containsKey(id);
				if (id != null && obtained)
				{
					recordReward(reward.message, rawId);
					items.remove();
				}
			}
			if (reward.items.isEmpty())
			{
				iterator.remove();
			}
		}
	}

	private boolean completedConversion(PendingReward reward)
	{
		Map<Integer, Long> current = containerSnapshots.get(InventoryID.INV);
		int original = RewardMessageParser.anglerInput(reward.conversionItem);
		return current != null
			&& current.getOrDefault(reward.conversionItem, 0L) > reward.baseline.getOrDefault(reward.conversionItem, 0L)
			&& current.getOrDefault(original, 0L) < reward.baseline.getOrDefault(original, 0L)
			&& reward.baseline.getOrDefault(ItemID.SPIRIT_FLAKES, 0L) - current.getOrDefault(ItemID.SPIRIT_FLAKES, 0L) >= 1_200;
	}

	@Subscribe
	public void onLootReceived(LootReceived event)
	{
		if (!ready() || event.getType() == LootRecordType.PLAYER || !loadCatalog())
		{
			return;
		}
		Map<Integer, Long> quantities = new LinkedHashMap<>();
		for (ItemStack stack : event.getItems())
		{
			Integer id = catalog.collectionItem(stack.getId());
			if (id != null && stack.getQuantity() > 0)
			{
				recentLoot.put(id, tickCount + ITEM_CORRELATION_TICKS);
				quantities.merge(id, (long) stack.getQuantity(), Long::sum);
			}
		}
		resolveMessages();
		String username = localUsername();
		String source = DropMessageParser.clean(event.getName());
		if (source.isEmpty())
		{
			source = "unknown source";
		}
		for (Map.Entry<Integer, Long> entry : quantities.entrySet())
		{
			int id = entry.getKey();
			String itemName = catalog.itemName(id);
			if (itemName == null || itemName.isEmpty())
			{
				itemName = "item " + id;
			}
			String evidence = "RuneLite Loot Tracker event: " + username + " obtained " + entry.getValue()
				+ " x " + itemName + " (item ID: " + id + "), source: " + source + ", type: " + event.getType() + ".";
			queueDrop(new DropMessageParser.Drop(username, itemName, false, evidence, DetectionSource.LOOT_TRACKER), id);
		}
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (!ready())
		{
			return;
		}
		if (event.getScriptId() == ScriptID.NOTIFICATION_START)
		{
			popupStarted = true;
		}
		else if (event.getScriptId() == ScriptID.NOTIFICATION_DELAY)
		{
			boolean started = popupStarted;
			popupStarted = false;
			String title = DropMessageParser.clean(client.getVarcStrValue(VarClientID.NOTIFICATION_TITLE));
			String rawDetail = client.getVarcStrValue(VarClientID.NOTIFICATION_MAIN);
			String detail = DropMessageParser.clean(rawDetail);
			if (started && "Collection log".equalsIgnoreCase(title) && detail.startsWith("New item:"))
			{
				recordMessage(new DropMessageParser.Drop(localUsername(), detail.substring("New item:".length()).trim(), false,
					rawDetail, DetectionSource.COLLECTION_LOG_POPUP));
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		tickCount++;
		long now = clock.millis();
		recentDrops.entrySet().removeIf(entry -> now - entry.getValue() >= DEDUPLICATION_MILLIS);
		recentLoot.entrySet().removeIf(entry -> entry.getValue() < tickCount);
		recentInventoryItems.entrySet().removeIf(entry -> entry.getValue() < tickCount);
		if (ready())
		{
			observeRewardDialogs();
			resolveRewards();
		}
		resolveMessages();
		if (pendingDrops.isEmpty())
		{
			return;
		}
		final HttpUrl endpoint;
		try
		{
			endpoint = SubmissionClient.endpoint(config.submissionUrl());
		}
		catch (IllegalArgumentException ex)
		{
			pendingDrops.clear();
			warnOnce(ex.getMessage());
			return;
		}
		String apiKey = config.clanApiKey();
		Iterator<Submission> iterator = pendingDrops.values().iterator();
		while (iterator.hasNext())
		{
			Submission drop = iterator.next();
			if (!drop.isPersonal())
			{
				submissions.submit(drop, null, endpoint, apiKey, uploadSession);
				iterator.remove();
			}
		}
		if (pendingDrops.isEmpty() || frameRequested)
		{
			return;
		}
		List<Submission> drops = new ArrayList<>(pendingDrops.values());
		pendingDrops.clear();
		int frameGeneration = generation;
		long frameSession = uploadSession;
		frameRequested = true;
		drawManager.requestNextFrameListener(frame ->
		{
			try
			{
				if (!running || frameGeneration != generation)
				{
					return;
				}
				BufferedImage screenshot = copyFrame(frame);
				if (screenshot == null)
				{
					notifyPlayer("Unable to capture a game screenshot; please submit the drop manually.");
					return;
				}
				for (Submission drop : drops)
				{
					if (running && frameGeneration == generation)
					{
						submissions.submit(drop, screenshot, endpoint, apiKey, frameSession);
					}
				}
			}
			finally
			{
				if (frameGeneration == generation)
				{
					frameRequested = false;
				}
			}
		});
	}

	private void recordMessage(DropMessageParser.Drop drop)
	{
		if (drop.isBroadcast() && DropMessageParser.identity(drop.getUsername()).equals(DropMessageParser.identity(localUsername())))
		{
			return;
		}
		if (!loadCatalog())
		{
			return;
		}
		Integer itemId = findItem(drop);
		if (itemId != null)
		{
			queueDrop(drop, itemId);
		}
		else if (!drop.isBroadcast() && unresolvedMessages.size() < MAX_PENDING)
		{
			unresolvedMessages.add(new UnresolvedMessage(drop, tickCount + ITEM_CORRELATION_TICKS));
		}
		else
		{
			log.debug("Skipped unknown or ambiguous collection log item: {}", drop.getItemName());
		}
	}

	private Integer findItem(DropMessageParser.Drop drop)
	{
		Integer id = catalog.find(drop.getItemName());
		if (id == null && !drop.isBroadcast())
		{
			Set<Integer> exactIds = new HashSet<>(recentLoot.keySet());
			exactIds.addAll(recentInventoryItems.keySet());
			id = catalog.find(drop.getItemName(), exactIds);
		}
		return id;
	}

	private void resolveMessages()
	{
		Iterator<UnresolvedMessage> iterator = unresolvedMessages.iterator();
		while (iterator.hasNext())
		{
			UnresolvedMessage pending = iterator.next();
			if (pending.expiresAtTick < tickCount)
			{
				log.debug("Skipped unknown or ambiguous collection log item: {}", pending.drop.getItemName());
				iterator.remove();
				continue;
			}
			Integer id = findItem(pending.drop);
			if (id != null)
			{
				queueDrop(pending.drop, id);
				iterator.remove();
			}
		}
	}

	private boolean ready()
	{
		return running && client.getGameState() == GameState.LOGGED_IN;
	}

	private String localUsername()
	{
		Player player = client.getLocalPlayer();
		return player == null ? "" : DropMessageParser.clean(player.getName());
	}

	private boolean loadCatalog()
	{
		try
		{
			catalog.load();
			return catalog.isLoaded();
		}
		catch (RuntimeException ex)
		{
			log.debug("Collection log cache could not be read", ex);
			warnOnce("Collection log cache unavailable; please submit this drop manually.");
			return false;
		}
	}

	private String submissionClanName(boolean broadcast)
	{
		if (broadcast)
		{
			ClanChannel channel = client.getClanChannel(ClanID.CLAN);
			return channel == null ? "" : DropMessageParser.clean(channel.getName());
		}
		ClanSettings clan = client.getClanSettings(ClanID.CLAN);
		return clan == null ? "" : DropMessageParser.clean(clan.getName());
	}

	private EnumSet<WorldType> submissionWorldTypes(DropMessageParser.Drop drop)
	{
		if (!drop.isBroadcast())
		{
			return client.getWorldType();
		}
		ClanChannel channel = client.getClanChannel(ClanID.CLAN);
		if (channel == null)
		{
			return null;
		}
		ClanChannelMember member = channel.findMember(drop.getUsername());
		if (member == null)
		{
			String identity = DropMessageParser.identity(drop.getUsername());
			for (ClanChannelMember candidate : channel.getMembers())
			{
				if (identity.equals(DropMessageParser.identity(candidate.getName())))
				{
					member = candidate;
					break;
				}
			}
		}
		int worldId = member == null ? 0 : member.getWorld();
		if (worldId <= 0)
		{
			return null;
		}
		if (worldId == client.getWorld())
		{
			return client.getWorldType();
		}
		World[] worlds = client.getWorldList();
		if (worlds != null)
		{
			for (World world : worlds)
			{
				if (world != null && world.getId() == worldId)
				{
					return world.getTypes();
				}
			}
		}
		return null;
	}

	private void queueDrop(DropMessageParser.Drop drop, int itemId)
	{
		String clanName = submissionClanName(drop.isBroadcast());
		if (clanName.isEmpty() || drop.getUsername().isEmpty())
		{
			warnOnce(drop.isBroadcast() ? "Join your clan channel before submitting broadcasts."
				: "Clan membership data unavailable; please submit this drop manually.");
			return;
		}
		try
		{
			SubmissionClient.validateChatMessage(drop.getChatMessage());
		}
		catch (IllegalArgumentException ex)
		{
			warnOnce(ex.getMessage());
			return;
		}
		EnumSet<WorldType> worldTypes = submissionWorldTypes(drop);
		if (worldTypes == null)
		{
			log.debug("Skipped submission with unavailable recipient world data: {}", drop.getUsername());
			warnOnce("Recipient world data unavailable; please submit this drop manually.");
			return;
		}
		String flags = worldTypes.stream()
			.map(type -> "\"" + type.name() + "\"")
			.collect(Collectors.joining(",", "[", "]"));
		String key = DropMessageParser.identity(drop.getUsername()) + ":" + itemId + ":" + DropMessageParser.identity(clanName) + ":" + flags;
		long now = clock.millis();
		Long previous = recentDrops.get(key);
		if (previous != null && now - previous < DEDUPLICATION_MILLIS)
		{
			String pendingKey = key + ":" + previous;
			Submission existing = pendingDrops.get(pendingKey);
			if (existing != null && drop.getSource().outranks(existing.getSource()))
			{
				pendingDrops.put(pendingKey, new Submission(existing.getIdempotencyKey(), existing.getUsername(), existing.getClanName(),
					existing.getItemId(), existing.getItemName(), existing.getWorldType(), drop.getChatMessage(),
					existing.isPersonal(), drop.getSource()));
			}
			return;
		}
		if (pendingDrops.size() >= MAX_PENDING)
		{
			warnOnce("Too many pending drops; please submit missed drops manually.");
			return;
		}
		recentDrops.put(key, now);
		pendingDrops.put(key + ":" + now, new Submission(UUID.randomUUID(), drop.getUsername(), clanName, itemId,
			DropMessageParser.clean(drop.getItemName()), flags,
			drop.getChatMessage(), !drop.isBroadcast(), drop.getSource()));
	}

	private void warnOnce(String warning)
	{
		if (!warning.equals(lastWarning))
		{
			lastWarning = warning;
			notifyPlayer(warning);
		}
	}

	private void notifyPlayer(String message)
	{
		int notificationGeneration = generation;
		clientThread.invoke(() ->
		{
			if (running && notificationGeneration == generation && client.getGameState() == GameState.LOGGED_IN
				&& config.submissionChatNotification())
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "[Clan clog] " + message, null);
			}
		});
	}

	private void reset()
	{
		recentDrops.clear();
		pendingDrops.clear();
		recentLoot.clear();
		recentInventoryItems.clear();
		containerSnapshots.clear();
		unresolvedMessages.clear();
		pendingRewards.clear();
		dialogTexts.clear();
		tickCount = 0;
		frameRequested = false;
		popupStarted = false;
		lastWarning = null;
	}

	private static BufferedImage copyFrame(Image frame)
	{
		int width = frame.getWidth(null);
		int height = frame.getHeight(null);
		if (width <= 0 || height <= 0 || width > 8192 || height > 8192 || (long) width * height > 32_000_000)
		{
			return null;
		}
		BufferedImage copy = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = copy.createGraphics();
		try
		{
			graphics.drawImage(frame, 0, 0, null);
		}
		finally
		{
			graphics.dispose();
		}
		return copy;
	}

	private static final class UnresolvedMessage
	{
		private final DropMessageParser.Drop drop;
		private final int expiresAtTick;

		private UnresolvedMessage(DropMessageParser.Drop drop, int expiresAtTick)
		{
			this.drop = drop;
			this.expiresAtTick = expiresAtTick;
		}
	}

	private static final class PendingReward
	{
		private final String message;
		private final Set<Integer> items;
		private int expiresAtTick;
		private int component;
		private int conversionItem;
		private Map<Integer, Long> baseline;

		private PendingReward(String message, Set<Integer> items, int expiresAtTick)
		{
			this.message = message;
			this.items = new HashSet<>(items);
			this.expiresAtTick = expiresAtTick;
		}
	}
}
