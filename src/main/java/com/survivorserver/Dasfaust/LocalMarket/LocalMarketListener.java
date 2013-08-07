package com.survivorserver.Dasfaust.LocalMarket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.survivorserver.GlobalMarket.InterfaceViewer;
import com.survivorserver.GlobalMarket.Listing;
import com.survivorserver.GlobalMarket.LocaleHandler;
import com.survivorserver.GlobalMarket.Market;
import com.survivorserver.GlobalMarket.Events.InterfaceCreateEvent;
import com.survivorserver.GlobalMarket.Events.ListingClickEvent;
import com.survivorserver.GlobalMarket.Events.ListingCreateEvent;
import com.survivorserver.GlobalMarket.Events.ListingRemoveEvent;
import com.survivorserver.GlobalMarket.Events.ViewerRemoveEvent;

public class LocalMarketListener implements Listener {

	LocalMarket localMarket;
	Market market;
	FileConfiguration conf;
	LocaleHandler locale;
	Map<String, String> opened;
	
	public LocalMarketListener(LocalMarket localMarket, Market market, FileConfiguration conf) {
		this.localMarket = localMarket;
		this.market = market;
		this.conf = conf;
		locale = market.getLocale();
		opened = new HashMap<String, String>();
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
	
	public List<Listing> getAllListings(String loc) {
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
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (block.getType() == Material.CHEST) {
			Chest chest = (Chest) block.getState();
			if (chest.getInventory().getHolder() instanceof DoubleChest) {
				DoubleChest dChest = (DoubleChest) chest.getInventory().getHolder();
				String locLeft = locToString(((Chest) dChest.getLeftSide()).getLocation());
				String locRight = locToString(((Chest) dChest.getRightSide()).getLocation());
				String loc = null;
				if (conf.isSet("chests." + locLeft)) {
					event.setCancelled(true);
					loc = locLeft;
				} else if (conf.isSet("chests." + locRight)) {
					event.setCancelled(true);
					loc = locRight;
				}
				if (loc != null) {
					if (event.getPlayer().hasPermission("globalmarket.admin")
							|| conf.getString("chests." + loc + ".owner").equalsIgnoreCase(event.getPlayer().getName())) {
						for (Entry<String, String> entry : opened.entrySet()) {
							if (entry.getValue().equalsIgnoreCase(loc)) {
								localMarket.getServer().getPlayer(entry.getKey()).closeInventory();
								market.getInterfaceHandler().removeViewer(market.getInterfaceHandler().findViewer(entry.getValue()));
							}
						}
						for (Listing listing : getAllListings(loc)) {
							if (!listing.getSeller().equalsIgnoreCase(market.getInfiniteSeller())) {
								block.getWorld().dropItemNaturally(locFromString(loc), listing.getItem());
							}
						}
						event.setCancelled(false);
						event.getPlayer().sendMessage(ChatColor.GREEN + locale.get("localmarket.market_chest_removed"));
						conf.set("chests." + loc, null);
					}
				}
			}
		}
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		Block block = event.getClickedBlock();
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && block.getType() == Material.CHEST && event.getPlayer().hasPermission("globalmarket.admin")) {
			Chest chest = (Chest) block.getState();
			String loc = locToString(block.getLocation());
			if (chest.getInventory().getHolder() instanceof DoubleChest) {
				DoubleChest dChest = (DoubleChest) chest.getInventory().getHolder();
				String locLeft = locToString(((Chest) dChest.getLeftSide()).getLocation());
				String locRight = locToString(((Chest) dChest.getRightSide()).getLocation());	
				if (conf.isSet("chests." + locLeft)) {
					loc = locLeft;
				} else if (conf.isSet("chests." + locRight)) {
					loc = locRight;
				}
			}
			if (conf.isSet("chests." + loc)) {
				event.setCancelled(true);
				opened.put(event.getPlayer().getName(), loc);
				market.getInterfaceHandler().showListings(event.getPlayer(), null);
			}
		}
	}
	
	@EventHandler
	public void onInterfaceCreate(InterfaceCreateEvent event) {
		InterfaceViewer viewer = event.getViewer();
		if (opened.containsKey(viewer.getViewer())) {
			List<Listing> listings = event.getListings();
			listings.clear();
			String loc = opened.get(viewer.getViewer());
			listings.addAll(getAllListings(loc));
			Chest chest = (Chest) locFromString(loc).getBlock().getState();
			Inventory chestInv = chest.getInventory();
			if ((!chestInv.getName().equalsIgnoreCase("Chest") && !chestInv.getName().equalsIgnoreCase("Large Chest")) && !viewer.getGui().getName().equalsIgnoreCase(chestInv.getName())) {
				viewer.setGui(localMarket.getServer().createInventory(chestInv.getHolder(), chestInv.getSize(), chestInv.getName()));
			}
		}
	}
	
	@EventHandler
	public void onListingCreate(ListingCreateEvent event) {
		if (localMarket.getConfig().getBoolean("force_local") || (event.getArgs().contains("-local") && event.getSeller().hasPermission("localmarket.create"))) {
			event.setCancelled(true);
			Player player = event.getSeller();
			Block block = player.getTargetBlock(null, 4);
			if (block.getType() == Material.CHEST) {
				Chest chest = (Chest) block.getState();
				Inventory inv = chest.getInventory();
				String loc = locToString(block.getLocation());
				ItemStack item = new ItemStack(event.getItem());
				
				if (chest.getInventory().getHolder() instanceof DoubleChest) {
					DoubleChest dChest = (DoubleChest) chest.getInventory().getHolder();
					String locLeft = locToString(((Chest) dChest.getLeftSide()).getLocation());
					String locRight = locToString(((Chest) dChest.getRightSide()).getLocation());
					if (conf.isSet("chests." + locLeft)) {
						loc = locLeft;
					} else if (conf.isSet("chests." + locRight)) {
						loc = locRight;	
					}
				}
				if (!conf.isSet("chests." + loc)) {
					boolean empty = true;
					ItemStack[] contents = inv.getContents();
					for (int i = 0; i < contents.length; i++) {
						if (contents[i] != null) {
							empty = false;
							break;
						}
					}
					if (!empty) {
						player.sendMessage(ChatColor.RED + locale.get("localmarket.please_empty_your_chest"));
						return;
					}
					conf.set("chests." + loc + ".owner", event.getSeller().getName());
				} else {
					if (!conf.getString("chests." + loc + ".owner").equalsIgnoreCase(event.getSeller().getName())) {
						player.sendMessage(ChatColor.RED + locale.get("localmarket.you_dont_own_this_chest"));
						return;
					}
				}
				if (getNumListings(loc) > localMarket.getConfig().getInt("max_listings")) {
					player.sendMessage(ChatColor.RED + locale.get("localmarket.this_chest_is_full"));
					return;
				}
				boolean inf = false;
				if (event.getArgs().contains("-inf") && event.getSeller().hasPermission("globalmarket.infinite")) {
					inf = true;
				}
				if (!inf) {
					if (event.getAmount() < item.getAmount()) {
						player.getItemInHand().setAmount(player.getItemInHand().getAmount() - event.getAmount());
					} else {
						player.setItemInHand(new ItemStack(Material.AIR));
					}
				}
				int id = getIndexFor(loc);
				incrementIndex(loc);
				String path = "chests." + loc + ".listings." + id;
				conf.set(path + ".item", event.getItem());
				conf.set(path + ".price", event.getPrice());
				if (inf) {
					conf.set(path + ".seller", market.getInfiniteSeller());
				} else {
					conf.set(path + ".seller", event.getSeller().getName());
				}
				conf.set(path + ".time", System.currentTimeMillis() / 1000);
				player.sendMessage(ChatColor.GREEN + locale.get("item_listed"));
				
			} else {
				player.sendMessage(ChatColor.RED + locale.get("localmarket.aim_cursor_at_chest"));
			}
		}
	}
	
	@EventHandler
	public void onListingClick(ListingClickEvent event) {
		String player = event.getViewer().getViewer();
		if (opened.containsKey(player)) {
			String loc = opened.get(player);
			event.setListing(getListing(loc, event.getViewer().getBoundSlots().get(event.getClickEvent().getSlot())));
		}
	}
	
	@EventHandler
	public void onViewerRemove(ViewerRemoveEvent event) {
		if (opened.containsKey(event.getViewerName())) {
			opened.remove(event.getViewerName());
		}
	}
	
	
	@EventHandler
	public void listingRemove(ListingRemoveEvent event) {
		if (opened.containsKey(event.getViewerName())) {
			event.setCancelled(true);
			removeListing(opened.get(event.getViewerName()), event.getId());
		}
	}
	
	public String locToString(Location loc) {
		return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
	}
	
	public Location locFromString(String string) {
		String[] loc = string.split(",");
		return new Location(localMarket.getServer().getWorld(loc[0]), Double.parseDouble(loc[1]), Double.parseDouble(loc[2]), Double.parseDouble(loc[3]));
	}
}
