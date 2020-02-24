package de.fs.fintech.geogame.data;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * Created by nicolaskepper on 15.06.17.
 */

@DatabaseTable(tableName="LeaderboardAlpha")
public class LeaderBoardAlpha {

    @DatabaseField(id=true)
    public String id;
    @DatabaseField(canBeNull = false)
    public String name;
    @DatabaseField(canBeNull = false)
    public int score;

    public LeaderBoardAlpha() {

    }

    public LeaderBoardAlpha(String name, int score) {
        this.name = name;
        this.score = score;

        String[] nameSplit = name.split(" ");

        try{
            id = /*System.currentTimeMillis() + */nameSplit[0].substring(0, 1) + nameSplit[1].substring(0, 1);
        } catch(IndexOutOfBoundsException ioobe) {
            id = nameSplit[0].substring(0, 2);
        }

    }
}
