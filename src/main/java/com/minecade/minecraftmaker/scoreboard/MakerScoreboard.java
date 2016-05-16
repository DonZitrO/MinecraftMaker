package com.minecade.minecraftmaker.scoreboard;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.minecade.core.data.Rank;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

/**
 * @tip properties/methods organized in descendant order according to protection level
 * @tip properties getters and setters at the end of the class
 */
public class MakerScoreboard {

    //private static final String HEALTH = "Health";
    private static final String NAME = "Name";
    private static final int SERVER_ID = MinecraftMakerPlugin.getInstance().getConfig().getInt("server.id");
    private static final List<String> TITLE = Arrays.asList(
            "§B§LMinecraftMaker", "§LM§B§LinecraftMaker", "§LMi§B§LnecraftMaker",
            "§LMin§B§LecraftMaker", "§LMine§B§LcraftMaker", "§LMinec§B§LraftMaker",
            "§LMinecr§B§LaftMaker", "§LMinecra§B§LftMaker", "§LMinecraf§B§LtMaker",
            "§LMinecraft§B§LMaker", "§LMinecraftM§B§Laker", "§LMinecraftMa§B§Lker",
            "§LMinecraftMak§B§Ler", "§LMinecraftMake§B§Lr", "§B§LMinecraftMaker",
            "§B§LMinecraftMaker", "§B§LMinecraftMaker", "§F§LMinecraftMaker",
            "§B§LMinecraftMaker", "§F§LMinecraftMaker", "§B§LMinecraftMaker",
            "§F§LMinecraftMaker");

    private final MinecraftMakerPlugin plugin;

    private int index;
    private Scoreboard scoreboard;
    private HashMap<Integer, String> scores;

    public MakerScoreboard(){
        // Initialize variables
        this.index = 0;
        this.scores = new HashMap<Integer, String>();
        this.plugin = MinecraftMakerPlugin.getInstance();
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        // Clear teams
        if (!this.scoreboard.getTeams().isEmpty()){
            this.scoreboard.getTeams().clear();
        }

        // Create teams
        for (Rank rank : Rank.values()) {
            if (this.scoreboard.getTeam(rank.getDisplayName()) == null) {
                this.scoreboard.registerNewTeam(rank.getDisplayName()).setPrefix(String.format("%s §F", rank.getDisplayName()));
            }
        }
    }

    public void destroy() {
        // Reset player scores
        this.resetPlayerScores();

        // Delete scoreboard teams
        for (Team team : this.scoreboard.getTeams()) {
            team.unregister();
        }

        // Unregister
        this.scoreboard.getObjective(NAME).unregister();
        //this.scoreboard.getObjective(HEALTH).unregister();
    }

    public synchronized void removePlayer(MakerPlayer makerPlayer){
        // Get online players
        for(Player player : Bukkit.getOnlinePlayers()){
            // Player needs to have a scoreboard
            if(player.getScoreboard() != null){
                // Get team
                Team team = scoreboard.getTeam(makerPlayer.getData().getDisplayRank().getDisplayName());

                // Add player
                if(team != null && team.getPlayers().contains(player)){
                    team.removePlayer(makerPlayer.getPlayer());
                }
            }
        }
    }

    public synchronized void addPlayer(Player player, String team){
        // Register player team in online players scoreboard
        for(Player onlinePlayer : Bukkit.getOnlinePlayers()){
            // Online Player needs to have a scoreboard
            if(onlinePlayer.getScoreboard() != null){
                // Get team
                Team onlinePlayerTeam = onlinePlayer.getScoreboard().getTeam(team);

                // Add player to online player scoreboard team
                if(onlinePlayerTeam != null && !onlinePlayerTeam.getPlayers().contains(player)){
                    onlinePlayerTeam.addPlayer(player);
                }

                // Add online player to player scoreboard team
                Team onlinePlayerCurrentTeam = onlinePlayer.getScoreboard().getPlayerTeam(onlinePlayer);

                // Add player to online player scoreboard team
                if(onlinePlayerCurrentTeam != null){
                    // Get new player team to register online player
                    Team playerTeam = this.scoreboard.getTeam(onlinePlayerCurrentTeam.getName());

                    // Add player to online player scoreboard team
                    if(playerTeam != null && !playerTeam.getPlayers().contains(onlinePlayer)) {
                        playerTeam.addPlayer(onlinePlayer);
                    }
                }
            }
        }
    }

    public void setScore(int slot, String value) {
        // Cut value with a bigger length than 16.
        if (value.length() > 16) {
            value = value.substring(0, 15);
        }

        // Get previous value
        String previousValue = this.scores.put(slot, value);

        // Set new scoreboard value
        if(StringUtils.isEmpty(previousValue)){
            Score score = this.scoreboard.getObjective(NAME).getScore(value);
            score.setScore(slot);
            return;
        }

        // Replace scoreboard value
        if(!value.equals(previousValue)){
            Score score = this.scoreboard.getObjective(NAME).getScore(value);
            this.scoreboard.resetScores(previousValue);
            score.setScore(slot);
        }
    }

    public void setup(Player player){
        // Unregister previous objectives
        if (this.scoreboard.getObjective(NAME) != null){
            this.scoreboard.getObjective(NAME).unregister();
        }

//        // Unregister previous objectives
//        if (this.scoreboard.getObjective(HEALTH) != null){
//            this.scoreboard.getObjective(HEALTH).unregister();
//        }

        // Register objectives
        this.scoreboard.registerNewObjective(NAME, NAME);
//        this.scoreboard.registerNewObjective(HEALTH, HEALTH);

        // Reset scores
        if (player.isOnline()) {
            this.resetPlayerScores();

            // Set objective values
            this.scoreboard.getObjective(NAME).setDisplaySlot(DisplaySlot.SIDEBAR);
            this.scoreboard.getObjective(NAME).setDisplayName(TITLE.get(index));
//            this.scoreboard.getObjective(HEALTH).setDisplaySlot(DisplaySlot.BELOW_NAME);
//            this.scoreboard.getObjective(HEALTH).setDisplayName(Double.toString(player.getHealth()));

            player.setScoreboard(this.scoreboard);
        } else {
            this.destroy();
        }
    }

    public void update(MakerPlayer makerPlayer) {
        this.displayFlashingTitle(makerPlayer.getCurrentTick());

        if(makerPlayer.getCurrentTick() % 20 == 0) {
            this.setScore(13, "§4");
            this.setScore(12, plugin.getMessage("scoreboard.server.title"));
            this.setScore(11, plugin.getMessage("scoreboard.server.name", SERVER_ID));
            this.setScore(10, "§3");
            this.setScore(7, "§2");
            this.setScore(4, "§1");
            this.setScore(1, "§0");
            this.setScore(0, plugin.getMessage("scoreboard.url"));

            if(makerPlayer.getCurrentLevel() == null){
                this.setScore(9, plugin.getMessage("scoreboard.coins.title"));
                this.setScore(8, plugin.getMessage("scoreboard.coins.name", makerPlayer.getData().getCoins()));
                this.setScore(6, plugin.getMessage("scoreboard.clear-level.title"));
                this.setScore(5, plugin.getMessage("scoreboard.clear-level.name", makerPlayer.getData().getLevelsClear().size()));
                this.setScore(3, plugin.getMessage("scoreboard.likes.title"));
                this.setScore(2, plugin.getMessage("scoreboard.likes.name", makerPlayer.getLevelsLikes()));
            } else if(GameMode.CREATIVE.equals(makerPlayer.getPlayer().getGameMode()) || makerPlayer.getCurrentTick() % 240 < 120){
                this.setScore(9, plugin.getMessage("scoreboard.level-name.title"));
                this.setScore(8, plugin.getMessage("scoreboard.level-name.name", StringUtils.isEmpty(makerPlayer.getCurrentLevel().getLevelName()) ?
                        MinecraftMakerPlugin.getInstance().getMessage("general.empty") : makerPlayer.getCurrentLevel().getLevelName()));
                this.setScore(6, plugin.getMessage("scoreboard.level-author.title"));
                this.setScore(5, plugin.getMessage("scoreboard.level-author.name", makerPlayer.getCurrentLevel().getAuthorName()));
                this.setScore(3, plugin.getMessage("scoreboard.level-likes.title"));
                this.setScore(2, plugin.getMessage("scoreboard.level-likes.name", makerPlayer.getCurrentLevel().getLikes()));
            } else {
                this.setScore(9, plugin.getMessage("scoreboard.level-player.title"));
                this.setScore(8, plugin.getMessage("scoreboard.level-player.name", makerPlayer.getRecordUsername()));
                this.setScore(6, plugin.getMessage("scoreboard.level-time.title"));
                this.setScore(5, plugin.getMessage("scoreboard.level-time.name", makerPlayer.getRecordTime()));
                this.setScore(3, plugin.getMessage("scoreboard.player-best.title"));
                this.setScore(2, plugin.getMessage("scoreboard.player-best.name", makerPlayer.getPlayerRecordTime()));
            }
        }
    }

    private void displayFlashingTitle(long currentTick){
        if(this.scoreboard.getObjective(NAME) != null && currentTick % 3 == 0){
            this.index = this.index == TITLE.size() - 1 ? 0 : this.index + 1;
            this.scoreboard.getObjective(NAME).setDisplayName(TITLE.get(this.index));
        }
    }

    private void resetPlayerScores() {
        for (String entry : this.scoreboard.getEntries()) {
            this.scoreboard.resetScores(entry);
        }

        this.scores.clear();
    }
}
