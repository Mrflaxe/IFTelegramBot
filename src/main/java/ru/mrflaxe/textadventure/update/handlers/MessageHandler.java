package ru.mrflaxe.textadventure.update.handlers;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;

import ru.mrflaxe.textadventure.configuration.Configuration;
import ru.mrflaxe.textadventure.update.UpdateProvider;

public abstract class MessageHandler {
    
    protected final TelegramBot bot;
    protected final Configuration messages;
    protected final UpdateProvider updateProvider;
    
    public MessageHandler(TelegramBot bot, Configuration messages, UpdateProvider updateProvider) {
        this.bot = bot;
        this.messages = messages;
        this.updateProvider = updateProvider;
    }
    
    public abstract void handle(Update update);
}
