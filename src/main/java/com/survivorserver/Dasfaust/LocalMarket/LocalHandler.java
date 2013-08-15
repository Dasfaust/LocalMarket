package com.survivorserver.Dasfaust.LocalMarket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.survivorserver.GlobalMarket.InterfaceViewer;
import com.survivorserver.GlobalMarket.Listing;
import com.survivorserver.GlobalMarket.LocaleHandler;
import com.survivorserver.GlobalMarket.Market;
import com.survivorserver.GlobalMarket.Interface.MarketInterface;

public class LocalHandler implements Listener {

	LocalMarket localMarket;
	Market market;
	LocaleHandler locale;
	FileConfiguration conf;
	Economy econ;
	Map<String, String> creating;
	
	public LocalHandler(LocalMarket localMarket) {
		this.localMarket = localMarket;
		creating = new HashMap<String, String>();
	}
	
	public void load(Market market, FileConfiguration conf) {
		this.market = market;
		this.conf = conf;
		locale = market.getLocale();
		econ = market.getEcon();
	}
	
	public String locToString(Location loc) {
		return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
	}
	
	public Location locFromString(String string) {
		String[] loc = string.split(",");
		return new Location(localMarket.getServer().getWorld(loc[0]), Double.parseDouble(loc[1]), Double.parseDouble(loc[2]), Double.parseDouble(loc[3]));
	}
	
	public String getChest(Chest chest) {
		String loc = null;
		if (chest.getInventory().getHolder() instanceof DoubleChest) {
			DoubleChest dChest = (DoubleChest) chest.getInventory().getHolder();
			String locLeft = locToString(((Chest) dChest.getLeftSide()).getLocation());
			String locRight = locToString(((Chest) dChest.getRightSide()).getLocation());
			if (conf.isSet("chests." + locLeft)) {
				loc = locLeft;
			} else if (conf.isSet("chests." + locRight)) {
				loc = locRight;	
			}
		} else {
			String loc1 = locToString(chest.getLocation());
			if (conf.isSet("chests." + loc1)) {
				loc = loc1;
			}
		}
		return loc;
	}
	
	public int getMaxChests() {
		return localMarket.getConfig().getInt("max_chests");
	}
	
	public int countChests(String name) {
		if (conf.isSet("chests")) {
			int c = 0;
			for (String l : conf.getConfigurationSection("chests").getKeys(false)) {
				if (conf.isSet("chests." + l + ".owner" + name)) {
					c++;
				}
			}
			return c;
		}
		return 0;
	}
	
	List<Listing> getListings(String loc) {
		List<Listing> listings = new ArrayList<Listing>();
		String path = "chests." + loc + ".listings";
		if (conf.isSet(path)) {
			for (String l : conf.getConfigurationSection(path).getKeys(false)) {
				String p = path + "." + l;
				Listing listing = new Listing(market, Integer.parseInt(l), conf.getItemStack(p + ".item").clone(), conf.getString(p + ".seller"), conf.getDouble(p + ".price"), conf.getLong(p + ".time"));
				listings.add(listing);
			}
			Collections.reverse(listings);
		}
		return listings;
	}
	
	public int getIndexFor(String loc) {
		String path = "chests." + loc + ".index";
		if (!conf.isSet(path)) {
			conf.set(path, 0);
		}
		return conf.getInt(path);
	}
	
	public void incrementIndex(String loc) {
		int index = getIndexFor(loc);
		String path = "chests." + loc + ".index";
		conf.set(path, index + 1);
	}
	
	public int getNumListings(String loc) {
		String path = "chests." + loc + ".listings";
		if (!conf.isSet(path)) {
			return 0;
		}
		return conf.getConfigurationSection(path).getKeys(false).size();
	}
	
	public Listing getListing(String loc, int id) {
		String path = "chests." + loc + ".listings." + id;
		return new Listing(market, id, conf.getItemStack(path + ".item").clone(), conf.getString(path + ".seller"), conf.getDouble(path + ".price"), conf.getLong(path + ".time"));
	}
	
	public void removeListing(String loc, int id) {
		String path = "chests." + loc + ".listings." + id;
		if (conf.isSet(path)) {
			conf.set(path, null);
		}
	}
	
	public void storeListing(String loc, ItemStack item, double price, String seller) {
		int id = getIndexFor(loc);
		incrementIndex(loc);
		String path = "chests." + loc + ".listings." + id;
		conf.set(path + ".item", item);
		conf.set(path + ".price", price);
		conf.set(path + ".seller", seller);
		conf.set(path + ".time", System.currentTimeMillis() / 1000);
	}
	
	public boolean useInventory() {
		return localMarket.getConfig().getBoolean("enable_inventory");
	}
	
	public void closeAllLocalViewers() {
		List<InterfaceViewer> toRemove = new ArrayList<InterfaceViewer>();
		for (InterfaceViewer viewer : market.getInterfaceHandler().getAllViewers()) {
			if (viewer instanceof LocalViewer) {
				toRemove.add(viewer);
			}
		}
		for (InterfaceViewer viewer : toRemove) {
			market.getInterfaceHandler().removeViewer(viewer);
			Player player = localMarket.getServer().getPlayer(viewer.getViewer());
			if (player != null) {
				player.closeInventory();
				player.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + locale.get("interface_closed_due_to_reload"));
			}
		}
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.PHYSICAL) && event.getClickedBlock().getType() == Material.CHEST) {
			Chest chest = (Chest) event.getClickedBlock().getState();
			String loc = getChest(chest);
			if (loc != null) {
				event.setCancelled(true);
				Player player = event.getPlayer();
				if (player.isSneaking()) {
					ItemStack inHand = player.getItemInHand();
					if (inHand != null && inHand.getType() != Material.AIR) {
						if (!localMarket.getConfig().getBoolean("enable_pages")) {
							if (getNumListings(loc) > chest.getInventory().getSize() - 9) {
								player.sendMessage(ChatColor.RED + locale.get("localmarket.too_many_listings"));
								return;
							}
						}
						if (market.itemBlacklisted(player.getItemInHand())) {
							player.sendMessage(ChatColor.RED + locale.get("item_is_blacklisted"));
							return;
						}
						player.sendMessage(ChatColor.GREEN + locale.get("localmarket.type_your_price"));
						if (!creating.containsKey(player.getName())) {
							creating.put(player.getName(), loc);
						}
						final String name = player.getName();
						new BukkitRunnable() {
							public void run() {
								if (creating.containsKey(name)) {
									creating.remove(name);
									Player p = localMarket.getServer().getPlayer(name);
									if (p != null) {
										p.sendMessage(ChatColor.GREEN + locale.get("localmarket.creation_cancelled"));
									}
								}
							}
						}.runTaskLater(localMarket, 200);
					} else {
						player.sendMessage(ChatColor.RED + locale.get("localmarket.hold_an_item"));
					}
				} else {
					Inventory inv = market.getServer().createInventory(event.getPlayer(), chest.getInventory().getSize(),
							(chest.getInventory().getTitle().equalsIgnoreCase("container.chest") || chest.getInventory().getTitle().equalsIgnoreCase("Large Chest")) ? market.getInterfaceHandler().getInterface("Local Listings").getTitle() : chest.getInventory().getTitle());
					InterfaceViewer viewer = new LocalViewer(event.getPlayer().getName(), inv, "Local Listings", loc);
					MarketInterface gui = market.getInterfaceHandler().getInterface("Local Listings");
					market.getInterfaceHandler().addViewer(viewer);
					market.getInterfaceHandler().refreshInterface(viewer, gui);
					market.getInterfaceHandler().openGui(viewer);
				}
			}
		}
	}
	
	@EventHandler
	public void onChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		if (creating.containsKey(player.getName())) {
			event.setCancelled(true);
			String response = event.getMessage();
			if (response.equalsIgnoreCase("cancel")) {
				creating.remove(player.getName());
				player.sendMessage(ChatColor.GREEN + locale.get("localmarket.creation_cancelled"));
			} else {
				String[] msg = response.split(" ");
				double price = 0;
				try {
					price = Double.parseDouble(msg[0]);
				} catch(NumberFormatException e) {
					player.sendMessage(ChatColor.RED + locale.get("not_a_valid_number", msg[0]));
					return;
				}
				if (price < 0) {
					player.sendMessage(locale.get("price_too_low"));
					return;
				}
				ItemStack toList = player.getItemInHand().clone();
				if (toList == null || toList.getType() == Material.AIR) {
					player.sendMessage(ChatColor.RED + locale.get("localmarket.hold_an_item"));
					return;
				}
				List<String> extraArgs = new ArrayList<String>();
				if (msg.length >= 2) {
					for (int i = 0; i < msg.length; i++) {
						if (msg[i].startsWith("-")) {
							extraArgs.add(msg[i]);
						}
					}
				}
				boolean infinite = false;
				if (player.hasPermission("globalmarket.infinite") && extraArgs.contains("-inf")) {
					infinite = true;
				}
				double fee = market.getCreationFee(player, price);
				if (fee > 0) {
					if (econ.has(player.getName(), fee)) {
						econ.withdrawPlayer(player.getName(), fee);
						market.getStorage().incrementSpent(player.getName(), fee);
						player.sendMessage(ChatColor.GREEN + locale.get("charged_fee", econ.format(fee)));
					} else {
						player.sendMessage(ChatColor.RED + locale.get("you_cant_pay_this_fee"));
						return;
					}
				}
				if (!infinite) {
					player.setItemInHand(new ItemStack(Material.AIR));
				}
				storeListing(creating.get(player.getName()), toList, price, infinite ? market.getInfiniteSeller() : player.getName());
				player.sendMessage(ChatColor.GREEN + locale.get("item_listed"));
				market.getStorage().storeHistory(player.getName(), locale.get("history.item_listed", market.getItemName(toList) + "x" + toList.getAmount(), price));
				creating.remove(player.getName());
			}
		}
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (block.getType() == Material.CHEST) {
			Chest chest = (Chest) block.getState();
			String loc = getChest(chest);
			if (loc != null) {
				if (event.getPlayer().hasPermission("globalmarket.admin")
						|| conf.getString("chests." + loc + ".owner").equalsIgnoreCase(event.getPlayer().getName())) {
					List<InterfaceViewer> toRemove = new ArrayList<InterfaceViewer>();
					for (InterfaceViewer viewer : market.getInterfaceHandler().getAllViewers()) {
						if (viewer instanceof LocalViewer) {
							if (((LocalViewer) viewer).getLoc().equalsIgnoreCase(loc)) {
								toRemove.add(viewer);
							}
						}
					}
					for (InterfaceViewer viewer : toRemove) {
						market.getInterfaceHandler().removeViewer(viewer);
						Player player = localMarket.getServer().getPlayer(viewer.getViewer());
						if (player != null) {
							player.closeInventory();
						}
					}
					for (Listing listing : getListings(loc)) {
						if (!listing.getSeller().equalsIgnoreCase(market.getInfiniteSeller())) {
							block.getWorld().dropItemNaturally(locFromString(loc), listing.getItem());
						}
					}
					event.setCancelled(false);
					event.getPlayer().sendMessage(ChatColor.GREEN + locale.get("localmarket.market_chest_removed"));
					conf.set("chests." + loc, null);
				} else {
					event.getPlayer().sendMessage(ChatColor.RED + locale.get("localmarket.you_cant_remove_this_chest"));
				}
			}
		}
	}
}
