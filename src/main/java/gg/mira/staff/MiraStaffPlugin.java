package gg.mira.staff;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class MiraStaffPlugin extends JavaPlugin implements Listener {
    private static final String PREFIX = "&5&lMira &8>> &r";
    private final Set<UUID> staffMode = new HashSet<>();
    private final Set<UUID> vanished = new HashSet<>();
    private final Set<UUID> frozen = new HashSet<>();
    private StaffApi api;

    @Override
    public void onEnable() {
        api = new StaffApiImpl();
        getServer().getServicesManager().register(StaffApi.class, api, this, ServicePriority.Normal);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        for (UUID id : new HashSet<>(vanished)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) setVanish(player, false);
        }
        getServer().getServicesManager().unregisterAll(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) && !command.getName().equalsIgnoreCase("freeze") && !command.getName().equalsIgnoreCase("staffchat")) {
            msg(sender, "&cPlayers only.");
            return true;
        }
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "staff" -> toggleStaff((Player) sender);
            case "vanish" -> toggleVanish((Player) sender);
            case "freeze" -> freeze(sender, args);
            case "inspect" -> inspect((Player) sender, args);
            case "staffchat" -> staffChat(sender, args);
            case "stafftp" -> teleport((Player) sender, args);
            default -> false;
        };
    }

    private boolean toggleStaff(Player player) {
        UUID id = player.getUniqueId();
        if (staffMode.remove(id)) {
            if (vanished.contains(id)) setVanish(player, false);
            msg(player, "&cStaff mode disabled.");
        } else {
            staffMode.add(id);
            msg(player, "&aStaff mode enabled. &7Use /vanish, /inspect, /freeze and /stafftp. Flight remains controlled by MiraFly.");
        }
        return true;
    }

    private boolean toggleVanish(Player player) {
        setVanish(player, !vanished.contains(player.getUniqueId()));
        return true;
    }

    private void setVanish(Player player, boolean value) {
        UUID id = player.getUniqueId();
        if (value) {
            vanished.add(id);
            for (Player viewer : Bukkit.getOnlinePlayers()) if (!viewer.hasPermission("mirastaff.vanish.see")) viewer.hidePlayer(this, player);
            msg(player, "&aVanish enabled.");
        } else {
            vanished.remove(id);
            for (Player viewer : Bukkit.getOnlinePlayers()) viewer.showPlayer(this, player);
            msg(player, "&cVanish disabled.");
        }
    }

    private boolean freeze(CommandSender sender, String[] args) {
        if (args.length < 1) { msg(sender, "&cUsage: /freeze <player>"); return true; }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { msg(sender, "&cPlayer not online."); return true; }
        if (frozen.remove(target.getUniqueId())) {
            msg(target, "&aYou have been unfrozen by staff.");
            msg(sender, "&aUnfroze " + target.getName() + ".");
        } else {
            frozen.add(target.getUniqueId());
            msg(target, "&cYou have been frozen by staff. Do not disconnect.");
            msg(sender, "&eFroze " + target.getName() + ".");
        }
        return true;
    }

    private boolean inspect(Player staff, String[] args) {
        if (args.length < 1) { msg(staff, "&cUsage: /inspect <player>"); return true; }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { msg(staff, "&cPlayer not online."); return true; }
        staff.openInventory(target.getInventory());
        msg(staff, "&7Inspecting &f" + target.getName() + "&7's inventory.");
        return true;
    }

    private boolean teleport(Player staff, String[] args) {
        if (args.length < 1) { msg(staff, "&cUsage: /stafftp <player>"); return true; }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { msg(staff, "&cPlayer not online."); return true; }
        staff.teleportAsync(target.getLocation());
        msg(staff, "&aTeleported to " + target.getName() + ".");
        return true;
    }

    private boolean staffChat(CommandSender sender, String[] args) {
        if (args.length == 0) { msg(sender, "&cUsage: /staffchat <message>"); return true; }
        String message = "&8[&bStaff&8] &f" + sender.getName() + "&7: &f" + String.join(" ", args);
        for (Player online : Bukkit.getOnlinePlayers()) if (online.hasPermission("mirastaff.chat")) msg(online, message);
        if (!(sender instanceof Player)) msg(sender, message);
        return true;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!frozen.contains(event.getPlayer().getUniqueId()) || event.getTo() == null) return;
        if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getY() != event.getTo().getY() || event.getFrom().getZ() != event.getTo().getZ()) event.setTo(event.getFrom());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        for (UUID id : vanished) {
            Player hidden = Bukkit.getPlayer(id);
            if (hidden != null && !event.getPlayer().hasPermission("mirastaff.vanish.see")) event.getPlayer().hidePlayer(this, hidden);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (frozen.contains(event.getPlayer().getUniqueId())) {
            for (Player online : Bukkit.getOnlinePlayers()) if (online.hasPermission("mirastaff.use")) msg(online, "&c[FROZEN QUIT] &f" + event.getPlayer().getName() + " disconnected while frozen.");
        }
        staffMode.remove(event.getPlayer().getUniqueId());
        vanished.remove(event.getPlayer().getUniqueId());
    }

    private void msg(CommandSender sender, String raw) { sender.sendMessage(ChatColor.translateAlternateColorCodes('&', PREFIX + raw)); }

    public interface StaffApi {
        boolean isStaffMode(UUID player);
        boolean isVanished(UUID player);
        boolean isFrozen(UUID player);
        void freeze(UUID player, boolean frozen);
    }

    private final class StaffApiImpl implements StaffApi {
        @Override public boolean isStaffMode(UUID player) { return staffMode.contains(player); }
        @Override public boolean isVanished(UUID player) { return vanished.contains(player); }
        @Override public boolean isFrozen(UUID player) { return frozen.contains(player); }
        @Override public void freeze(UUID player, boolean value) { if (value) frozen.add(player); else frozen.remove(player); }
    }
}
