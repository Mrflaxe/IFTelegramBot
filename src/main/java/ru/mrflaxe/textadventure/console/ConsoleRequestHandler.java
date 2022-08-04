package ru.mrflaxe.textadventure.console;

import ru.mrflaxe.textadventure.MyBot;

public class ConsoleRequestHandler {

    private final MyBot bot;
    
    public ConsoleRequestHandler(MyBot bot) {
        this.bot = bot;
    }
    
    public void run() {
        while(true) {
            String request = System.console().readLine();
            handelRequest(request);
        }
    }
    
    private void handelRequest(String request) {
        if(request.equals("stop")) {
            bot.disable();
            return;
        }
        
        System.out.println("[ERROR]: Unknown command");
        return;
    }
}
