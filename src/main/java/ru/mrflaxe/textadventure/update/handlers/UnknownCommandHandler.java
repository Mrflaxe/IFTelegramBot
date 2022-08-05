package ru.mrflaxe.textadventure.update.handlers;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;

import ru.mrflaxe.textadventure.configuration.Configuration;
import ru.mrflaxe.textadventure.update.UpdateProvider;

public class UnknownCommandHandler extends MessageHandler {

    public UnknownCommandHandler(TelegramBot bot, Configuration messages, UpdateProvider updateProvider) {
        super(bot, messages, updateProvider);
        
    }

    @Override
    public void handle(Update update) {
        String message = messages.getString("error.unknown-command", true);
        long chatID = update.message().chat().id();
        
        SendMessage request = new SendMessage(chatID, message);
        request.parseMode(ParseMode.HTML);
        
        bot.execute(request);
    }
}
