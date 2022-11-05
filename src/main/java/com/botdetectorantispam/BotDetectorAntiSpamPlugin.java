package com.botdetectorantispam;

import com.botdetectorantispam.enums.Type;
import com.botdetectorantispam.model.NaiveBayes;
import com.botdetectorantispam.model.Token;
import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.plaf.synth.SynthToolTipUI;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.menus.WidgetMenuOption;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

import java.awt.*;
import java.util.Map;

@Slf4j
@PluginDescriptor(
	name = "Bot Detector Anti-spam"
)
public class BotDetectorAntiSpamPlugin extends Plugin
{
	// default injections
	@Inject
	private Client client;

	@Inject
	private BotDetectorAntiSpamConfig config;

	// custom injections
	@Inject
	private NaiveBayes naiveBayes;
	@Inject
	private MenuManager menuManager;

	// custom variables
	private static final String MARK_HAM_OPTION = "mark ham";
	private static final String MARK_SPAM_OPTION = "mark spam";

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

	// TODO: (on menu option click) mark ham naiveBayes.markMessage(message, Type.HAM);
	// TODO: (on menu option click) mark spam naiveBayes.markMessage(message, Type.SPAM);

	@Subscribe
	public void onMenuOpened(MenuOpened event) {
		// check if the user clicked on chat
		MenuEntry menuEntry = event.getFirstEntry();
		Widget widget = menuEntry.getWidget();

		// check if the user clicked chat?
		if (widget.getParentId() != WidgetInfo.CHATBOX_MESSAGE_LINES.getId()) {
			return;
		}
		// TODO: get message from widget?
		// copied from: https://github.com/jackriccomini/spamfilter-plugin-runelite/
		// As far as I can tell by skimming the builtin chat history and hiscores plugins:
		// Click doesn't happen on a chat message, it happens on the *sender* of the chat message.
		// There is a static list of senders. First static child is the most recent sender,
		// Second static child is second most recent sender, and so on.
		// Chat messages are dynamic children of CHATBOX_MESSAGES_LINES.
		int firstChatSender = WidgetInfo.CHATBOX_FIRST_MESSAGE.getChildId();
		int clickedChatSender = WidgetInfo.TO_CHILD(widget.getId());
		int clickOffset = clickedChatSender - firstChatSender;
		// can calculate the offset between "clicked-on chat message" and "most recent chat message"
		// by looking at the offset between "clicked-on chat sender" and "most recent chat sender"
		int selectedChatOffset = (clickOffset * 4) + 1;

		Widget selectedChatWidget = widget.getParent().getChild(selectedChatOffset);
		if (selectedChatWidget == null) {
			return;
		}
		String selectedChat = Text.removeTags(selectedChatWidget.getText());

		for (String entry : new String[]{MARK_HAM_OPTION, MARK_SPAM_OPTION}){
			client.createMenuEntry(1)
					.setOption(entry)
					.setType(MenuAction.RUNELITE)
					.setTarget(ColorUtil.wrapWithColorTag("message", Color.WHITE))
					.onClick(e -> {
						naiveBayes.markMessage(
								selectedChat,
								Type.valueOf(entry.replace("mark ","").toUpperCase())
						);
						// TODO: this doesnt work
						naiveBayes.tokens.forEach((key, value) -> System.out.println(key + ":" + value));
						// TODO: if marked spam than add user to ignore list
					});
		}
	}
	@Subscribe
	public void onChatMessage(ChatMessage chatMessage){
		// get message node from message event
		MessageNode msgNode = chatMessage.getMessageNode();
		String playerName = msgNode.getSender();
		String message = msgNode.getValue();

		// TODO: ignore friends & clan members
		// only look at public chat
		if (msgNode.getType() != ChatMessageType.PUBLICCHAT) {
			return;
		}

		// get prediction
		float prediction = naiveBayes.predict(message);

		// format prediction
		String strPrediction = String.format("%.2f", prediction);

		// rewrite message
		message = message + " " +"[" + strPrediction + "]";

		// print message
		// TODO: remove
		System.out.println(message);

		// display prediction to user
		msgNode.setValue(message);

		// TODO: threshold must be configurable
		double threshold = 0.75;

		if (prediction > threshold){
			// TODO: add user to ignore list
		}

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
