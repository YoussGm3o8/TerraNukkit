/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package com.dfsek.terra.addon.loader;

import ca.solostudios.strata.Versions;
import ca.solostudios.strata.version.Version;
import ca.solostudios.strata.version.VersionRange;

import java.nio.file.Path;
import java.util.Collections;

import com.dfsek.terra.api.addon.BaseAddon;
import com.dfsek.terra.api.addon.bootstrap.BootstrapAddonClassLoader;
import com.dfsek.terra.api.addon.bootstrap.BootstrapBaseAddon;

/**
 * API Addon Loader for Terra.
 * This class provides addon loading functionality for the Terra API.
 */
public class ApiAddonLoader implements BootstrapBaseAddon<BaseAddon> {
    private static final Version VERSION = Versions.getVersion(0, 1, 0);

    @Override
    public String getID() {
        return "terra:api_addon_loader";
    }

    @Override
    public Version getVersion() {
        return VERSION;
    }

    @Override
    public Iterable<BaseAddon> loadAddons(Path addonsFolder, BootstrapAddonClassLoader parent) {
        // This is a minimal implementation for bootstrapping
        return Collections.emptyList();
    }
}
