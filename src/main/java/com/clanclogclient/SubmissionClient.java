package com.clanclogclient;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
class SubmissionClient
{
	static final int MAX_PENDING = 8;
	static final int MAX_ATTEMPTS = 5;
	static final int MAX_PNG_BYTES = 10 * 1024 * 1024;
	private static final MediaType PNG = MediaType.parse("image/png");
	private final OkHttpClient httpClient;
	private final Map<UUID, Upload> pending = new HashMap<>();
	private ScheduledExecutorService worker;
	private Consumer<String> notifications;
	private long session;

	@Inject
	SubmissionClient(OkHttpClient httpClient)
	{
		this.httpClient = httpClient.newBuilder().followRedirects(false).followSslRedirects(false)
			.callTimeout(30, TimeUnit.SECONDS).build();
	}

	synchronized long start(Consumer<String> notifications)
	{
		stop();
		this.notifications = notifications;
		worker = Executors.newSingleThreadScheduledExecutor(task ->
		{
			Thread thread = new Thread(task, "clan-clog-submissions");
			thread.setDaemon(true);
			return thread;
		});
		return session;
	}

	synchronized void stop()
	{
		session++;
		for (Upload upload : pending.values())
		{
			if (upload.call != null)
			{
				upload.call.cancel();
			}
			if (upload.retry != null)
			{
				upload.retry.cancel(false);
			}
		}
		pending.clear();
		if (worker != null)
		{
			worker.shutdownNow();
			worker = null;
		}
	}

	synchronized boolean submit(Submission submission, BufferedImage screenshot, HttpUrl endpoint, String apiKey, long expectedSession)
	{
		if (worker == null || session != expectedSession)
		{
			return false;
		}
		if (pending.size() >= MAX_PENDING)
		{
			notifications.accept("Upload queue is full; a drop could not be submitted.");
			return false;
		}
		Upload upload = new Upload(submission);
		pending.put(submission.getIdempotencyKey(), upload);
		worker.execute(() -> prepare(upload, screenshot, endpoint, apiKey));
		return true;
	}

	private void prepare(Upload upload, BufferedImage screenshot, HttpUrl endpoint, String apiKey)
	{
		try
		{
			validateChatMessage(upload.submission.getChatMessage());
			byte[] png = null;
			if (upload.submission.isPersonal())
			{
				if (screenshot == null)
				{
					throw new IOException("Missing personal-drop screenshot");
				}
				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				try (MemoryCacheImageOutputStream output = new MemoryCacheImageOutputStream(bytes))
				{
					if (!ImageIO.write(screenshot, "png", output))
					{
						throw new IOException("PNG encoder unavailable");
					}
				}
				if (bytes.size() > MAX_PNG_BYTES)
				{
					finish(upload, "Screenshot exceeds the API's 10 MiB limit.");
					return;
				}
				png = bytes.toByteArray();
			}
			Request request = buildRequest(upload.submission, png, endpoint, apiKey);
			synchronized (this)
			{
				if (!isPending(upload))
				{
					return;
				}
				upload.request = request;
				send(upload);
			}
		}
		catch (IOException | RuntimeException ex)
		{
			log.debug("Unable to prepare submission evidence", ex);
			finish(upload, "Could not prepare the drop message or screenshot; please submit manually.");
		}
	}

	private synchronized void send(Upload upload)
	{
		if (!isPending(upload))
		{
			return;
		}
		upload.attempts++;
		upload.call = httpClient.newCall(upload.request);
		upload.call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException ex)
			{
				log.debug("Clan Clog upload failed ({})", ex.getClass().getSimpleName());
				retry(upload, null);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (Response closed = response)
				{
					int code = closed.code();
					if (code == 201)
					{
						finish(upload, upload.submission.getItemName() + " submission for "
							+ upload.submission.getUsername() + " accepted by API.");
					}
					else if (code == 408 || code == 429 || code >= 500)
					{
						retry(upload, closed.header("Retry-After"));
					}
					else
					{
						finish(upload, failureMessage(code));
					}
				}
			}
		});
	}

	private synchronized void retry(Upload upload, String retryAfter)
	{
		if (!isPending(upload))
		{
			return;
		}
		if (upload.attempts >= MAX_ATTEMPTS)
		{
			finish(upload, "Drop upload failed after retries; please submit it manually.");
			return;
		}
		long delay = retryDelaySeconds(retryAfter, upload.attempts);
		upload.retry = worker.schedule(() -> send(upload), delay, TimeUnit.SECONDS);
	}

	private synchronized void finish(Upload upload, String message)
	{
		if (isPending(upload))
		{
			pending.remove(upload.submission.getIdempotencyKey());
			notifications.accept(message);
		}
	}

	private boolean isPending(Upload upload)
	{
		return worker != null && pending.get(upload.submission.getIdempotencyKey()) == upload;
	}

	static HttpUrl endpoint(String value)
	{
		HttpUrl url = HttpUrl.parse(value.trim());
		if (url == null || !url.username().isEmpty() || !url.password().isEmpty() || url.fragment() != null)
		{
			throw new IllegalArgumentException("Enter a full submission endpoint URL without credentials or a fragment.");
		}
		boolean local = url.host().equals("localhost") || url.host().equals("127.0.0.1") || url.host().equals("::1");
		if (!url.isHttps() && !local)
		{
			throw new IllegalArgumentException("The submission endpoint must use HTTPS (HTTP is allowed on localhost).");
		}
		return url;
	}

	static Request buildRequest(Submission submission, byte[] png, HttpUrl endpoint, String apiKey)
	{
		validateChatMessage(submission.getChatMessage());
		MultipartBody.Builder body = new MultipartBody.Builder().setType(MultipartBody.FORM)
			.addFormDataPart("username", submission.getUsername())
			.addFormDataPart("clanName", submission.getClanName())
			.addFormDataPart("itemId", Integer.toString(submission.getItemId()))
			.addFormDataPart("worldType", submission.getWorldType())
			.addFormDataPart("chatMessage", submission.getChatMessage());
		if (submission.isPersonal())
		{
			if (png == null)
			{
				throw new IllegalArgumentException("Personal drops require a screenshot");
			}
			body.addFormDataPart("screenshot", "drop.png", RequestBody.create(PNG, png));
		}
		Request.Builder request = new Request.Builder().url(endpoint).post(body.build())
			.header("Idempotency-Key", submission.getIdempotencyKey().toString());
		if (!apiKey.isEmpty())
		{
			request.header("X-Clan-API-Key-Base64", Base64.getUrlEncoder().withoutPadding()
				.encodeToString(apiKey.getBytes(StandardCharsets.UTF_8)));
		}
		return request.build();
	}

	static void validateChatMessage(String message)
	{
		if (message == null || message.trim().isEmpty() || message.indexOf('\0') >= 0
			|| message.getBytes(StandardCharsets.UTF_8).length > 8192)
		{
			throw new IllegalArgumentException("The triggering message must be nonempty, contain no NUL characters and fit in 8192 UTF-8 bytes.");
		}
	}

	static long retryDelaySeconds(String retryAfter, int attempts)
	{
		long fallback = Math.min(60, 1L << attempts);
		if (retryAfter != null)
		{
			try
			{
				return Math.max(1, Long.parseLong(retryAfter));
			}
			catch (NumberFormatException ex)
			{
				try
				{
					return Math.max(1, Duration.between(ZonedDateTime.now(),
						ZonedDateTime.parse(retryAfter, DateTimeFormatter.RFC_1123_DATE_TIME)).getSeconds());
				}
				catch (DateTimeParseException ignored)
				{
					return fallback;
				}
			}
		}
		return fallback;
	}

	static String failureMessage(int code)
	{
		switch (code)
		{
			case 401: return "API rejected the clan key (401); check Clan API key.";
			case 403: return "API rejected the drop (403); check clan, world and submission policies.";
			case 409: return "API reported an idempotency conflict (409); please submit manually.";
			case 413: return "API rejected the screenshot size (413).";
			case 422: return "API rejected the item or chat message (422); check the clan catalog and message evidence.";
			default: return "API rejected the upload (HTTP " + code + "); check the endpoint and server settings.";
		}
	}

	private static final class Upload
	{
		private final Submission submission;
		private Request request;
		private Call call;
		private ScheduledFuture<?> retry;
		private int attempts;

		private Upload(Submission submission)
		{
			this.submission = submission;
		}
	}
}
