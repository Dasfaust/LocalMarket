package com.survivorserver.Dasfaust.LocalMarket;

import com.survivorserver.GlobalMarket.LocaleHandler;

public class MarketLocaleHandler {

	LocaleHandler locale;
	
	public MarketLocaleHandler(LocaleHandler locale) {
		this.locale = locale;
		registerLocale();
	}
	
	public void registerLocale() {
		locale.registerLocale("localmarket.market_chest_removed", "Market chest removed");
		locale.registerLocale("localmarket.please_empty_your_chest", "Please emtpy your chest first");
		locale.registerLocale("localmarket.you_dont_own_this_chest", "You don't own this chest");
		locale.registerLocale("localmarket.this_chest_is_full", "This chest is full");
		locale.registerLocale("localmarket.not_a_double_chest", "You can only create Market chests with double chests");
		locale.registerLocale("localmarket.aim_cursor_at_chest", "Please aim at a chest!");
	}
}
