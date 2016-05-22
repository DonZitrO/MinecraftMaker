package com.minecade.core.i18n;

public interface Translatable {

	public String getDisplayName();

	public void setDisplayName(String displayName);

	public String getTranslationKeyBase();

	public String getName();

	default void translate(Internationalizable plugin) {
		String translationKey = String.format("%s.%s.display-name", getTranslationKeyBase().toLowerCase(), getName().toLowerCase().replace('_', '-'));
		String displayName = plugin.getMessage(translationKey);
		if (displayName != translationKey) {
			setDisplayName(displayName);
		}
	}

}
