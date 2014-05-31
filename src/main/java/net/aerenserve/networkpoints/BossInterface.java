package net.aerenserve.networkpoints;

import org.black_ixx.bossapi.pointplugins.BAPointsPluginInterface;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class BossInterface implements BAPointsPluginInterface {
	
	private NetworkPoints plugin;
	
	public BossInterface(NetworkPoints plugin) {
		this.plugin = plugin;
	}

	@Override
	public Plugin getPointPlugin() {
		return this.plugin;
	}

	@Override
	public int getPoints(String playername) {
		return NetworkPoints.getBalance(Bukkit.getOfflinePlayer(playername).getUniqueId());
	}

	@Override
	public boolean givePoints(String playername, int amount) {
		NetworkPoints.addPoints(Bukkit.getOfflinePlayer(playername).getUniqueId(), amount);
		return true;
	}

	@Override
	public boolean setPoints(String playername, int amount) {
		NetworkPoints.setBalance(Bukkit.getOfflinePlayer(playername).getUniqueId(), amount);
		return true;
	}

	@Override
	public boolean takePoints(String playername, int amount) {
		if(NetworkPoints.checkTransaction(Bukkit.getOfflinePlayer(playername).getUniqueId(), amount)) {
			NetworkPoints.removePoints(Bukkit.getOfflinePlayer(playername).getUniqueId(), amount);
			return true;
		}
		return false;
	}

}
