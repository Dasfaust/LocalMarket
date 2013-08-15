package com.survivorserver.Dasfaust.LocalMarket;

import org.bukkit.inventory.Inventory;

import com.survivorserver.GlobalMarket.InterfaceViewer;

public class LocalViewer extends InterfaceViewer {

	String loc;
	
	public LocalViewer(String player, Inventory gui, String interfaceName, String loc) {
		super(player, gui, interfaceName);
		this.loc = loc;
	}

	public String getLoc() {
		return loc;
	}
	
	public void setLoc(String loc) {
		this.loc = loc;
	}
}
