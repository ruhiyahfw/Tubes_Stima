package za.co.entelect.challenge.entities;

import com.google.gson.annotations.SerializedName;

public class MyWorm extends Worm {
    @SerializedName("weapon")
    public Weapon weapon;

    @SerializedName("bananas")
    public bananaBombs bananaBombs;

    @SerializedName("snowballs")
    public snowballs snowballs;
}
