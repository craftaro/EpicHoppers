package com.craftaro.epichoppers;

import com.craftaro.epichoppers.boost.BoostManager;
import com.craftaro.epichoppers.containers.ContainerManager;
import com.craftaro.epichoppers.database.DataManager;
import com.craftaro.epichoppers.hopper.teleport.TeleportHandler;
import com.craftaro.epichoppers.player.PlayerDataManager;
import com.craftaro.epichoppers.hopper.levels.LevelManager;
import org.jetbrains.annotations.ApiStatus;

public class EpicHoppersApi {
    private static EpicHoppersApi instance;

    private final LevelManager levelManager;
    private final BoostManager boostManager;
    private final ContainerManager containerManager;
    private final TeleportHandler teleportHandler;
    private final PlayerDataManager playerDataManager;
    private final DataManager dataManager;

    private EpicHoppersApi(LevelManager levelManager,
                           BoostManager boostManager,
                           ContainerManager containerManager,
                           TeleportHandler teleportHandler,
                           PlayerDataManager playerDataManager,
                           DataManager dataManager) {
        this.levelManager = levelManager;
        this.boostManager = boostManager;
        this.containerManager = containerManager;
        this.teleportHandler = teleportHandler;
        this.playerDataManager = playerDataManager;
        this.dataManager = dataManager;
    }

    public LevelManager getLevelManager() {
        return this.levelManager;
    }

    public BoostManager getBoostManager() {
        return this.boostManager;
    }

    public ContainerManager getContainerManager() {
        return this.containerManager;
    }

    public TeleportHandler getTeleportHandler() {
        return this.teleportHandler;
    }

    public PlayerDataManager getPlayerDataManager() {
        return this.playerDataManager;
    }

    /**
     * TODO: The DataManager probably shouldn't be exposed to the API.
     */
    @ApiStatus.Internal
    public DataManager getDataManager() {
        return this.dataManager;
    }

    public static EpicHoppersApi getApi() {
        return instance;
    }

    static void initApi(LevelManager levelManager, BoostManager boostManager, ContainerManager containerManager, TeleportHandler teleportHandler, PlayerDataManager playerDataManager, DataManager dataManager) {
        if (instance != null) {
            throw new IllegalStateException(EpicHoppersApi.class.getSimpleName() + " already initialized");
        }
        instance = new EpicHoppersApi(levelManager, boostManager, containerManager, teleportHandler, playerDataManager, dataManager);
    }
}
