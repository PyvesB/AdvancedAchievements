package com.hm.achievement.lang;

public enum InfoLang implements Lang {
	DESCRIPTION("Description:"), 
	DESCRIPTION_DETAILS("Advanced Achievements enables unique and challenging achievements. " +
			"Try to collect as many as you can, earn rewards, climb the rankings and receive RP books!"), 
	VERSION("Version:"), 
	AUTHOR("Author:"), 
	WEBSITE("Website:"), 
	VAULT("Vault integration:"), 
	PETMASTER("Pet Master integration:"), 
	BTLP("BungeeTabListPlus integration:"), 
	ESSENTIALS("Essentials integration:"), 
	PLACEHOLDERAPI("PlaceholderAPI integration:"), 
	DATABASE("Database type:");

	private final String defaultMessage;

	InfoLang(String defaultMessage) {
		this.defaultMessage = defaultMessage;
	}


	@Override
	public String getPath() {
		return "version-command-" + Lang.toPath(name());
	}

	@Override
	public String getDefaultMessage() {
		return defaultMessage;
	}
}
