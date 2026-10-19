package com.clanclogclient;

import com.sun.net.httpserver.HttpServer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.Buffer;
import org.junit.Test;
import static org.junit.Assert.*;

public class SubmissionClientTest
{
	private final Submission submission = new Submission(UUID.randomUUID(), "Collector", "Test Clan", 4151, "Abyssal whip",
		"[\"MEMBERS\",\"HARDCORE_MAIN\"]", " Valuable drop: <col=ff0000>Abyssal\u00a0whip</col> (1,000,000 coins) ", true, false);
	private final Submission broadcast = new Submission(UUID.randomUUID(), "Nerd Stark", "Test Clan", 22486, "Scythe of Vitur (uncharged)",
		"[\"MEMBERS\"]", "Nerd\u00a0Stark received special loot from a raid: Scythe of Vitur (uncharged) (1,188,452,005 coins).", false, false);
	private final Submission silentLoot = new Submission(UUID.randomUUID(), "Collector", "Test Clan", 4151, "Abyssal whip",
		"[\"MEMBERS\"]", "RuneLite Loot Tracker event: Collector obtained 1 x Abyssal whip (item ID: 4151), source: Abyssal demon, type: NPC.", true, true);

	@Test
	public void requestMatchesApiContractAndPreservesExactUnicodeKey() throws Exception
	{
		String apiKey = " \u00e6\u2764\nkey ";
		byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47};
		Request request = SubmissionClient.buildRequest(submission, png,
			SubmissionClient.endpoint("https://clan.example/custom/submissions"), apiKey);
		assertEquals("POST", request.method());
		assertEquals("https://clan.example/custom/submissions", request.url().toString());
		assertEquals(submission.getIdempotencyKey().toString(), request.header("Idempotency-Key"));
		assertNull(request.header("X-Clan-API-Key"));
		assertEquals(apiKey, new String(Base64.getUrlDecoder().decode(request.header("X-Clan-API-Key-Base64")), StandardCharsets.UTF_8));
		Buffer buffer = new Buffer();
		request.body().writeTo(buffer);
		String body = buffer.readString(StandardCharsets.ISO_8859_1);
		for (String field : new String[]{"username", "clanName", "itemId", "worldType", "chatMessage", "itemName", "screenshot"})
		{
			assertTrue(body, body.contains("name=\"" + field + "\""));
		}
		assertEquals(7, body.split("Content-Disposition:").length - 1);
		assertTrue(body.contains("Collector"));
		assertTrue(body.contains("Test Clan"));
		assertTrue(body.contains("4151"));
		assertTrue(body.contains(submission.getWorldType()));
		assertTrue(body.contains("Content-Type: image/png"));
		Buffer chatBytes = new Buffer();
		((okhttp3.MultipartBody) request.body()).parts().get(4).body().writeTo(chatBytes);
		assertEquals(submission.getChatMessage(), chatBytes.readUtf8());
		Buffer nameBytes = new Buffer();
		((okhttp3.MultipartBody) request.body()).parts().get(5).body().writeTo(nameBytes);
		assertEquals(submission.getItemName(), nameBytes.readUtf8());
	}

	@Test
	public void broadcastRequestContainsExactChatAndNeverAttachesPng() throws Exception
	{
		// Even an accidentally supplied image must not be attached to a broadcast.
		Request request = SubmissionClient.buildRequest(broadcast, new byte[]{1, 2, 3},
			SubmissionClient.endpoint("https://clan.example/submit"), "");
		assertNull(request.header("X-Clan-API-Key-Base64"));
		Buffer buffer = new Buffer();
		request.body().writeTo(buffer);
		String body = buffer.readUtf8();
		assertEquals(6, body.split("Content-Disposition:").length - 1);
		assertTrue(body.contains(broadcast.getItemName()));
		assertTrue(body.contains(broadcast.getChatMessage()));
		assertTrue(body.contains("name=\"chatMessage\""));
		assertFalse(body.contains("name=\"screenshot\""));
		assertFalse(body.contains("image/png"));
	}

	@Test
	public void validatesSubmittedItemNameAndPreservesUnicode() throws Exception
	{
		for (String invalid : new String[]{null, "", " \t", "drop\0name", "x".repeat(201)})
		{
			assertThrows(IllegalArgumentException.class, () -> SubmissionClient.validateItemName(invalid));
		}
		SubmissionClient.validateItemName("x".repeat(200));
		Submission drop = new Submission(UUID.randomUUID(), "Collector", "Test Clan", 4151,
			" Original \u00e9 item ", "MEMBERS", "Unrecognized chat proof", false, false);
		Request request = SubmissionClient.buildRequest(drop, null, SubmissionClient.endpoint("https://clan.example/submit"), "");
		Buffer bytes = new Buffer();
		((okhttp3.MultipartBody) request.body()).parts().get(5).body().writeTo(bytes);
		assertEquals(drop.getItemName(), bytes.readUtf8());
	}

	@Test
	public void acceptsUnmatchedPendingReceiptWithoutRetrying() throws Exception
	{
		AtomicInteger attempts = new AtomicInteger();
		CompletableFuture<String> notification = new CompletableFuture<>();
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/submit", exchange ->
		{
			attempts.incrementAndGet();
			exchange.getRequestBody().readAllBytes();
			byte[] receipt = "{\"status\":\"pending\",\"itemId\":null,\"gameItemId\":22486}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(201, receipt.length);
			exchange.getResponseBody().write(receipt);
			exchange.close();
		});
		server.start();
		SubmissionClient transport = new SubmissionClient(new OkHttpClient());
		try
		{
			long session = transport.start(notification::complete);
			assertTrue(transport.submit(broadcast, null, HttpUrl.parse("http://127.0.0.1:" + server.getAddress().getPort() + "/submit"), "", session));
			assertEquals(broadcast.getItemName() + " submission for " + broadcast.getUsername() + " accepted by API.", notification.get(10, TimeUnit.SECONDS));
			assertEquals(1, attempts.get());
		}
		finally
		{
			transport.stop();
			server.stop(0);
		}
	}

	@Test
	public void validatesFullChatProofWithoutTruncatingIt()
	{
		for (String invalid : new String[]{null, "", " \n\t", "drop\0message", "\u00e9".repeat(4097)})
		{
			assertThrows(IllegalArgumentException.class, () -> SubmissionClient.validateChatMessage(invalid));
		}
		SubmissionClient.validateChatMessage("\u00e9".repeat(4096));
		assertThrows(IllegalArgumentException.class, () -> SubmissionClient.buildRequest(submission, null,
			SubmissionClient.endpoint("https://clan.example/submit"), ""));
	}

	@Test
	public void validatesEndpointAndAllowsLocalDevelopment()
	{
		assertEquals("http", SubmissionClient.endpoint("http://localhost:3000/api/v1/runelite/submissions").scheme());
		assertEquals("https", SubmissionClient.endpoint(" https://clan.example/api ").scheme());
		for (String endpoint : new String[]{"", "not a url", "http://clan.example/api", "https://user:secret@clan.example/api", "https://clan.example/#fragment"})
		{
			assertThrows(endpoint, IllegalArgumentException.class, () -> SubmissionClient.endpoint(endpoint));
		}
	}

	@Test
	public void retryScheduleHonorsServerBackoff()
	{
		assertEquals(60, SubmissionClient.retryDelaySeconds("60", 1));
		assertEquals(1, SubmissionClient.retryDelaySeconds("0", 1));
		assertEquals(2, SubmissionClient.retryDelaySeconds(null, 1));
		assertEquals(4, SubmissionClient.retryDelaySeconds("invalid", 2));
	}

	@Test
	public void rejectsStaleFramesFromPreviousConfigurationSession()
	{
		SubmissionClient transport = new SubmissionClient(new OkHttpClient());
		long oldSession = transport.start(message -> fail("Unexpected upload notification"));
		long newSession = transport.start(message -> fail("Unexpected upload notification"));
		assertNotEquals(oldSession, newSession);
		assertFalse(transport.submit(submission, null, null, "", oldSession));
		transport.stop();
		assertFalse(transport.submit(submission, null, null, "", newSession));
	}

	@Test
	public void retriesIdenticalMultipartAndKeyAfterRateLimit() throws Exception
	{
		assertRetry(silentLoot);
	}

	@Test
	public void retriesChatOnlyBroadcastWithoutEverAddingScreenshot() throws Exception
	{
		assertRetry(broadcast);
	}

	private void assertRetry(Submission drop) throws Exception
	{
		AtomicInteger attempts = new AtomicInteger();
		List<byte[]> bodies = new ArrayList<>();
		List<String> keys = new ArrayList<>();
		CompletableFuture<String> notification = new CompletableFuture<>();
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/submit", exchange ->
		{
			bodies.add(exchange.getRequestBody().readAllBytes());
			keys.add(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
			int code = attempts.incrementAndGet() == 1 ? 429 : 201;
			exchange.getResponseHeaders().add("Retry-After", "1");
			exchange.sendResponseHeaders(code, -1);
			exchange.close();
		});
		server.start();
		SubmissionClient transport = new SubmissionClient(new OkHttpClient());
		try
		{
			long session = transport.start(notification::complete);
			assertTrue(transport.submit(drop, drop.isPersonal() ? new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB) : null,
				HttpUrl.parse("http://127.0.0.1:" + server.getAddress().getPort() + "/submit"), "test-key", session));
			assertEquals(drop.getItemName() + " submission for " + drop.getUsername() + " accepted by API.",
				notification.get(10, TimeUnit.SECONDS));
			assertEquals(2, attempts.get());
			assertEquals(keys.get(0), keys.get(1));
			assertArrayEquals(bodies.get(0), bodies.get(1));
			assertTrue(new String(bodies.get(0), StandardCharsets.UTF_8).contains(drop.getChatMessage()));
			String raw = new String(bodies.get(0), StandardCharsets.ISO_8859_1);
			int pngStart = raw.indexOf("\u0089PNG\r\n\u001a\n");
			if (drop.isPersonal())
			{
				assertTrue("Uploaded file must be PNG", pngStart > 0);
				BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(bodies.get(0), pngStart, bodies.get(0).length - pngStart));
				assertEquals(2, decoded.getWidth());
				assertEquals(2, decoded.getHeight());
			}
			else
			{
				assertEquals(-1, pngStart);
				assertFalse(raw.contains("name=\"screenshot\""));
			}
		}
		finally
		{
			transport.stop();
			server.stop(0);
		}
	}

	@Test
	public void doesNotFollowRedirectOrRetryAuthenticationFailure() throws Exception
	{
		for (int status : new int[]{302, 401, 403, 422})
		{
			AtomicInteger attempts = new AtomicInteger();
			CompletableFuture<String> notification = new CompletableFuture<>();
			HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
			server.createContext("/", exchange ->
			{
				attempts.incrementAndGet();
				exchange.getRequestBody().readAllBytes();
				exchange.getResponseHeaders().add("Location", "/other");
				exchange.sendResponseHeaders(status, -1);
				exchange.close();
			});
			server.start();
			SubmissionClient transport = new SubmissionClient(new OkHttpClient());
			try
			{
				long session = transport.start(notification::complete);
				transport.submit(submission, new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB),
					HttpUrl.parse("http://127.0.0.1:" + server.getAddress().getPort() + "/"), "test-key", session);
				assertTrue(notification.get(10, TimeUnit.SECONDS).contains(Integer.toString(status)));
				assertEquals(1, attempts.get());
			}
			finally
			{
				transport.stop();
				server.stop(0);
			}
		}
	}
}
