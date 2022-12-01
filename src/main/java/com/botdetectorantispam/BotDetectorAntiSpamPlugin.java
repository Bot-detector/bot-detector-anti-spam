package com.botdetectorantispam;

import com.botdetectorantispam.enums.Type;
import com.botdetectorantispam.model.NaiveBayes;
import com.botdetectorantispam.model.DataPersister;

import com.google.inject.Provides;
import javax.inject.Inject;


import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;

import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import static net.runelite.api.widgets.WidgetInfo.TO_CHILD;
import static net.runelite.api.widgets.WidgetInfo.TO_GROUP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
	private  DataPersister dataPersister;

	// custom variables
	private static final String MARK_HAM_OPTION = "mark ham";
	private static final String MARK_SPAM_OPTION = "mark spam";

	// TODO: load from file
	private List<String> blackList = new ArrayList<String>();
	private List<String> ignoreList = new ArrayList<String>();
	private List<String> whiteList = new ArrayList<String>();
	private List<String> excludeList = new ArrayList<String>(Arrays.asList(" ", ",", "."));

	@Override
	protected void startUp() throws Exception
	{
		// TODO: naiveBayes.load();
		dataPersister.setup();
		naiveBayes.tokens = dataPersister.readTokens();
		naiveBayes.excludeList = excludeList;
		log.info("bot-detector-anti-spam started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		// TODO: naiveBayes.save();
		log.info("bot-detector-anti-spam stopped!");
	}
	private String[] getChatBoxMessage(MenuEntry entry){
		if (entry.getType() != MenuAction.CC_OP_LOW_PRIORITY && entry.getType() != MenuAction.RUNELITE)
		{
			return null;
		}

		final int groupId = TO_GROUP(entry.getParam1());
		final int childId = TO_CHILD(entry.getParam1());

		if (groupId != WidgetInfo.CHATBOX.getGroupId())
		{
			return null;
		}

		final Widget widget = client.getWidget(groupId, childId);
		final Widget parent = widget.getParent();

		if (WidgetInfo.CHATBOX_MESSAGE_LINES.getId() != parent.getId())
		{
			return null;
		}

		// Get child id of first chat message static child so we can substract this offset to link to dynamic child
		// later
		final int first = WidgetInfo.CHATBOX_FIRST_MESSAGE.getChildId();

		// Convert current message static widget id to dynamic widget id of message node with message contents
		// When message is right clicked, we are actually right clicking static widget that contains only sender.
		// The actual message contents are stored in dynamic widgets that follow same order as static widgets.
		// Every first dynamic widget is message sender, every second one is message contents,
		// every third one is clan name and every fourth one is clan rank icon.
		// The last two are hidden when the message is not from a clan chat or guest clan chat.
		final int dynamicChildId = (childId - first) * 4 + 1;
		final int senderId = dynamicChildId - 1;


		// Extract and store message contents when menu is opened because dynamic children can change while right click
		// menu is open and dynamicChildId will be outdated
		final Widget messageContents = parent.getChild(dynamicChildId);
		final Widget sender = parent.getChild(senderId);

		if (messageContents == null)
		{
			return null;
		}

		// get current message, remove the data between <>
		String currentMessage = messageContents
				.getText()
				.replaceAll("<.*?>", "");

		// get the sender, remove the data(timestamp) between [] and the : after the name
		String senderName = sender
				.getText()
				.replaceAll("\\[.*?\\]\\s", "")
				.replace(":","");
		return new String[] {senderName, currentMessage};
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event) {
		/*
		* add two buttons, where the user cna mark spam or ham.
		* */
		// copied from https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/chathistory/ChatHistoryPlugin.java#L184
		// TODO: check config for
		if (event.getMenuEntries().length < 2)
		{
			return;
		}

		// Use second entry as first one can be walk here with transparent chatbox
		final MenuEntry entry = event.getMenuEntries()[event.getMenuEntries().length - 2];

		String[] chatMessage = getChatBoxMessage(entry);

		if (chatMessage == null){
			return;
		}

		String senderName = chatMessage[0];
		String currentMessage = chatMessage[1];

		System.out.println(senderName+":"+currentMessage);

		for (String option : new String[]{MARK_HAM_OPTION, MARK_SPAM_OPTION}){
			client.createMenuEntry(1)
					.setOption(option)
					.setTarget(entry.getTarget())
					.setType(MenuAction.RUNELITE)
					.onClick(e ->
					{
						naiveBayes.markMessage(
								currentMessage,
								Type.valueOf(option.replace("mark ","").toUpperCase())
						);
						// TODO: find a better place to execute this, maybe on a clock?
						try {
							dataPersister.writeTokens(naiveBayes.tokens);
						} catch (IOException exception){
							exception.printStackTrace();
						}

						switch (option){
							case MARK_SPAM_OPTION:
								// add the message to the blacklist
								blackList.add(currentMessage);
								// if the sender is not on the ignore list add him to the ignorelist
								if (!ignoreList.contains(senderName)){
									ignoreList.add(senderName);
								}
								break;
							case  MARK_HAM_OPTION:
								whiteList.add(currentMessage);
								break;
						}
					});
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage){
		/*
		* Hide message if
		*  - sender is on ignore list
		*  - prediction is spam
		*  - message is already marked spam
		* */

		// get message node from message event
		MessageNode msgNode = chatMessage.getMessageNode();
		String senderName = msgNode.getName();
		String message = msgNode.getValue();

		// we need to remove a msg from the ChatLineBuffer
		final ChatLineBuffer lineBuffer = client.getChatLineMap().get(msgNode.getType().getType());

		// TODO: configurable: ignore friends & clan members
		if (msgNode.getType() != ChatMessageType.PUBLICCHAT) {
			return;
		}

		// TODO: replace println with log

		// check ignore list
		if (ignoreList.contains(senderName)){
			System.out.println("[IGNORED] " + "player:" + senderName);
			lineBuffer.removeMessageNode(msgNode);
			return;
		}

		// check blacklist
		if( blackList.contains(message)){
			System.out.println("[IGNORED] " + message);
			naiveBayes.markMessage(message, Type.SPAM);
			ignoreList.add(senderName);
			lineBuffer.removeMessageNode(msgNode);
			return;
		}

		if (whiteList.contains(message)){
			System.out.println("[WHITELIST] " + message);
			naiveBayes.markMessage(message, Type.HAM);
		}

		// get prediction
		float prediction = naiveBayes.predict(message);

		// format prediction
		String strPrediction = String.format("%.2f", prediction);

		// rewrite message
		message = message + " " +"[" + strPrediction + "]";

		// display prediction to user
		msgNode.setValue(message);

		// TODO: threshold must be configurable
		double threshold = 0.75;

		if (prediction > threshold){
			// TODO: add user to ignore list
			// TODO: add hash of msg to list of known bad msg
		}

	}
	
	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged event) {
		/*
		* */
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
