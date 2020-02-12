package io.banditoz.gmecord;

import io.banditoz.gmecord.api.Attachment;
import io.banditoz.gmecord.api.BotMessage;
import io.banditoz.gmecord.web.MessageHandler;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroupmeMessageCreator {
    private final BotMessage message;
    private StringBuilder initialMessage;
    private final List<Attachment> initialAttachments;

    public GroupmeMessageCreator(String initialMessage, boolean isSystemMessage) {
        if (isSystemMessage) {
            this.initialMessage = new StringBuilder(initialMessage);
            this.initialMessage.insert(0, "<SYSTEM MESSAGE> ");
        }
        else {
            this.initialMessage = new StringBuilder(initialMessage);
        }
        message = new BotMessage();
        initialAttachments = new ArrayList<>();

        build();
    }

    public GroupmeMessageCreator(String initialMessage, boolean isSystemMessage, ArrayList<Attachment> attachments) {
        if (isSystemMessage) {
            this.initialMessage = new StringBuilder(initialMessage);
            this.initialMessage.insert(0, "<SYSTEM MESSAGE> ");
        }
        else {
            this.initialMessage = new StringBuilder(initialMessage);
        }
        message = new BotMessage();
        initialAttachments = attachments;

        build();
    }

    /**
     * Builds the BotMessage.
     */
    private void build() {
        sanitizeSpoilers();
        checkForMentionables();
        message.setAttachments(initialAttachments);
        message.setText(initialMessage.toString());
    }

    /**
     * Sanitize spoilers to make them blocks.
     */
    private void sanitizeSpoilers() {
        Pattern r = Pattern.compile("\\|\\|(.*?)\\|\\|");
        Matcher m = r.matcher(initialMessage);
        while (m.find()) {
            // TODO: Probably shouldn't create a new StringBuilder each time...
            initialMessage = new StringBuilder(initialMessage.toString().replace(m.group(), "■■■"));
        }
    }

    /**
     * This is the stupidest method I have ever written. Groupme web shows it shouldn't work, but the android
     * version shows it working just fine. What the fuck? It even pings me when it shouldn't (as shown by the
     * mobile version.) Maybe I <i>am</i> doing it wrong but Groupme is fixing it? Or maybe they're doing it wrong
     * and so am I? Maybe they should just do it like Discord, where it's just <@user_id>. So easy.
     */
    private void checkForMentionables() {
        boolean found = false;
        List<String> userIdList = new ArrayList<>();
        List<List<Integer>> lociList = new ArrayList<>();
        if (initialMessage.toString().contains("@GroupmeBridge")) {
            int index = initialMessage.indexOf("@GroupmeBridge"); // if someone pings the bridge, ping the last user other than the bot
            initialMessage.replace(index, index + 14, "@" + MessageHandler.lastUser); // length of @GroupmeBridge
        }
        if (initialMessage.toString().contains("@everyone")) {
            int index = initialMessage.indexOf("@everyone");
            initialMessage.replace(index - 1, index + 9, ""); // length of @everyone
            for (String s : Bot.getMentionableGroupme().keySet()) {
                initialMessage.insert(index, "@" + s + " ");
                index += s.length() + 2; // '@' and ' ' are two characters.
            }
            
        }
        for (String k : Bot.getMentionableGroupme().keySet()) {
            if (initialMessage.toString().contains("@" + k)) {
                if (!found) {
                    found = true;
                }
                int start = initialMessage.toString().indexOf(k) - 1;
                int len = k.length() + 1;
                List<Integer> intList = new ArrayList<>();
                intList.add(start);
                intList.add(len);
                userIdList.add(Bot.getMentionableGroupme().get(k));
                lociList.add(intList);
            }
        }
        if (found) {
            Attachment a = new Attachment();
            lociList.sort(Comparator.comparing(l -> l.get(0))); // TODO sort userIdList... somehow..... :(
            a.setType("mentions");
            a.setUserIds(userIdList);
            a.setLoci(lociList);
            initialAttachments.add(a);
        }
    }

    /**
     * Gets the message, assuming it's built.
     * @return The message.
     */
    public BotMessage getMessage() {
        return message;
    }
}
