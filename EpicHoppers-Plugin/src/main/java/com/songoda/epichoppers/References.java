package com.songoda.epichoppers;

public class References {

    private String prefix;

    public References() {
        prefix = EpicHoppersPlugin.getInstance().getLocale().getMessage("general.nametag.prefix") + " ";
    }

    public String getPrefix() {
        return this.prefix;
    }
}
