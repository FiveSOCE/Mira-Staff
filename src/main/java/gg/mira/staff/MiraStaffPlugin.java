package gg.mira.staff;

import com.mira.core.api.MiraCore;
import com.mira.core.api.MiraCoreProvider;
import com.mira.core.api.ModuleHealth;
import gg.mira.staff.api.event.StaffStateChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class MiraStaffPlugin extends JavaPlugin implements Listener, TabExecutor {
    private static final String PREFIX = "&5&lMira &8>> &r";

    private final Set<UUID> staffMode = new HashSet<>();
    private final Set<UUID> vanished = new HashSet<>();
    private final Set<UUID> frozen = new HashSet<>();
    private MiraCore core;
    private StaffApi api;
    private File stateFile;
    private YamlConfiguration state;

    @Override
    public void onEnable() {
        core = MiraCoreProvider.require();
        loadState();
        api = new StaffApiImpl();

        getServer().getServicesManager().register(StaffApi.class, api, this, ServicePriority.Normal);
        core.services().register(StaffApi.class, api);
        core.modules().register(this, "MiraStaff");
        core.modules().setHealth(this, ModuleHealth.HEALTHY,
                "Staff mode, vanish, persistent freeze state and audited staff tools ready");

        getServer().getPluginManager().registerEvents(this, this);
        for (String commandName : List.of("staff", "vanish", "freeze", "inspect", "staffchat", "stafftp", "staffstatus")) {
            var command = getCommand(commandName);
            if (command != null) {
                command.setExecutor(this);
                command.setTabCompleter(this);
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) applyVisibilityFor(player);
        getLogger().info("MiraStaff v" + getPluginMeta().getVersion() + " enabled with " + frozen.size() + " persistent frozen player(s).");
    }

    @Override
    public void onDisable() {
        saveState();
        for (UUID id : new HashSet<>(vanished)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) setVanish(player, false, false);
        }
        getServer().getServicesManager().unregisterAll(this);
        if (core != null) {
            if (api != null) core.services().unregister(StaffApi.class, api);
            core.modules().unregister(this);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (!(sender instanceof Player) && !Set.of("freeze", "staffchat", "staffstatus").contains(name)) {
            msg(sender, "&cPlayers only.");
            return true;
        }

        return switch (name) {
            case "staff" -> toggleStaff((Player) sender);
            case "vanish" -> toggleVanish((Player) sender);
            case "freeze" -> freeze(sender, args);
            case "inspect" -> inspect((Player) sender, args);
            case "staffchat" -> staffChat(sender, args);
            case "stafftp" -> teleport((Player) sender, args);
            case "staffstatus" -> status(sender, args);
            default -> false;
        };
    }

    private boolean toggleStaff(Player player) {
        UUID id = player.getUniqueId();
        boolean enabled;
        if (staffMode.remove(id)) {
            enabled = false;
            if (vanished.contains(id)) setVanish(player, false, true);
            msg(player, "&cStaff mode disabled.");
        } else {
            staffMode.add(id);
            enabled = true;
            msg(player, "&aStaff mode enabled. &7Use /vanish, /inspect, /freeze and /stafftp. Flight remains controlled by MiraFly.");
        }
        emit(player, StaffStateChangeEvent.State.STAFF_MODE, enabled);
        audit(player, "STAFF_MODE", player.getUniqueId().toString(), Map.of("enabled", Boolean.toString(enabled)));
        return true;
    }

    private boolean toggleVanish(Player player) {
        setVanish(player, !vanished.contains(player.getUniqueId()), true);
        return true;
    }

    private void setVanish(Player player, boolean value, boolean notify) {
        UUID id = player.getUniqueId();
        if (value) {
            vanished.add(id);
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (!viewer.hasPermission("mirastaff.vanish.see")) viewer.hidePlayer(this, player);
            }
            if (notify) msg(player, "&aVanish enabled.");
        } else {
            vanished.remove(id);
            for (Player viewer : Bukkit.getOnlinePlayers()) viewer.showPlayer(this, player);
            if (notify) msg(player, "&cVanish disabled.");
        }
        if (notify) {
            emit(player, StaffStateChangeEvent.State.VANISH, value);
            audit(player, "VANISH", id.toString(), Map.of("enabled", Boolean.toString(value)));
        }
    }

    private boolean freeze(CommandSender sender, String[] args) {
        if (args.length < 1) {
            msg(sender, "&eUsage: /freeze <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            msg(sender, "&cPlayer not online.");
            return true;
        }

        boolean enabled;
        if (frozen.remove(target.getUniqueId())) {
            enabled = false;
            msg(target, "&aYou have been unfrozen by staff.");
            msg(sender, "&aUnfroze &f" + target.getName() + "&a.");
        } else {
            enabled = true;
            frozen.add(target.getUniqueId());
            msg(target, "&cYou have been frozen by staff. Do not disconnect.");
            msg(sender, "&eFroze &f" + target.getName() + "&e.");
        }
        saveState();
        emit(target, StaffStateChangeEvent.State.FREEZE, enabled);
        audit(sender, "FREEZE_STATE", target.getUniqueId().toString(),
                Map.of("enabled", Boolean.toString(enabled), "targetName", target.getName()));
        return true;
    }

    private boolean inspect(Player staff, String[] args) {
        if (args.length < 1) {
            msg(staff, "&eUsage: /inspect <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            msg(staff, "&cPlayer not online.");
            return true;
        }
        if (target.getUniqueId().equals(staff.getUniqueId())) {
            msg(staff, "&eYou are already looking at your own inventory.");
            return true;
        }
        staff.openInventory(target.getInventory());
        audit(staff, "INVENTORY_INSPECT", target.getUniqueId().toString(), Map.of("targetName", target.getName()));
        msg(staff, "&7Inspecting &f" + target.getName() + "&7's inventory.");
        return true;
    }

    private boolean teleport(Player staff, String[] args) {
        if (args.length < 1) {
            msg(staff, "&eUsage: /stafftp <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            msg(staff, "&cPlayer not online.");
            return true;
        }
        staff.teleportAsync(target.getLocation()).thenAccept(success -> {
            if (success) {
                audit(staff, "STAFF_TELEPORT", target.getUniqueId().toString(), Map.of("targetName", target.getName()));
                msg(staff, "&aTeleported to &f" + target.getName() + "&a.");
            } else {
                msg(staff, "&cStaff teleport failed.");
            }
        });
        return true;
    }

    private boolean staffChat(CommandSender sender, String[] args) {
        if (args.length == 0) {
            msg(sender, "&eUsage: /staffchat <message>");
            return true;
        }
        String plain = String.join(" ", args).trim();
        if (plain.isBlank()) return true;
        String message = "&8[&bStaff&8] &f" + sender.getName() + "&7: &f" + plain;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("mirastaff.chat")) msg(online, message);
        }
        if (!(sender instanceof Player)) msg(sender, message);
        audit(sender, "STAFF_CHAT", "staff", Map.of("message", plain));
        return true;
    }

    private boolean status(CommandSender sender, String[] args) {
        if (args.length < 1) {
            msg(sender, "&eUsage: /staffstatus <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            msg(sender, "&cPlayer not online.");
            return true;
        }
        UUID id = target.getUniqueId();
        msg(sender, "&bStaff State &7- &f" + target.getName());
        msg(sender, "&7Staff mode: " + yes(staffMode.contains(id))
                + " &7Vanish: " + yes(vanished.contains(id))
                + " &7Frozen: " + yes(frozen.contains(id)));
        return true;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!frozen.contains(event.getPlayer().getUniqueId()) || event.getTo() == null) return;
        if (event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ()) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        applyVisibilityFor(event.getPlayer());
        if (frozen.contains(event.getPlayer().getUniqueId())) {
            msg(event.getPlayer(), "&cYou are still frozen by staff. Do not disconnect.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (frozen.contains(id)) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.hasPermission("mirastaff.use")) {
                    msg(online, "&c[FROZEN QUIT] &f" + event.getPlayer().getName() + " &7disconnected while frozen.");
                }
            }
            core.audit().record("MiraStaff", "FROZEN_QUIT", id, event.getPlayer().getName(), id.toString(),
                    "Player disconnected while frozen", Map.of());
        }
        staffMode.remove(id);
        if (vanished.remove(id)) {
            for (Player viewer : Bukkit.getOnlinePlayers()) viewer.showPlayer(this, event.getPlayer());
        }
    }

    private void applyVisibilityFor(Player viewer) {
        for (UUID id : vanished) {
            Player hidden = Bukkit.getPlayer(id);
            if (hidden != null && !viewer.hasPermission("mirastaff.vanish.see")) viewer.hidePlayer(this, hidden);
        }
    }

    private void loadState() {
        getDataFolder().mkdirs();
        stateFile = new File(getDataFolder(), "state.yml");
        state = YamlConfiguration.loadConfiguration(stateFile);
        frozen.clear();
        for (String raw : state.getStringList("frozen")) {
            try { frozen.add(UUID.fromString(raw)); }
            catch (IllegalArgumentException ignored) { }
        }
    }

    private synchronized void saveState() {
        if (state == null || stateFile == null) return;
        state.set("frozen", frozen.stream().map(UUID::toString).sorted().toList());
        try { state.save(stateFile); }
        catch (IOException ex) { getLogger().severe("Could not save MiraStaff state.yml: " + ex.getMessage()); }
    }

    private void emit(Player player, StaffStateChangeEvent.State state, boolean enabled) {
        getServer().getPluginManager().callEvent(new StaffStateChangeEvent(player, state, enabled));
    }

    private void audit(CommandSender sender, String action, String target, Map<String, String> metadata) {
        UUID actor = sender instanceof Player player ? player.getUniqueId() : null;
        core.audit().record("MiraStaff", action, actor, sender.getName(), target, action, metadata);
    }

    private String yes(boolean value) { return value ? "&aYES" : "&cNO"; }
    private void msg(CommandSender sender, String raw) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', PREFIX + raw));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (args.length == 1 && Set.of("freeze", "inspect", "stafftp", "staffstatus").contains(name)) {
            return complete(args[0], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        return List.of();
    }

    private static List<String> complete(String prefix, Collection<String> values) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).distinct().sorted().toList();
    }

    public interface StaffApi {
        boolean isStaffMode(UUID player);
        boolean isVanished(UUID player);
        boolean isFrozen(UUID player);
        void freeze(UUID player, boolean frozen);
        Set<UUID> frozenPlayers();
    }

    private final class StaffApiImpl implements StaffApi {
        @Override public boolean isStaffMode(UUID player) { return staffMode.contains(player); }
        @Override public boolean isVanished(UUID player) { return vanished.contains(player); }
        @Override public boolean isFrozen(UUID player) { return frozen.contains(player); }
        @Override public void freeze(UUID player, boolean value) {
            if (value) frozen.add(player); else frozen.remove(player);
            saveState();
        }
        @Override public Set<UUID> frozenPlayers() { return Set.copyOf(frozen); }
    }
}
