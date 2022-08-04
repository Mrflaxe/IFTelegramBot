package ru.mrflaxe.textadventure.tool;

import lombok.Getter;

public class Cooldown {

    private final long delay;
    
    @Getter
    private boolean cooldown;
    
    public Cooldown(long delay) {
        this.delay = delay * 1000;
        this.cooldown = false;
    }
    
    public void setCooldown() {
        runCooldown();
    }
    
    private void runCooldown() {
        cooldown = true;
        
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                cooldown = false;
            } catch (InterruptedException ignored) {}
        }).start();
    }
}
