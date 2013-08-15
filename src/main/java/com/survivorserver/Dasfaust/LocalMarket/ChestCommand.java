package com.survivorserver.Dasfaust.LocalMarket;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.survivorserver.GlobalMarket.LocaleHandler;
import com.survivorserver.GlobalMarket.Market;
import com.survivorserver.GlobalMarket.Command.SubCommand;

public class ChestCommand extends SubCommand {

	LocalHandler handler;
	
	public ChestCommand(Market market, LocaleHandler locale, LocalHandler handler) {
		super(market, locale);
		this.handler = handler;
	}

	@Override
	public String getCommand() {
		return "chest";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermissionNode() {
		return "localmarket.create";
	}

	@Override
	public String getHelp() {
		return locale.get("cmd.prefix") + locale.get("localmarket.cmd.chest_syntax") + " " + locale.get("localmarket.cmd.chest_descr");
	}

	@Override
	public boolean allowConsoleSender() {
		return false;
	}

	@Override
	public boolean onCommand(CommandSender sender, String[] args) {
		if (args.length == 1) {
			Player player = (Player) sender;
			Block block = player.getTargetBlock(null, 4);
			if (block.getType() == Material.CHEST) {
				Chest chest = (Chest) block.getState();
				String loc = handler.getChest(chest);
				if (loc == null) {
					int maxChests = handler.getMaxChests();
					if (maxChests > 0 && handler.countChests(player.getName()) >= maxChests) {
						player.sendMessage(ChatColor.RED + locale.get("localmarket.too_many_chests"));
						return true;
					}
					ItemStack[] contents = chest.getInventory().getContents();
					for (int i = 0; i < contents.length; i++) {
						if (contents[i] != null) {
							player.sendMessage(ChatColor.RED + locale.get("localmarket.empty_your_chest"));
							return true;
						}
					}
					handler.conf.set("chests." + handler.locToString(chest.getLocation()) + ".owner", sender.getName());
					player.sendMessage(locale.get("cmd.prefix") + locale.get("localmarket.chest_created"));
					return true;
				} else {
					sender.sendMessage(ChatColor.YELLOW + locale.get("localmarket.already_market_chest"));
					return true;
				}
			} else {
				player.sendMessage(ChatColor.RED + locale.get("localmarket.aim_cursor_at_chest"));
				return true;
			}
		}
		return false;
	}
}
