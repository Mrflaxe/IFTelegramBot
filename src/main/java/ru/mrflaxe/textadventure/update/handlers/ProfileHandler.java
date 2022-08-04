package ru.mrflaxe.textadventure.update.handlers;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;

import ru.mrflaxe.textadventure.configuration.Configuration;
import ru.mrflaxe.textadventure.update.UpdateProvider;

public class ProfileHandler extends MessageHandler {

    public ProfileHandler(TelegramBot bot, Configuration messages, UpdateProvider updateProvider) {
        super(bot, messages, updateProvider);
    }

    @Override
    public void handle(Update update) {
        Long chatId = update.message().chat().id();
        String message = messages.getString("error.button.not-ready");
        
        SendMessage request = new SendMessage(chatId, message);
        ParseMode parseMode = ParseMode.HTML;
        request.parseMode(parseMode);
        bot.execute(request);
    }
}
