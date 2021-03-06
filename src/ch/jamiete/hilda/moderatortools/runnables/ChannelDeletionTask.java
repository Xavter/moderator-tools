/*******************************************************************************
 * Copyright 2017 jamietech
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package ch.jamiete.hilda.moderatortools.runnables;

import java.util.List;
import java.util.stream.Collectors;
import ch.jamiete.hilda.Hilda;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.MessageBuilder.Formatting;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;

public class ChannelDeletionTask implements Runnable {
    private final TextChannel channel;
    private final boolean automatic;

    public ChannelDeletionTask(final TextChannel channel) {
        this(channel, false);
    }

    public ChannelDeletionTask(final TextChannel channel, final boolean automatic) {
        this.channel = channel;
        this.automatic = automatic;
    }

    @Override
    public void run() {
        Hilda.getLogger().info("Automatically clearing " + this.channel.getName() + " " + this.channel.getId());

        this.channel.sendMessage("Channel being automatically cleared...").queue();
        this.channel.sendTyping().queue();

        final MessageHistory history = this.channel.getHistory();
        Hilda.getLogger().fine("Getting first history...");

        while (true) {
            Hilda.getLogger().fine("Getting 100 messages");
            final List<Message> messages = history.retrievePast(100).complete().stream().filter(message -> !message.isPinned()).collect(Collectors.toList());

            if (messages.isEmpty()) {
                break;
            }

            try {
                this.channel.deleteMessages(messages).complete();
            } catch (final ErrorResponseException e) {
                this.channel.sendMessage("I couldn't clear the messages in this channel as they're more than two weeks old. Please re-create the channel to clear the messages.").queue();
                Hilda.getLogger().info("Given up clearing because channel messages are too old.");
                break;
            }

            if (messages.size() < 100) {
                break;
            }
        }

        Hilda.getLogger().fine("Finished getting and deleting histories.");

        final MessageBuilder mb = new MessageBuilder();

        mb.append("This channel's messages were cleared.", Formatting.BOLD);

        if (this.automatic) {
            mb.append("\n\n");
            mb.append("I automatically cleared messages in this channel at 00:00 UTC every day.", Formatting.ITALICS);
        }

        this.channel.sendMessage(mb.build()).queue();

        Hilda.getLogger().info("Finished clearing " + this.channel.getName() + " " + this.channel.getId());
    }

}
