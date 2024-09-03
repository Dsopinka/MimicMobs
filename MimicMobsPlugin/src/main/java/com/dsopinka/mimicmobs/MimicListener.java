package com.dsopinka.mimicmobs;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MimicListener implements Listener {

    private final JavaPlugin plugin;
    private final ConcurrentHashMap<UUID, Integer> mobGroupCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, List<LivingEntity>> activeMimicGroups = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, BukkitRunnable> activeCountdownTasks = new ConcurrentHashMap<>();

    public MimicListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();

        ConfigurationSection mimicConfig = plugin.getConfig().getConfigurationSection("mimics." + blockType.name());
        if (mimicConfig != null) {
            double chance = mimicConfig.getDouble("chance", 0.0);
            Random random = new Random();
            if (random.nextDouble() < chance) {
                String mimicType = mimicConfig.getString("type", "Explosive_Mimic");
                transformToMimic(player, mimicType, mimicConfig);
            }
        }
    }

    private void transformToMimic(Player player, String mimicType, ConfigurationSection mimicConfig) {
        UUID groupID = UUID.randomUUID(); // Unique identifier for the group
        int totalMobs = 0;
        List<LivingEntity> spawnedMobs = new java.util.ArrayList<>();

        // Get the block type where the mimic was triggered
        String blockType = player.getTargetBlockExact(5).getType().toString();

        switch (mimicType) {
            case "Explosive_Mimic":
                player.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "MIMIC: " + ChatColor.UNDERLINE + ChatColor.YELLOW + "You have triggered an Explosive Mimic!");
                LivingEntity mimic = (LivingEntity) player.getWorld().spawnEntity(player.getLocation(), EntityType.CREEPER);
                mimic.setCustomName("Explosive Mimic");
                mimic.setMetadata("mimicType", new FixedMetadataValue(plugin, mimicType));
                mimic.setMetadata("groupID", new FixedMetadataValue(plugin, groupID));
                mimic.setMetadata("blockType", new FixedMetadataValue(plugin, blockType)); // Setting blockType metadata
                mimic.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 1)); // Prevents sunlight damage
                mimic.setRemoveWhenFarAway(false); // Prevents despawning
                spawnedMobs.add(mimic);
                mimic.getWorld().createExplosion(mimic.getLocation(), (float) mimicConfig.getDouble("explosion_power", 2.0));
                break;

            case "Summoning_Mimic":
                player.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "MIMIC: " + ChatColor.UNDERLINE + ChatColor.YELLOW + "You have triggered a Summoning Mimic!");
                List<String> mobsToSummon = mimicConfig.getStringList("summon_mobs");
                int summonCount = mimicConfig.getInt("summon_count", 3);
                totalMobs = mobsToSummon.size() * summonCount;
                for (int i = 0; i < summonCount; i++) {
                    for (String mobType : mobsToSummon) {
                        EntityType entityType = EntityType.valueOf(mobType.toUpperCase());
                        LivingEntity summonedMob = (LivingEntity) player.getWorld().spawnEntity(player.getLocation(), entityType);
                        summonedMob.setCustomName("Summoned by Mimic");
                        summonedMob.setMetadata("mimicType", new FixedMetadataValue(plugin, mimicType));
                        summonedMob.setMetadata("groupID", new FixedMetadataValue(plugin, groupID));
                        summonedMob.setMetadata("blockType", new FixedMetadataValue(plugin, blockType)); // Setting blockType metadata
                        summonedMob.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 1)); // Prevents sunlight damage
                        summonedMob.setRemoveWhenFarAway(false); // Prevents despawning
                        spawnedMobs.add(summonedMob);
                    }
                }
                break;

            case "Ambusher_Mimic":
                player.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "MIMIC: " + ChatColor.UNDERLINE + ChatColor.YELLOW + "You have triggered an Ambusher Mimic!");
                String ambushMob = mimicConfig.getString("ambush_mob", "SILVERFISH");
                int ambushCount = mimicConfig.getInt("ambush_count", 5);
                totalMobs = ambushCount;
                for (int i = 0; i < ambushCount; i++) {
                    EntityType entityType = EntityType.valueOf(ambushMob.toUpperCase());
                    LivingEntity ambushEntity = (LivingEntity) player.getWorld().spawnEntity(player.getLocation(), entityType);
                    ambushEntity.setCustomName("Ambush by Mimic");
                    ambushEntity.setMetadata("mimicType", new FixedMetadataValue(plugin, mimicType));
                    ambushEntity.setMetadata("groupID", new FixedMetadataValue(plugin, groupID));
                    ambushEntity.setMetadata("blockType", new FixedMetadataValue(plugin, blockType)); // Setting blockType metadata
                    ambushEntity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 1)); // Prevents sunlight damage
                    ambushEntity.setRemoveWhenFarAway(false); // Prevents despawning
                    spawnedMobs.add(ambushEntity);
                }
                break;

            default:
                player.sendMessage("You have encountered an unknown type of Mimic!");
                break;
        }

        if (totalMobs > 0 || !spawnedMobs.isEmpty()) {
            mobGroupCounts.put(groupID, totalMobs);
            activeMimicGroups.put(groupID, spawnedMobs);
            startMimicTimer(groupID, mimicConfig, player);
        }
    }

    @EventHandler
    public void onMimicDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        // Only proceed if the entity has both "mimicType" and "groupID" metadata
        if (!entity.hasMetadata("mimicType") || !entity.hasMetadata("groupID")) {
            // If metadata is missing, do nothing and return
            return;
        }

        String mimicType = entity.getMetadata("mimicType").get(0).asString();
        UUID groupID = (UUID) entity.getMetadata("groupID").get(0).value();

        // Check if the group ID is valid
        if (mobGroupCounts.containsKey(groupID)) {
            int remaining = mobGroupCounts.get(groupID) - 1;

            if (remaining > 0) {
                mobGroupCounts.put(groupID, remaining);
                if (killer != null) {
                    killer.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "MIMIC: " + ChatColor.YELLOW + remaining + " mobs remaining!");
                }
            } else {
                // Cancel the countdown task when all mobs are killed
                cancelMimicCountdown(groupID);

                mobGroupCounts.remove(groupID);
                activeMimicGroups.remove(groupID);

                // Check if blockType metadata exists before accessing it
                if (entity.hasMetadata("blockType")) {
                    String blockType = entity.getMetadata("blockType").get(0).asString();
                    String configPath = "mimics." + blockType;
                    ConfigurationSection mimicConfig = plugin.getConfig().getConfigurationSection(configPath);

                    // Give rewards using the correct mimic configuration, if the killer is not null
                    if (killer != null && mimicConfig != null) {
                        giveRewards(killer, mimicConfig);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Iterate over all active mimic groups
        for (UUID groupID : activeMimicGroups.keySet()) {
            List<LivingEntity> mobs = activeMimicGroups.get(groupID);

            // Check if the player who died is the same as the player who triggered the mimic
            if (mobs != null && !mobs.isEmpty() && mobs.get(0).getKiller() != null && mobs.get(0).getKiller().equals(player)) {
                // Despawn all mobs in the group
                despawnMimicMobs(groupID);

                // Cancel the countdown task when the player dies
                cancelMimicCountdown(groupID);

                // Notify the player that the mimic has ended
                player.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "MIMIC: " + ChatColor.YELLOW + "The mimic has ended because you died!");

                // Break the loop as we've handled the mimic group
                break;
            }
        }
    }

    private void startMimicTimer(UUID groupID, ConfigurationSection mimicConfig, Player player) {
        if (plugin.getConfig().getBoolean("time_limit_enabled", false)) {
            int timeLimit = plugin.getConfig().getInt("time_limit_seconds", 60);
            player.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "MIMIC: " + ChatColor.YELLOW + "You have " + timeLimit + " seconds before the mimic ends!");

            // Ensure that any previous countdown task is canceled before starting a new one
            cancelMimicCountdown(groupID);

            // Main timer to despawn mobs after the time limit
            BukkitRunnable mainTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (activeMimicGroups.containsKey(groupID)) {
                        despawnMimicMobs(groupID);
                        player.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "MIMIC: " + ChatColor.YELLOW + "The mimic has ended!");
                    }
                }
            };
            mainTask.runTaskLater(plugin, timeLimit * 20L);

            // Store the countdown task so it can be canceled
            activeCountdownTasks.put(groupID, mainTask);

            // Countdown tasks
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!mainTask.isCancelled()) {
                        player.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "MIMIC: " + ChatColor.YELLOW + "This mimic will end in 3 seconds!");
                    }
                }
            }.runTaskLater(plugin, (timeLimit - 3) * 20L);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!mainTask.isCancelled()) {
                        player.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "MIMIC: " + ChatColor.YELLOW + "This mimic will end in 2 seconds!");
                    }
                }
            }.runTaskLater(plugin, (timeLimit - 2) * 20L);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!mainTask.isCancelled()) {
                        player.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "MIMIC: " + ChatColor.YELLOW + "This mimic will end in 1 second!");
                    }
                }
            }.runTaskLater(plugin, (timeLimit - 1) * 20L);
        }
    }

    private void cancelMimicCountdown(UUID groupID) {
        BukkitRunnable countdownTask = activeCountdownTasks.remove(groupID);
        if (countdownTask != null) {
            countdownTask.cancel();
        }
    }

    private void despawnMimicMobs(UUID groupID) {
        List<LivingEntity> mobs = activeMimicGroups.remove(groupID);
        if (mobs != null) {
            for (LivingEntity mob : mobs) {
                mob.remove();
            }
        }
        mobGroupCounts.remove(groupID);
        cancelMimicCountdown(groupID);
    }

    public void despawnAllMimicMobs() {
        for (UUID groupID : activeMimicGroups.keySet()) {
            despawnMimicMobs(groupID);
        }
    }

    private void giveRewards(Player player, ConfigurationSection mimicConfig) {
        ConfigurationSection rewardsConfig = mimicConfig.getConfigurationSection("rewards");
        if (rewardsConfig != null) {
            List<Map<?, ?>> items = rewardsConfig.getMapList("items");
            for (Map<?, ?> itemConfig : items) {
                String materialName = (String) itemConfig.get("material");
                int amount = (int) itemConfig.get("amount");

                try {
                    Material material = Material.valueOf(materialName.toUpperCase());
                    ItemStack rewardItem = new ItemStack(material, amount);
                    player.getInventory().addItem(rewardItem);
                    player.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "MIMIC: " + ChatColor.YELLOW + "You received " + amount + " " + materialName + "(s) as a reward!");
                } catch (IllegalArgumentException e) {
                    player.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "MIMIC: " + ChatColor.YELLOW + "Error: Invalid material name '" + materialName + "' in configuration.");
                }
            }

            // Give buff rewards
            List<String> buffs = rewardsConfig.getStringList("buffs");
            for (String buff : buffs) {
                String[] parts = buff.split(":");
                PotionEffectType effectType = PotionEffectType.getByName(parts[0].toUpperCase());
                int duration = Integer.parseInt(parts[1]);
                int amplifier = Integer.parseInt(parts[2]);
                player.addPotionEffect(new PotionEffect(effectType, duration, amplifier));
                player.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "MIMIC: " + ChatColor.YELLOW + "You received a buff: " + effectType.getName() + " for " + (duration / 20) + " seconds!");
            }

            if (rewardsConfig.contains("xp")) {
                int xpAmount = rewardsConfig.getInt("xp");
                player.giveExp(xpAmount);
                player.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "MIMIC: " + ChatColor.YELLOW + "You received " + xpAmount + " XP as a reward!");
            }
        } else {
            player.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "MIMIC: " + ChatColor.YELLOW + "No rewards configured for this mimic.");
        }
    }

    public void listMimics(Player player) {
        player.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "MIMIC SETUPS:");
        ConfigurationSection mimicsSection = plugin.getConfig().getConfigurationSection("mimics");

        if (mimicsSection != null) {
            for (String blockType : mimicsSection.getKeys(false)) {
                ConfigurationSection mimicConfig = mimicsSection.getConfigurationSection(blockType);
                String type = mimicConfig.getString("type", "Unknown");
                double chance = mimicConfig.getDouble("chance", 0.0) * 100;
                StringBuilder summary = new StringBuilder();
                summary.append(ChatColor.GOLD + blockType + ": ");
                summary.append(ChatColor.YELLOW + "Type: " + type + ", ");
                summary.append("Trigger Chance: " + chance + "%");

                if (type.equals("Summoning_Mimic")) {
                    List<String> mobs = mimicConfig.getStringList("summon_mobs");
                    int summonCount = mimicConfig.getInt("summon_count", 1);
                    summary.append(", Summons: ");
                    for (String mob : mobs) {
                        summary.append(summonCount + "x " + mob + ", ");
                    }
                }

                ConfigurationSection rewardsConfig = mimicConfig.getConfigurationSection("rewards");
                if (rewardsConfig != null) {
                    summary.append(", Rewards: ");
                    List<Map<?, ?>> items = rewardsConfig.getMapList("items");
                    for (Map<?, ?> itemConfig : items) {
                        String materialName = (String) itemConfig.get("material");
                        int amount = (int) itemConfig.get("amount");
                        summary.append(amount + "x " + materialName + ", ");
                    }

                    List<String> buffs = rewardsConfig.getStringList("buffs");
                    for (String buff : buffs) {
                        String[] parts = buff.split(":");
                        summary.append("Buff: " + parts[0] + " (" + (Integer.parseInt(parts[1]) / 20) + "s), ");
                    }

                    if (rewardsConfig.contains("xp")) {
                        int xpAmount = rewardsConfig.getInt("xp");
                        summary.append("XP: " + xpAmount + ", ");
                    }
                }

                player.sendMessage(summary.toString().trim());
            }
        } else {
            player.sendMessage(ChatColor.RED + "No mimic setups found in the config.");
        }
    }
}
