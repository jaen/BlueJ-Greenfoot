/*
 This file is part of the BlueJ program.
 Copyright (C) 2025  Michael Kolling and John Rosenberg

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 This file is subject to the Classpath exception as provided in the
 LICENSE.txt file that accompanied this code.
 */
package bluej.parser.pratt.integration.benchmark;

import java.util.HashMap;
import java.util.Map;

/**
 * Strategy-specific configuration options for benchmarking.
 * 
 * Different parser integration strategies may have tunable parameters
 * that affect their performance characteristics. This class provides
 * a flexible way to configure these parameters for benchmarking.
 * 
 * Examples of strategy-specific configurations:
 * - Cache sizes for Lazy AST strategies
 * - Thread pool sizes for concurrent strategies
 * - Memory thresholds for garbage collection strategies
 * - Error recovery depth limits
 */
public class StrategyConfiguration {
    private final Map<String, Object> parameters;
    
    public StrategyConfiguration() {
        this.parameters = new HashMap<>();
    }
    
    public StrategyConfiguration(Map<String, Object> parameters) {
        this.parameters = new HashMap<>(parameters);
    }
    
    /**
     * Set a configuration parameter.
     */
    public StrategyConfiguration setParameter(String key, Object value) {
        parameters.put(key, value);
        return this;
    }
    
    /**
     * Get a configuration parameter.
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, T defaultValue) {
        Object value = parameters.get(key);
        if (value != null && defaultValue != null && 
            defaultValue.getClass().isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return defaultValue;
    }
    
    /**
     * Get a string parameter.
     */
    public String getString(String key, String defaultValue) {
        return getParameter(key, defaultValue);
    }
    
    /**
     * Get an integer parameter.
     */
    public int getInt(String key, int defaultValue) {
        return getParameter(key, defaultValue);
    }
    
    /**
     * Get a boolean parameter.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return getParameter(key, defaultValue);
    }
    
    /**
     * Get a double parameter.
     */
    public double getDouble(String key, double defaultValue) {
        return getParameter(key, defaultValue);
    }
    
    /**
     * Check if a parameter exists.
     */
    public boolean hasParameter(String key) {
        return parameters.containsKey(key);
    }
    
    /**
     * Get all parameter keys.
     */
    public java.util.Set<String> getParameterKeys() {
        return parameters.keySet();
    }
    
    /**
     * Create a copy of this configuration.
     */
    public StrategyConfiguration copy() {
        return new StrategyConfiguration(parameters);
    }
    
    /**
     * Common configuration presets for different strategy types.
     */
    public static class Presets {
        
        /**
         * Configuration optimized for memory efficiency.
         */
        public static StrategyConfiguration memoryOptimized() {
            return new StrategyConfiguration()
                    .setParameter("cacheSize", 100)
                    .setParameter("enableGcOptimization", true)
                    .setParameter("memoryThreshold", 50 * 1024 * 1024); // 50MB
        }
        
        /**
         * Configuration optimized for parsing speed.
         */
        public static StrategyConfiguration speedOptimized() {
            return new StrategyConfiguration()
                    .setParameter("cacheSize", 1000)
                    .setParameter("threadPoolSize", Runtime.getRuntime().availableProcessors())
                    .setParameter("enablePreallocation", true);
        }
        
        /**
         * Configuration for error recovery testing.
         */
        public static StrategyConfiguration errorRecoveryFocused() {
            return new StrategyConfiguration()
                    .setParameter("maxRecoveryDepth", 10)
                    .setParameter("enableDetailedErrorTracking", true)
                    .setParameter("errorRecoveryTimeout", 5000); // 5 seconds
        }
        
        /**
         * Default balanced configuration.
         */
        public static StrategyConfiguration defaultConfig() {
            return new StrategyConfiguration()
                    .setParameter("cacheSize", 500)
                    .setParameter("enableOptimizations", true)
                    .setParameter("maxMemoryMB", 100);
        }
    }
    
    @Override
    public String toString() {
        return "StrategyConfiguration{parameters=" + parameters + "}";
    }
}