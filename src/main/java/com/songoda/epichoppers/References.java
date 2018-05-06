package com.songoda.epichoppers;

public class References {

    private String prefix;

    public References() {
        prefix = EpicHoppers.getInstance().getLocale().getMessage("general.nametag.prefix") + " ";
    }

    public String getPrefix() {
        return this.prefix;
    }
}
