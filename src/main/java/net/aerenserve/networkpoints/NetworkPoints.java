package net.aerenserve.networkpoints;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import net.aerenserve.minesql.MineSQL;

import org.black_ixx.bossapi.BossAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class NetworkPoints extends JavaPlugin implements Listener {

	static MineSQL minesql;
	static String VERSION = "1.6";
	private BossAPI bossapi;

	@Override
	public void onEnable() {

		saveDefaultConfig();

		getServer().getPluginManager().registerEvents(this, this);

		String host = getConfig().getString("database.ip");
		String port = getConfig().getString("database.port");
		String database = getConfig().getString("database.dbname");
		String user = getConfig().getString("database.user");
		String pass = getConfig().getString("database.pass");

		minesql = new MineSQL(host, port, database, user, pass);

		try {
			minesql.updateSQL("CREATE TABLE IF NOT EXISTS `playerpoints` (id int PRIMARY KEY AUTO_INCREMENT, username text, balance int);");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		Plugin plugin = getServer().getPluginManager().getPlugin("BossAPI");
		if(plugin == null) {
			getLogger().info("Could not find BossAPI! Disabling support for BossAPI / BossShop");
		} else {
			bossapi = (BossAPI) plugin;
			bossapi.getPointsPluginManager().addInterface(new BossInterface(this));
		}

		getLogger().info("NetworkPoints v" + VERSION + " by hatten33 enabled");
	}

	@Override
	public void onDisable() {
		getLogger().info("NetworkPoints v" + VERSION + " by hatten33 disabled");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(cmd.getName().equalsIgnoreCase("points")) {
			if(args.length >= 1) {
				if(args[0].equalsIgnoreCase("get")) {
					UUID account = null;
					if(sender instanceof Player) account = ((Player) sender).getUniqueId();
					if(args.length > 1 && Bukkit.getPlayer(args[1]) != null) account = Bukkit.getPlayer(args[1]).getUniqueId();
					if(account != null) {
						sender.sendMessage(ChatColor.GRAY + "You currently have " + ChatColor.GREEN + getBalance(account) + ChatColor.GRAY + " points.");
					} else sender.sendMessage("You must be a player to see your balance");
				}
				if(args[0].equalsIgnoreCase("add")) {
					if(args.length >= 3) {
						UUID account;
						if(Bukkit.getPlayer(args[1]) != null) {
							account = Bukkit.getPlayer(args[1]).getUniqueId();
							Integer amount = Integer.parseInt(args[2]);
							addPoints(account, amount);
						} else sender.sendMessage(ChatColor.RED + "Cannot find that player!");
					} else sender.sendMessage(ChatColor.RED + "Usage: /points add (player) (amount)");
				}
				if(args[0].equalsIgnoreCase("subtract")) {
					if(args.length >= 3) {
						UUID account;
						if(Bukkit.getOfflinePlayer(args[1]) != null) {
							account = Bukkit.getOfflinePlayer(args[1]).getUniqueId();
							Integer amount = Integer.parseInt(args[2]);
							removePoints(account, amount);
						}
					} else sender.sendMessage(ChatColor.RED + "Usage: /points subtract (player) (amount)");
				}
				if(args[0].equalsIgnoreCase("set")) {
					if(args.length >= 3) {
						UUID account;
						if(Bukkit.getOfflinePlayer(args[1]) != null) {
							account = Bukkit.getOfflinePlayer(args[1]).getUniqueId();
							Integer amount = Integer.parseInt(args[2]);
							setBalance(account, amount);
						} else sender.sendMessage(ChatColor.RED + "Cannot find that player!");
					} else sender.sendMessage(ChatColor.RED + "Usage: /points set (player) (amount)");
				}

			} else sender.sendMessage(ChatColor.RED + "Usage: /points (get | add | subtract | set)");
		}
		return false;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		createPlayer(e.getPlayer().getUniqueId());
	}

	public static void createPlayer(UUID player) {
		if(!playerExists(player)) {
			try {
				minesql.updateSQL("INSERT INTO playerpoints (username, balance) VALUES ('" + player + "',0);");
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
	}

	public static boolean playerExists(UUID player) {
		try {
			ResultSet res = minesql.querySQL("SELECT * FROM playerpoints WHERE username = '" + player.toString() + "';");
			if(res.next()) {
				if(res.getString("username") == null) {
					return false;
				} else {
					return true;
				}
			} else {
				return false;
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		return false;
	}

	public static boolean checkTransaction(UUID player, Integer amount) {
		if(getBalance(player) >= amount) return true;
		else return false;
	}

	public static Integer getBalance(UUID player) {
		Integer retval = null;
		if(playerExists(player)) {
			try {
				ResultSet res = minesql.querySQL("SELECT * FROM playerpoints WHERE username = '" + player.toString() + "';");
				if(res.next()) {
					if((Integer) res.getInt("balance") != null) {
						retval = res.getInt("balance");
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			createPlayer(player);
			getBalance(player);
		}
		return retval;

	}

	public static void addPoints(UUID player, Integer amount) {
		if(playerExists(player)) {
			setBalance(player, (getBalance(player) + amount));
		} else {
			createPlayer(player);
			addPoints(player, amount);  //This might be bad? time will tell
		}
	}

	public static void setBalance(UUID player, Integer amount) {
		if(playerExists(player)) {
			try {
				minesql.updateSQL("UPDATE playerpoints SET balance=" + amount + " WHERE username='" + player.toString() + "';");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			createPlayer(player);
			setBalance(player, amount);
		}
	}

	public static void removePoints(UUID player, Integer amount) { //TODO add special exceptions
		if(playerExists(player)) {
			if(checkTransaction(player, amount)) {
				setBalance(player, (getBalance(player) - amount));
			}
		} else {
			createPlayer(player);
		}
	}
}
