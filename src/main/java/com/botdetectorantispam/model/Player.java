package com.botdetectorantispam.model;
import  com.botdetectorantispam.enums.PlayerState;
import com.botdetectorantispam.model.Epoch;
import java.time.Instant;
public class Player {

    public String playerName;
    public PlayerState playerState = PlayerState.ALLOWED; // default allowed
    long stateChange = Instant.now().getEpochSecond();
    long validState = stateChange + Epoch.HOUR * 2;

    public void setStateChange(PlayerState state){
        this.playerState = state;
        this.stateChange = Instant.now().getEpochSecond();
        this.validState = stateChange + Epoch.HOUR  * 2;
    }

    public PlayerState getPlayerState(){
        long now = Instant.now().getEpochSecond();
        // if state is expired
        if (now > validState){
            System.out.println("state has expeired for" + this.playerName);
            this.setStateChange(PlayerState.ALLOWED);
        }
        return this.playerState;
    }

    public Player(String name){
        this.playerName = name;
    }
}
