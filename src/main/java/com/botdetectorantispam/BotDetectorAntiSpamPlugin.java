package com.botdetectorantispam;

import com.botdetectorantispam.enums.Type;
import com.botdetectorantispam.model.NaiveBayes;
import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.plaf.synth.SynthToolTipUI;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Bot Detector Anti-spam"
)
public class BotDetectorAntiSpamPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private NaiveBayes naiveBayes;

	@Inject
	private BotDetectorAntiSpamConfig config;

	@Override
	protected void startUp() throws Exception
	{
		// TODO: naiveBayes.load();
		log.info("bot-detector-anti-spam started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		// TODO: naiveBayes.save();
		log.info("bot-detector-anti-spam stopped!");
	}

	// TODO: (menu option) mark ham naiveBayes.markMessage(message, Type.HAM);
	// TODO: (menu option) mark spam naiveBayes.markMessage(message, Type.SPAM);
	@Subscribe
	public void onChatMessage(ChatMessage chatMessage){
		// TODO: ignore friends & clan members
		String message = chatMessage.getMessage();
		float prediction = naiveBayes.predict(message);
		String strPrediction = String.format("%.2f", prediction);
		System.out.println(message + " " +"[" + strPrediction + "]");
		// TODO: display prediction to user
		// TODO: if prediction > 75% add user to ignore list for 2 hours
	}


	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged event) {
		// TODO: ignore friends & clan members
		/*
		if (!(event.getActor() instanceof Player)) {
			return;
		}
		float prediction = naiveBayes.predict(message);
		double threshold = 0.75;

		if (prediction > threshold){
			event.getActor().setOverheadText(" ");
		}
		*/
	}
	@Provides
	BotDetectorAntiSpamConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BotDetectorAntiSpamConfig.class);
	}
}
