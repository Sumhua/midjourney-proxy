package com.github.novicezk.midjourney.wss.user;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.github.novicezk.midjourney.ProxyProperties;
import com.github.novicezk.midjourney.support.DiscordHelper;
import com.github.novicezk.midjourney.wss.WebSocketStarter;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import eu.bitwalker.useragentutils.UserAgent;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.api.utils.data.DataType;
import net.dv8tion.jda.internal.requests.WebSocketCode;
import net.dv8tion.jda.internal.utils.compress.Decompressor;
import net.dv8tion.jda.internal.utils.compress.ZlibDecompressor;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class UserWebSocketStarter extends WebSocketAdapter implements WebSocketStarter {
	private static final int CONNECT_RETRY_LIMIT = 3;

	private final String userToken;
	private final String userAgent;
	private final DataObject auth;

	private ScheduledExecutorService heartExecutor;
	private WebSocket socket = null;
	private String sessionId;
	private Future<?> heartbeatTask;
	private Decompressor decompressor;

	private boolean connected = false;
	private final AtomicInteger sequence = new AtomicInteger(0);

	@Resource
	private UserMessageListener userMessageListener;
	@Resource
	private DiscordHelper discordHelper;

	private final ProxyProperties properties;

	public UserWebSocketStarter(ProxyProperties properties) {
		initProxy(properties);
		this.properties = properties;
		this.userToken = properties.getDiscord().getUserToken();
		this.userAgent = properties.getDiscord().getUserAgent();
		this.auth = createAuthData();
	}

	@Override
	public synchronized void start() throws Exception {
		this.decompressor = new ZlibDecompressor(2048);
		this.heartExecutor = Executors.newSingleThreadScheduledExecutor();
		WebSocketFactory webSocketFactory = createWebSocketFactory(this.properties);
		this.socket = webSocketFactory.createSocket(this.discordHelper.getWss() + "/?encoding=json&v=9&compress=zlib-stream");
		this.socket.addListener(this);
		this.socket.addHeader("Accept-Encoding", "gzip, deflate, br").addHeader("Accept-Language", "en-US,en;q=0.9")
				.addHeader("Cache-Control", "no-cache").addHeader("Pragma", "no-cache")
				.addHeader("Sec-WebSocket-Extensions", "permessage-deflate; client_max_window_bits")
				.addHeader("User-Agent", this.userAgent);
		this.socket.connect();
	}

	@Override
	public void onConnected(WebSocket websocket, Map<String, List<String>> headers) {
		log.debug("[gateway] Connected to websocket.");
		this.connected = true;
	}

	@Override
	public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {
		byte[] decompressBinary = this.decompressor.decompress(binary);
		if (decompressBinary == null) {
			return;
		}
		String json = new String(decompressBinary, StandardCharsets.UTF_8);
		System.out.println("json is " + json);
		//DataObject data = DataObject.fromJson("{\"t\":\"MESSAGE_CREATE\",\"s\":11,\"op\":0,\"d\":{\"type\":0,\"tts\":false,\"timestamp\":\"2023-08-30T03:48:05.178000+00:00\",\"referenced_message\":null,\"pinned\":false,\"mentions\":[{\"username\":\"janeward\",\"public_flags\":0,\"member\":{\"roles\":[\"1105855689940803666\",\"1100104105415946340\",\"1072457413879402567\"],\"premium_since\":null,\"pending\":false,\"nick\":null,\"mute\":false,\"joined_at\":\"2023-08-28T09:24:46.083000+00:00\",\"flags\":42,\"deaf\":false,\"communication_disabled_until\":null,\"avatar\":null},\"id\":\"1091519978018185326\",\"global_name\":\"Skyline\",\"discriminator\":\"0\",\"avatar_decoration_data\":null,\"avatar\":null}],\"mention_roles\":[],\"mention_everyone\":false,\"member\":{\"roles\":[\"1053013891631824976\",\"1049418339195310214\"],\"premium_since\":null,\"pending\":false,\"nick\":null,\"mute\":false,\"joined_at\":\"2022-12-05T20:13:49.385000+00:00\",\"flags\":0,\"deaf\":false,\"communication_disabled_until\":\"2023-02-10T18:20:20.157000+00:00\",\"avatar\":null},\"id\":\"1146290218689896478\",\"flags\":0,\"embeds\":[],\"edited_timestamp\":null,\"content\":\"**city tower under the sunset** - <@1091519978018185326>\",\"components\":[{\"type\":1,\"components\":[{\"type\":2,\"style\":2,\"label\":\"U1\",\"custom_id\":\"UPSCALE:2925bb00-5671-43e2-8e40-2a1c5c904eee.jpg\"},{\"type\":2,\"style\":2,\"label\":\"U2\",\"custom_id\":\"UPSCALE:a491c110-23f1-456f-b55e-39e5a886d9d7.jpg\"},{\"type\":2,\"style\":2,\"label\":\"U3\",\"custom_id\":\"UPSCALE:669cc446-32af-45fd-a02c-cd3245ed916c.jpg\"},{\"type\":2,\"style\":2,\"label\":\"U4\",\"custom_id\":\"UPSCALE:19d0abe2-9151-4597-b7b9-108a7f788b28.jpg\"},{\"type\":2,\"style\":2,\"label\":\"ⓧ\",\"custom_id\":\"delete\"}]},{\"type\":1,\"components\":[{\"type\":2,\"style\":2,\"label\":\"V1\",\"custom_id\":\"VARIATION:2925bb00-5671-43e2-8e40-2a1c5c904eee.jpg\"},{\"type\":2,\"style\":2,\"label\":\"V2\",\"custom_id\":\"VARIATION:a491c110-23f1-456f-b55e-39e5a886d9d7.jpg\"},{\"type\":2,\"style\":2,\"label\":\"V3\",\"custom_id\":\"VARIATION:669cc446-32af-45fd-a02c-cd3245ed916c.jpg\"},{\"type\":2,\"style\":2,\"label\":\"V4\",\"custom_id\":\"VARIATION:19d0abe2-9151-4597-b7b9-108a7f788b28.jpg\"},{\"type\":2,\"style\":2,\"label\":\"\u180E\u180E\u180E\u180E\u180E\uD83D\uDD04\",\"custom_id\":\"redo\"}]}],\"channel_id\":\"1063523239718035607\",\"author\":{\"username\":\"BlueWillow\",\"public_flags\":65536,\"id\":\"1049413890276077690\",\"global_name\":null,\"discriminator\":\"6557\",\"bot\":true,\"avatar_decoration_data\":null,\"avatar\":\"d41be1723ccc7d6d0541119490767e23\"},\"attachments\":[{\"width\":1024,\"url\":\"https://cdn.discordapp.com/attachments/1063523239718035607/1146290217851039774/182f4c80-e182-454f-8613-4effc3c10186.jpg\",\"size\":145104,\"proxy_url\":\"https://media.discordapp.net/attachments/1063523239718035607/1146290217851039774/182f4c80-e182-454f-8613-4effc3c10186.jpg\",\"id\":\"1146290217851039774\",\"height\":1024,\"filename\":\"182f4c80-e182-454f-8613-4effc3c10186.jpg\",\"content_type\":\"image/jpeg\"}],\"guild_id\":\"1046979304547954728\"}}");
		DataObject data = DataObject.fromJson(json);
		int opCode = data.getInt("op");
		if (opCode != WebSocketCode.HEARTBEAT_ACK) {
			this.sequence.incrementAndGet();
		}
		if (opCode == WebSocketCode.HELLO) {
			if (this.heartbeatTask == null && this.heartExecutor != null) {
				long interval = data.getObject("d").getLong("heartbeat_interval");
				this.heartbeatTask =
						this.heartExecutor.scheduleAtFixedRate(this::heartbeat, interval, interval, TimeUnit.MILLISECONDS);
			}
			sayHello();
		} else if (opCode == WebSocketCode.HEARTBEAT_ACK) {
			log.trace("[gateway] Heartbeat ack.");
		} else if (opCode == WebSocketCode.HEARTBEAT) {
			send(DataObject.empty().put("op", WebSocketCode.HEARTBEAT).put("d", this.sequence));
		} else if (opCode == WebSocketCode.INVALIDATE_SESSION) {
			log.debug("[gateway] Invalid session.");
			close("session invalid");
		} else if (opCode == WebSocketCode.RECONNECT) {
			log.debug("[gateway] Received opcode 7 (reconnect).");
			close("reconnect");
		} else if (opCode == WebSocketCode.DISPATCH) {
			onDispatch(data);
		}
	}

	@Override
	public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame,
			boolean closedByServer) {
		reset();
		int code = 1000;
		String closeReason = "";
		if (clientCloseFrame != null) {
			code = clientCloseFrame.getCloseCode();
			closeReason = clientCloseFrame.getCloseReason();
		} else if (serverCloseFrame != null) {
			code = serverCloseFrame.getCloseCode();
			closeReason = serverCloseFrame.getCloseReason();
		}
		if (code >= 4010 || code == 4004) {
			log.warn("[gateway] Websocket closed and can't reconnect! code: {}, reason: {}", code, closeReason);
			System.exit(code);
			return;
		}
		log.warn("[gateway] Websocket closed and will be reconnect... code: {}, reason: {}", code, closeReason);
		ThreadUtil.execute(() -> {
			try {
				retryStart(0);
			} catch (Exception e) {
				log.error("[gateway] Websocket reconnect error", e);
				System.exit(1);
			}
		});
	}

	private void retryStart(int currentRetryTime) throws Exception {
		try {
			start();
		} catch (Exception e) {
			if (currentRetryTime < CONNECT_RETRY_LIMIT) {
				currentRetryTime++;
				log.warn("[gateway] Websocket start fail, retry {} time... error: {}", currentRetryTime,
						e.getMessage());
				Thread.sleep(5000L);
				retryStart(currentRetryTime);
			} else {
				throw e;
			}
		}
	}

	@Override
	public void handleCallbackError(WebSocket websocket, Throwable cause) throws Exception {
		log.error("[gateway] There was some websocket error", cause);
	}

	private void sayHello() {
		DataObject data;
		if (CharSequenceUtil.isBlank(this.sessionId)) {
			data = DataObject.empty().put("op", WebSocketCode.IDENTIFY).put("d", this.auth);
			log.trace("[gateway] Say hello: identify");
		} else {
			data = DataObject.empty().put("op", WebSocketCode.RESUME).put("d",
					DataObject.empty().put("token", this.userToken).put("session_id", this.sessionId).put("seq",
							Math.max(this.sequence.get() - 1, 0)));
			log.trace("[gateway] Say hello: resume");
		}
		send(data);
	}

	private void close(String reason) {
		this.connected = false;
		this.socket.disconnect(1000, reason);
	}

	private void reset() {
		this.connected = false;
		this.sessionId = null;
		this.sequence.set(0);
		this.decompressor = null;
		this.socket = null;
		if (this.heartbeatTask != null) {
			this.heartbeatTask.cancel(true);
			this.heartbeatTask = null;
		}
	}

	private void heartbeat() {
		if (!this.connected) {
			return;
		}
		send(DataObject.empty().put("op", WebSocketCode.HEARTBEAT).put("d", this.sequence));
	}

	private void onDispatch(DataObject raw) {
		if (!raw.isType("d", DataType.OBJECT)) {
			return;
		}
		DataObject content = raw.getObject("d");
		String t = raw.getString("t", null);
		if ("READY".equals(t)) {
			this.sessionId = content.getString("session_id");
			return;
		}
		try {
			this.userMessageListener.onMessage(raw);
		} catch (Exception e) {
			log.error("user-wss handle message error", e);
		}
	}

	protected void send(DataObject message) {
		log.trace("[gateway] > {}", message);
		this.socket.sendText(message.toString());
	}

	private DataObject createAuthData() {
		UserAgent agent = UserAgent.parseUserAgentString(this.userAgent);
		DataObject connectionProperties = DataObject.empty().put("os", agent.getOperatingSystem().getName())
				.put("browser", agent.getBrowser().getGroup().getName()).put("device", "").put("system_locale", "zh-CN")
				.put("browser_version", agent.getBrowserVersion().toString()).put("browser_user_agent", this.userAgent)
				.put("referer", "").put("referring_domain", "").put("referrer_current", "")
				.put("referring_domain_current", "").put("release_channel", "stable").put("client_build_number", 117300)
				.put("client_event_source", null);
		DataObject presence = DataObject.empty().put("status", "online").put("since", 0)
				.put("activities", DataArray.empty()).put("afk", false);
		DataObject clientState = DataObject.empty().put("guild_hashes", DataArray.empty()).put("highest_last_message_id", "0")
				.put("read_state_version", 0).put("user_guild_settings_version", -1).put("user_settings_version", -1);
		return DataObject.empty().put("token", this.userToken).put("capabilities", 4093)
				.put("properties", connectionProperties).put("presence", presence).put("compress", false)
				.put("client_state", clientState);
	}

}
