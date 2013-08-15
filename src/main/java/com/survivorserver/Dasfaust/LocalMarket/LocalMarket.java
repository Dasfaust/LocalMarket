package com.survivorserver.Dasfaust.LocalMarket;

import java.util.logging.Logger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.survivorserver.GlobalMarket.Market;
import com.survivorserver.GlobalMarket.MetricsLite;

public class LocalMarket extends JavaPlugin implements Listener {

	Logger log;
	Market market;
	LocalHandler handler;
	
	public void onEnable() {
		log = getLogger();
		market = (Market) getServer().getPluginManager().getPlugin("GlobalMarket");
		handler = new LocalHandler(this);
		getServer().getPluginManager().registerEvents(this, this);
		getServer().getPluginManager().registerEvents(handler, this);
		reloadConfig();
		getConfig().addDefault("allow_pages", false);
		getConfig().addDefault("enable_inventory", true);
		getConfig().addDefault("enable_metrics", true);
		getConfig().addDefault("max_shops", 0);
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
		hook();
		market.getLocale().registerLocale("localmarket.cmd.chest_syntax", "/market chest");
		market.getLocale().registerLocale("localmarket.cmd.chest_descr", "[Creates a Market chest]");
		market.getLocale().registerLocale("localmarket.too_many_listings", "This chest has too many listings");
		market.getLocale().registerLocale("localmarket.type_your_price", "Type your price into chat, type cancel to stop");
		market.getLocale().registerLocale("localmarket.creation_cancelled", "Creation cancelled");
		market.getLocale().registerLocale("localmarket.hold_an_item", "Hold an item to list it");
		market.getLocale().registerLocale("localmarket.market_chest_removed", "Market chest removed");
		market.getLocale().registerLocale("localmarket.you_cant_remove_this_chest", "You can't remove this chest");
		market.getLocale().registerLocale("localmarket.empty_your_chest", "You need to empty your chest first");
		market.getLocale().registerLocale("localmarket.chest_created", "Market chest created");
		market.getLocale().registerLocale("localmarket.already_market_chest", "That chest is already a Market chest");
		market.getLocale().registerLocale("localmarket.aim_cursor_at_chest", "Please aim your cursor at a chest");
	}
	
	@EventHandler
	public void pluginLoad(PluginEnableEvent event) {
		if (event.getPlugin() instanceof Market) {
			market = (Market) event.getPlugin();
			hook();
			log.info("Re-hooked into GlobalMarket");
		}
	}
	
	public void hook() {
		try {
			market.getConfigHandler().loadCustomConfig("local_listings");
		} catch (Exception e) {
			log.info("Could not load local_listings.yml:");
			e.printStackTrace();
			log.info("Can't continue until this issue is resolved");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		market.getInterfaceHandler().registerInterface(new LocalInterface(market, handler));
		market.getCmd().registerSubCommand(new ChestCommand(market, market.getLocale(), handler));
		handler.load(market, market.getConfigHandler().getCustomConfig("local_listings"));
	}
}
