package com.songoda.epichoppers.api;


import org.bukkit.ChatColor;

/**
 * The access point of the EpicHoppersAPI, a class acting as a bridge between API
 * and plugin implementation. It is from here where developers should access the
 * important and core methods in the API. All static methods in this class will
 * call directly upon the implementation at hand (in most cases this will be the
 * EpicHoppers plugin itself), therefore a call to {@link #getImplementation()} is
 * not required and redundant in most situations. Method calls from this class are
 * preferred the majority of time, though an instance of {@link EpicHoppers} may
 * be passed if absolutely necessary.
 *
 * @see EpicHoppers
 * @since 3.0.0
 */
public class EpicHoppersAPI {

    private static EpicHoppers implementation;

    /**
     * Set the EpicHoppers implementation. Once called for the first time, this
     * method will throw an exception on any subsequent invocations. The implementation
     * may only be set a single time, presumably by the EpicHoppers plugin
     *
     * @param implementation the implementation to set
     */
    public static void setImplementation(EpicHoppers implementation) {
        if (EpicHoppersAPI.implementation != null) {
            throw new IllegalArgumentException("Cannot set API implementation twice");
        }

        EpicHoppersAPI.implementation = implementation;
    }

    /**
     * Get the EpicHoppers implementation. This method may be redundant in most
     * situations as all methods present in {@link EpicHoppers} will be mirrored
     * with static modifiers in the {@link EpicHoppersAPI} class
     *
     * @return the EpicHoppers implementation
     */
    public static EpicHoppers getImplementation() {
        return implementation;
    }
}
