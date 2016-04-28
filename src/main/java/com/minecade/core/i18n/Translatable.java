package com.minecade.core.i18n;

public interface Translatable {

    //public String getDisplayName();

    //public void setDisplayName(String displayName);

    //public void setName(String name);

	public String getTranslationKeyBase();

	public String getName();

	public void translate(Internationalizable plugin);

}
