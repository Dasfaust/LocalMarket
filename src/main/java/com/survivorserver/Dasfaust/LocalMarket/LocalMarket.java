package com.survivorserver.Dasfaust.LocalMarket;

import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

import com.survivorserver.GlobalMarket.Market;
import com.survivorserver.GlobalMarket.MetricsLite;

public class LocalMarket extends JavaPlugin {

	Logger log;
	Market market;
	LocalMarketListener listener;
	
	public void onEnable() {
		log = getLogger();
		market = Market.getMarket();
		try {
			market.getConfigHandler().loadCustomConfig("local_listings");
		} catch (Exception e) {
			log.info("Could not load local_listings.yml:");
			e.printStackTrace();
			return;
		}
		listener = new LocalMarketListener(this, market, market.getConfigHandler().getCustomConfig("local_listings"));
		new MarketLocaleHandler(market.getLocale());
		getServer().getPluginManager().registerEvents(listener, this);
		reloadConfig();
		getConfig().addDefault("force_local", false);
		getConfig().addDefault("max_listings", 45);
		getConfig().addDefault("enable_metrics", true);
		getConfig().options().copyDefaults(true);
		saveConfig();
		if (getConfig().getBoolean("enable_metrics")) {
			try {
			    MetricsLite metrics = new MetricsLite(this);
			    metrics.start();
			} catch (Exception e) {
			    log.info("Failed to start Metrics!");
			}
		}
	}
	
	public void onDisable() { }
}
