package za.co.entelect.challenge.entities;

import com.google.gson.annotations.SerializedName;

public class MyPlayer {
    @SerializedName("id")
    public int id;

    @SerializedName("score")
    public int score;

    @SerializedName("health")
    public int health;

    @SerializedName("wormSelectTokens")
    public int remainingWormSelections;

    @SerializedName("worms")
    public MyWorm[] worms;
}
