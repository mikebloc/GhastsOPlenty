////////////////////////////////////
///                              ///
///            Cutzuu            ///
///                              ///
////////////////////////////////////

// https://github.com/cutzuu

package me.cutzuu.ghastsOPlenty;
import org.bukkit.*;
import org.bukkit.block.BlockType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;
import java.util.Random;

public final class GhastsOPlenty extends JavaPlugin implements Listener
{

    @Override
    public void onEnable()
    {
        saveDefaultConfig();
        loadConfiguration();
        getServer().getPluginManager().registerEvents(this, this);
    }


    public static class Global
    {
        public static EntityType theMob = EntityType.GHAST;
        public static boolean configToggleSquidSpam;
        public static boolean configToggleGhastsAttackEachOther;
        public static boolean configToggleHurtPlayer;
        public static int configExplosivePower;

        public static List<EntityType> ignoredEntities = List.of(
                EntityType.VILLAGER,
                EntityType.BAT,
                EntityType.ITEM,
                EntityType.EXPERIENCE_ORB,
                EntityType.FIREBALL,
                EntityType.DROWNED,
                EntityType.SALMON,
                EntityType.COD,
                EntityType.PUFFERFISH,
                EntityType.GLOW_SQUID,
                EntityType.NAUTILUS,
                EntityType.TROPICAL_FISH);
    }

    private void loadConfiguration()
    {
        Global.configToggleSquidSpam = this.getConfig().getBoolean("SquidSpam");
        Global.configToggleGhastsAttackEachOther = this.getConfig().getBoolean("GhastFriendlyFire");
        Global.configToggleHurtPlayer = this.getConfig().getBoolean("PlayerDamage");
        Global.configExplosivePower = this.getConfig().getInt("FireballPower");
    }


    // This ensures mob spawns tied to biomes such as cows on plains are also swapped.
    // MobSpawnEvent does not cover these.
    @EventHandler
    public void chunkLoad(ChunkLoadEvent e)
    {
        Chunk chunk = e.getChunk();
        World world = e.getWorld();
        for (Entity entity1 : chunk.getEntities())
        {
            boolean check1 = Global.ignoredEntities.contains(entity1.getType());
            if (check1) return;

            if (entity1.getType() != Global.theMob || entity1.getType() != EntityType.PLAYER)
            {
                boolean check = entity1 instanceof Mob;

                if (!check) return;
                Location location = entity1.getLocation();
                entity1.remove();

                world.spawnEntity(location, Global.theMob);
            }
        }
    }

    @EventHandler
    public void hello(EntitySpawnEvent e)
    {
        Entity entity = e.getEntity();
        World world = e.getEntity().getWorld();
        Location location = e.getLocation();
        World.Environment environment = world.getEnvironment();
        Random random = new Random();

        // Allows breeding of animals.
        CreatureSpawnEvent.SpawnReason spawnReason = e.getEntity().getEntitySpawnReason();
        if (spawnReason != CreatureSpawnEvent.SpawnReason.NATURAL)return;


        //Heavily decreases the squid replacement ghast spawn
        if (entity.getType() == EntityType.SQUID)
        {
            if(!Global.configToggleSquidSpam)
            {
                int squiD = random.nextInt(0,4);
                if (squiD == 2) return;
            }
        }

        //Makes the Nether and End playable.
        if (environment == World.Environment.NETHER || environment == World.Environment.THE_END)
        {
            int envr = random.nextInt(0,3);
            if (envr == 1) return;
        }

        // Prevents Ghast Replacements from causing more Ghast spawns.
        if (entity.getType() == Global.theMob) return;

        // This prevents things like block drops and projectiles from creating a Mob Spawn.
        boolean check2 = Global.ignoredEntities.contains(entity.getType());
        if (check2) return;


        if (entity instanceof Mob && entity.getType() != Global.theMob || entity.getType() != EntityType.PLAYER)
        {
            // Good for debugging.
            //Component message = Component.text(entity.getName() + " was purged.");
            //getServer().broadcast(message);

            entity.remove();

            // Grabs the player location so we can have a scope of where to spawn ghasts.
            Location playerLoc = null;
            for (Entity player : entity.getNearbyEntities(100,100,100))
            {
                if (player instanceof Player)
                {
                    playerLoc = player.getLocation();
                }
            }

            // If mobs are more than 5 blocks underneath the player, set the Y around the player height.
            // The 20 Check prevent massive ghast spam.
            if (playerLoc != null)
            {
                double playerY = playerLoc.getY();
                double entityY = entity.getY();

                if ((playerY - entityY) > 20) return;

                if ((playerY - entityY) > 5)
                {
                    double nextY = random.nextDouble(playerY-2, playerY+18);
                    location.setY(nextY);
                }

                int badCheck = 0;
                // Allow block checks to be made before we just decide that a Ghast cannot fit where the replacement spawned.
                while (location.getBlock().getBlockData() != BlockType.AIR || location.getBlock().getBlockData()!= BlockType.WATER)
                {
                    if (playerLoc.getY() <60 && badCheck > 4) break;
                    if (badCheck > 10) break;
                    location.setX(location.getX()+1);
                    badCheck++;
                }

                badCheck = 0;
                while (location.getBlock().getBlockData() != BlockType.AIR || location.getBlock().getBlockData()!= BlockType.WATER)
                {
                    if (playerLoc.getY() <60 && badCheck > 4) break;
                    if (badCheck > 10) break;
                    location.setY(location.getY()+1);
                    badCheck++;
                }

                badCheck = 0;
                while (location.getBlock().getBlockData() != BlockType.AIR || location.getBlock().getBlockData()!= BlockType.WATER)
                {
                    if (playerLoc.getY() <60 && badCheck > 4) break;
                    if (badCheck > 10) break;
                    location.setZ(location.getZ()+1);
                    badCheck++;
                }

                int swap1 = random.nextInt(-15,15);
                int swap2 = random.nextInt(-15,15);

                double x = location.getX() + swap1;
                double z = location.getZ() + swap2;
                location.set(x,location.getY(),z);

                world.spawnEntity(location, Global.theMob);
            }
        }
    }


    // Give the player 1 Bread and 1 Feather if they kill a Ghast.
    // Does not include fireball throwback kills. Idk how to code that. Fireball will allow ghast friendly fire / suicide to drop loot.
    @EventHandler
    public void deff(EntityDeathEvent e)
    {
        Entity entity = e.getEntity();
        World world = e.getEntity().getWorld();
        Location location = entity.getLocation();
        boolean badEntity = entity instanceof Mob;
        if (!badEntity) return;

        boolean causeByPlayer = e.getDamageSource().getCausingEntity() instanceof Player;
        if (!causeByPlayer) return;

        if (entity.getType() == EntityType.GHAST)
        {
            world.dropItem(location.toCenterLocation(), ItemStack.of(Material.BREAD));
            world.dropItem(location.toCenterLocation(), ItemStack.of(Material.FEATHER));
        }


    }

    @EventHandler
    public void ouch(EntityDamageEvent e)
    {
        EntityDamageEvent.DamageCause damageCause = e.getCause();
        Entity entity = e.getEntity();

        if (!Global.configToggleHurtPlayer)
        {
            if (entity instanceof Player)
            {
                e.setCancelled(true);
            }
        }

        // If the Ghast is in a wall or drowning, just remove them.
        // They sometimes just sit underwater or will fly in it and never rise.
        if (entity instanceof Ghast)
        {
            entity.setSilent(true);

            if (damageCause == EntityDamageEvent.DamageCause.DROWNING) entity.remove();

            if (damageCause == EntityDamageEvent.DamageCause.SUFFOCATION)
            {
                e.setCancelled(true);
                entity.remove();
            }
        }
    }

    @EventHandler
    public void booms(ProjectileHitEvent e)
    {
        Entity entity = e.getEntity();
        World world = e.getEntity().getWorld();
        Location location = entity.getLocation();
        if (entity instanceof Fireball)
        {
            if (Global.configExplosivePower > 100)
            {
                Global.configExplosivePower = 100;
            }
            world.createExplosion(location, Global.configExplosivePower);
        }
    }

    @EventHandler
    public void friendlyFire(EntityTargetEvent e)
    {
        if (Global.configToggleGhastsAttackEachOther)
        {
            assert e.getTarget() != null;
            Entity entity = e.getEntity();

            if (e.getTarget().getType() == EntityType.PLAYER)
            {
                e.setTarget(entity);

                for (Entity entity1 : entity.getNearbyEntities(60,60,60))
                {
                    assert e.getTarget() != null;
                    if (entity1.getType() == Global.theMob)
                    {
                        if (entity1.equals(entity)) return;
                        if (entity1.getType().equals(EntityType.PLAYER)) e.setCancelled(true);

                        e.setTarget(entity1);
                    }
                }
            }
        }
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args)
    {
        if (args.length == 0)
        {
            sender.sendMessage("§eUsage: /ghastsoplenty reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload"))
        {
            if (sender.hasPermission("ghastsoplenty.reload"))
            {
                reloadConfig();
                loadConfiguration();
                sender.sendMessage("§7[§6GhastsOPlenty§7] §aConfig reloaded.");
            }
            else sender.sendMessage("§cYou don't have permission to do that.");
            return true;
        }

        sender.sendMessage("§cUnknown subcommand.");
        return true;
    }

    @Override
    public void onDisable()
    {
        // Plugin shutdown logic
    }
}
