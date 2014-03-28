package net.aerenserve.networkpoints;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.aerenserve.minesql.MineSQL;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class NetworkPoints extends JavaPlugin implements Listener {

	static MineSQL minesql;
	static String VERSION = "1.5";

	@Override
	public void onEnable() {

		saveDefaultConfig();

		getServer().getPluginManager().registerEvents(this, this);

		String host = getConfig().getString("database.ip");
		String port = getConfig().getString("database.port");
		String database = getConfig().getString("database.dbname");
		String user = getConfig().getString("database.user");
		String pass = getConfig().getString("database.pass");

		minesql = new MineSQL(this, host, port, database, user, pass);

		try {
			minesql.updateSQL("CREATE TABLE IF NOT EXISTS `playerpoints` (id int PRIMARY KEY AUTO_INCREMENT, username text, balance int);");
		} catch (SQLException e) {
			e.printStackTrace();
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
					String account = null;
					if(sender instanceof Player) account = sender.getName();
					if(args.length > 1 && Bukkit.getPlayer(args[1]) != null) account = args[1];
					if(account != null) {
						sender.sendMessage(ChatColor.GRAY + "You currently have " + ChatColor.GREEN + getBalance(account) + ChatColor.GRAY + " points.");
					} else sender.sendMessage("You must be a player to see your balance");
				}
				if(args[0].equalsIgnoreCase("add")) {
					if(args.length >= 3) {
						String account = args[1];
						Integer amount = Integer.parseInt(args[2]);
						addPoints(account, amount);
					} else sender.sendMessage(ChatColor.RED + "Usage: /points add (player) (amount)");
				}
				if(args[0].equalsIgnoreCase("subtract")) {
					if(args.length >= 3) {
						String account = args[1];
						Integer amount = Integer.parseInt(args[2]);
						removePoints(account, amount);
					} else sender.sendMessage(ChatColor.RED + "Usage: /points subtract (player) (amount)");
				}
				if(args[0].equalsIgnoreCase("set")) {
					if(args.length >= 3) {
						String account = args[1];
						Integer amount = Integer.parseInt(args[2]);
						setBalance(account, amount);
					} else sender.sendMessage(ChatColor.RED + "Usage: /points set (player) (amount)");
				}

			} else sender.sendMessage(ChatColor.RED + "Usage: /points (get | add | subtract | set)");
		}
		return false;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		createPlayer(e.getPlayer().getName());
	}

	public static void createPlayer(String playername) {
		if(!playerExists(playername)) {
			try {
				minesql.updateSQL("INSERT INTO playerpoints (username, balance) VALUES ('" + playername + "',0);");
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
	}

	public static boolean playerExists(String playername) {
		try {
			ResultSet res = minesql.querySQL("SELECT * FROM playerpoints WHERE username = '" + playername + "';");
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

	public static boolean checkTransaction(String playername, Integer amount) {
		if(getBalance(playername) >= amount) return true;
		else return false;
	}

	public static Integer getBalance(String playername) {
		Integer retval = null;
		if(playerExists(playername)) {
			try {
				ResultSet res = minesql.querySQL("SELECT * FROM playerpoints WHERE username = '" + playername + "';");
				if(res.next()) {
					if((Integer) res.getInt("balance") != null) {
						retval = res.getInt("balance");
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			createPlayer(playername);
			getBalance(playername);
		}
		return retval;

	}

	public static void addPoints(String playername, Integer amount) {
		if(playerExists(playername)) {
			setBalance(playername, (getBalance(playername) + amount));
		} else {
			createPlayer(playername);
			addPoints(playername, amount);  //This might be bad? time will tell
		}
	}

	public static void setBalance(String playername, Integer amount) {
		if(playerExists(playername)) {
			try {
				minesql.updateSQL("UPDATE playerpoints SET balance=" + amount + " WHERE username='" + playername + "';");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			createPlayer(playername);
			setBalance(playername, amount);
		}
	}

	public static void removePoints(String playername, Integer amount) { //TODO add special exceptions
		if(playerExists(playername)) {
			if(checkTransaction(playername, amount)) {
				setBalance(playername, (getBalance(playername) - amount));
			}
		} else {
			createPlayer(playername);
		}
	}
}
