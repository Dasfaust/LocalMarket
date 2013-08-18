package com.survivorserver.Dasfaust.LocalMarket;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.survivorserver.GlobalMarket.InterfaceViewer;
import com.survivorserver.GlobalMarket.Listing;
import com.survivorserver.GlobalMarket.ListingsInterface;
import com.survivorserver.GlobalMarket.Market;
import com.survivorserver.GlobalMarket.Interface.MarketItem;

public class LocalInterface extends ListingsInterface {

	LocalHandler handler;
	
	public LocalInterface(Market market, LocalHandler handler) {
		super(market);
		this.handler = handler;
	}

	@Override
	public String getName() {
		return "Local Listings";
	}
	
	@Override
	public boolean enableSearch() {
		return false;
	}
	
	@Override
	public ItemStack prepareItem(MarketItem marketItem, InterfaceViewer viewer, int page, int slot, boolean leftClick, boolean shiftClick) {
		Listing listing = (Listing) marketItem;
		ItemStack item = listing.getItem();
		ItemMeta meta = item.getItemMeta().clone();
		
		boolean isSeller = viewer.getViewer().equalsIgnoreCase(listing.getSeller());
		boolean isAdmin = market.getInterfaceHandler().isAdmin(viewer.getViewer());
		
		List<String> lore = meta.getLore();
		if (!meta.hasLore()) {
			lore = new ArrayList<String>();
		}
		String price = ChatColor.WHITE + market.getLocale().get("price") + market.getEcon().format(listing.getPrice());
		String seller = ChatColor.WHITE + market.getLocale().get("seller") + ChatColor.GRAY + ChatColor.ITALIC + listing.getSeller();
		lore.add(price);
		lore.add(seller);
		
		// Don't want people buying their own listings
		if (isSeller && leftClick) {
			viewer.resetActions();
		}
		
		// Or canceling listings they don't have permissions to
		if (!isSeller && shiftClick) {
			if (!isAdmin) {
				viewer.resetActions();
			}
		}
		
		if (!isSeller) {
			String buyMsg = ChatColor.YELLOW + market.getLocale().get("click_to_buy");
			if (leftClick) {
				if (market.getEcon().has(viewer.getViewer(), listing.getPrice())) {
					buyMsg = ChatColor.GREEN + market.getLocale().get("click_again_to_confirm");
				} else {
					buyMsg = ChatColor.RED + market.getLocale().get("not_enough_money", market.getEcon().currencyNamePlural());
					viewer.resetActions();
				}
			}
			lore.add(buyMsg);
		}
		
		if (isSeller || isAdmin) {
			String removeMsg = ChatColor.DARK_GRAY + market.getLocale().get("shift_click_to_remove");
			if (shiftClick) {
				removeMsg = ChatColor.GREEN + market.getLocale().get("shift_click_again_to_confirm");
			}
			lore.add(removeMsg);
		}
		
		if (listing.getSeller().equalsIgnoreCase(market.getInfiniteSeller())) {
			lore.add(ChatColor.LIGHT_PURPLE + market.getLocale().get("interface.infinite"));
		}
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}
	
	@Override
	public void handleLeftClickAction(InterfaceViewer viewer, MarketItem item, InventoryClickEvent event) {
		viewer.resetActions();
		if (handler.useInventory()) {
			market.getCore().buyListing((Listing) item, (Player) event.getWhoClicked(), false, false, false);
			event.getWhoClicked().getInventory().addItem(item.getItem());
		} else {
			market.getCore().buyListing((Listing) item, (Player) event.getWhoClicked(), false, true, false);
		}
		if (!((Listing) item).getSeller().equalsIgnoreCase(market.getInfiniteSeller())) {
			handler.removeListing(((LocalViewer) viewer).getLoc(), item.getId());
		}
		if (handler.enableSounds()) {
			((Player) event.getWhoClicked()).playSound(event.getWhoClicked().getLocation(), Sound.ORB_PICKUP, 0.8f, 1);
		}
		market.getInterfaceHandler().updateAllViewers();
	}

	@Override
	public void handleShiftClickAction(InterfaceViewer viewer, MarketItem item, InventoryClickEvent event) {
		Listing listing = (Listing) item;
		Player player = (Player) event.getWhoClicked();
		handler.removeListing(((LocalViewer) viewer).getLoc(), item.getId());
		if (!listing.getSeller().equalsIgnoreCase(market.getInfiniteSeller())) {
			String itemName = market.getItemName(listing.getItem());
			if (listing.getSeller().equalsIgnoreCase(player.getName())) {
				market.getStorage().storeHistory(player.getName(), market.getLocale().get("history.listing_removed", "You", itemName + "x" + listing.getItem().getAmount()));
			} else {
				market.getStorage().storeHistory(listing.getSeller(), market.getLocale().get("history.listing_removed", player, itemName + "x" + listing.getItem().getAmount()));
			}
			if (handler.useInventory()) {
				event.getWhoClicked().getInventory().addItem(item.getItem());
			} else {
				market.getStorage().storeMail(item.getItem(), ((Listing) item).getSeller(), null, true);
			}
		}
		if (handler.enableSounds()) {
			((Player) event.getWhoClicked()).playSound(event.getWhoClicked().getLocation(), Sound.ORB_PICKUP, 0.8f, 1);
		}
		viewer.resetActions();
		market.getInterfaceHandler().updateAllViewers();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<MarketItem> getContents(InterfaceViewer viewer) {
		return (List<MarketItem>)(List<?>) handler.getListings(((LocalViewer) viewer).getLoc());
	}

	@Override
	public MarketItem getItem(InterfaceViewer viewer, int id) {
		return handler.getListing(((LocalViewer) viewer).getLoc(), id);
	}
}
