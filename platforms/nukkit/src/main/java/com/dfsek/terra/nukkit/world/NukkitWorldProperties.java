/*
 * This file is part of Terra.
 *
 * Terra is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Terra is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Terra.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.dfsek.terra.nukkit.world;

import cn.nukkit.level.Level;
import cn.nukkit.level.GameRule;
import org.jetbrains.annotations.NotNull;

import com.dfsek.terra.api.world.info.WorldProperties;

/**
 * Implementation of Terra's WorldProperties for Nukkit.
 */
public class NukkitWorldProperties implements WorldProperties {
    private final Level level;

    public NukkitWorldProperties(Level level) {
        this.level = level;
    }

    @Override
    public long getSeed() {
        return level.getSeed();
    }

    @Override
    public int getMaxHeight() {
        return 256; // Standard Minecraft height
    }

    @Override
    public int getMinHeight() {
        return 0; // Nukkit worlds typically start at Y=0
    }

    @Override
    public Object getHandle() {
        return level;
    }

    /**
     * Check if mobs spawn in this world.
     * 
     * @return Whether mobs spawn
     */
    public boolean doMobsSpawn() {
        return level.getGameRules().getBoolean(GameRule.DO_MOB_SPAWNING);
    }

    /**
     * Set whether mobs spawn in this world.
     * 
     * @param spawn Whether mobs should spawn
     */
    public void setMobsSpawn(boolean spawn) {
        level.getGameRules().setGameRule(GameRule.DO_MOB_SPAWNING, spawn);
    }

    /**
     * Get the current time in this world.
     * 
     * @return World time
     */
    public int getTime() {
        return (int) level.getTime();
    }

    /**
     * Set the time in this world.
     * 
     * @param time New time
     */
    public void setTime(int time) {
        level.setTime(time);
    }

    /**
     * Get the current weather in this world.
     * 
     * @return Current weather
     */
    public @NotNull Weather getWeather() {
        if(level.isRaining()) {
            return level.isThundering() ? Weather.THUNDER : Weather.RAIN;
        }
        return Weather.CLEAR;
    }

    /**
     * Set the weather in this world.
     * 
     * @param weather New weather
     */
    public void setWeather(@NotNull Weather weather) {
        switch(weather) {
            case CLEAR:
                level.setRaining(false);
                level.setThundering(false);
                break;
            case RAIN:
                level.setRaining(true);
                level.setThundering(false);
                break;
            case THUNDER:
                level.setRaining(true);
                level.setThundering(true);
                break;
        }
    }
} 