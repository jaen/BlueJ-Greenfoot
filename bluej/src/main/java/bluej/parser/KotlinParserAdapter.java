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
package bluej.parser;

import bluej.Config;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.pratt.KotlinPrattParser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * KotlinParserAdapter provides flexible parser selection for Kotlin code,
 * enabling switching between legacy and new Pratt parser implementations.
 * 
 * <p>This adapter implements the Strategy pattern to dynamically select
 * between different parser implementations based on configuration, feature
 * requirements, and performance characteristics.
 * 
 * <h2>Features:</h2>
 * <ul>
 *   <li>Configuration-based parser selection</li>
 *   <li>Automatic capability detection and fallback</li>
 *   <li>Performance monitoring with minimal overhead</li>
 *   <li>Thread-safe parser caching</li>
 *   <li>100% backward compatibility</li>
 * </ul>
 * 
 * <h2>Parser Selection Strategy:</h2>
 * <ol>
 *   <li>Check explicit configuration (kotlin.parser.type)</li>
 *   <li>Evaluate feature requirements</li>
 *   <li>Consider performance characteristics</li>
 *   <li>Apply fallback strategy if needed</li>
 * </ol>
 * 
 * <h2>Configuration:</h2>
 * <pre>
 * kotlin.parser.type = auto | legacy | pratt
 * kotlin.parser.performance.monitoring = true | false
 * kotlin.parser.performance.threshold = 100 (ms)
 * kotlin.parser.fallback.enabled = true | false
 * </pre>
 * 
 * @since BlueJ 5.4.0
 * @author BlueJ Development Team
 */
public class KotlinParserAdapter implements ParserBehavior {
    
    private static final Logger LOGGER = Logger.getLogger(KotlinParserAdapter.class.getName());
    
    /**
     * Parser type enumeration for configuration and selection.
     */
    public enum ParserType {
        /** Automatically select the best parser based on context */
        AUTO("auto"),
        /** Use the legacy KotlinParser implementation */
        LEGACY("legacy"),
        /** Use the new Pratt parser implementation */
        PRATT("pratt");
        
        private final String configValue;
        
        ParserType(String configValue) {
            this.configValue = configValue;
        }
        
        public String getConfigValue() {
            return configValue;
        }
        
        public static ParserType fromConfig(String value) {
            for (ParserType type : values()) {
                if (type.configValue.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return AUTO;
        }
    }
    
    /**
     * Parser capability flags for feature detection.
     */
    public enum ParserCapability {
        /** Support for Kotlin 1.9+ features */
        KOTLIN_1_9_FEATURES,
        /** Support for context receivers */
        CONTEXT_RECEIVERS,
        /** Support for value classes */
        VALUE_CLASSES,
        /** Support for sealed when exhaustiveness */
        SEALED_WHEN_EXHAUSTIVENESS,
        /** Support for definitely non-nullable types */
        DEFINITELY_NON_NULLABLE,
        /** Support for type inference improvements */
        ADVANCED_TYPE_INFERENCE,
        /** Support for inline classes */
        INLINE_CLASSES,
        /** Support for contracts */
        CONTRACTS,
        /** Support for coroutines */
        COROUTINES,
        /** Support for multiplatform */
        MULTIPLATFORM
    }
    
    /**
     * Performance metrics for monitoring parser behavior.
     *
     * <p>This class provides thread-safe performance tracking using lock-free
     * atomic operations and {@link LongAdder} for high-concurrency scenarios.
     * Metrics include parse counts, timing statistics, error rates, and fallback
     * occurrences.
     *
     * <h3>Thread Safety:</h3>
     * All operations in this class are thread-safe and optimized for
     * concurrent access with minimal contention.
     *
     * <h3>Usage Example:</h3>
     * <pre>
     * PerformanceMetrics metrics = adapter.getLegacyMetrics();
     * long avgTime = metrics.getAverageParseTimeNanos();
     * if (avgTime > threshold) {
     *     // Consider switching parsers
     * }
     * </pre>
     *
     * @since BlueJ 5.4.0
     */
    public static class PerformanceMetrics {
        private final LongAdder parseCount = new LongAdder();
        private final LongAdder totalParseTime = new LongAdder();
        private final LongAdder errorCount = new LongAdder();
        private final LongAdder fallbackCount = new LongAdder();
        private final AtomicLong maxParseTime = new AtomicLong(0);
        private final AtomicLong minParseTime = new AtomicLong(Long.MAX_VALUE);
        
        /**
         * Records a successful parse operation with its execution time.
         *
         * @param timeNanos The parse operation duration in nanoseconds
         */
        public void recordParse(long timeNanos) {
            parseCount.increment();
            totalParseTime.add(timeNanos);
            updateMaxMin(timeNanos);
        }
        
        /**
         * Records a parse error occurrence.
         * This increments the error counter for tracking parser reliability.
         */
        public void recordError() {
            errorCount.increment();
        }
        
        /**
         * Records a fallback event when the primary parser fails and
         * the system switches to an alternative parser.
         */
        public void recordFallback() {
            fallbackCount.increment();
        }
        
        /**
         * Updates the maximum and minimum parse times using lock-free
         * compare-and-set operations.
         *
         * @param timeNanos The parse time to compare against current extremes
         */
        private void updateMaxMin(long timeNanos) {
            long currentMax, currentMin;
            do {
                currentMax = maxParseTime.get();
            } while (timeNanos > currentMax && !maxParseTime.compareAndSet(currentMax, timeNanos));
            
            do {
                currentMin = minParseTime.get();
            } while (timeNanos < currentMin && !minParseTime.compareAndSet(currentMin, timeNanos));
        }
        
        /**
         * Gets the total number of parse operations recorded.
         *
         * @return The total parse count
         */
        public long getParseCount() {
            return parseCount.sum();
        }
        
        /**
         * Calculates the average parse time across all recorded operations.
         *
         * @return The average parse time in nanoseconds, or 0 if no parses recorded
         */
        public long getAverageParseTimeNanos() {
            long count = parseCount.sum();
            return count > 0 ? totalParseTime.sum() / count : 0;
        }
        
        /**
         * Gets the maximum parse time recorded.
         *
         * @return The maximum parse time in nanoseconds, or 0 if no parses recorded
         */
        public long getMaxParseTimeNanos() {
            return maxParseTime.get();
        }
        
        /**
         * Gets the minimum parse time recorded.
         *
         * @return The minimum parse time in nanoseconds, or 0 if no parses recorded
         */
        public long getMinParseTimeNanos() {
            long min = minParseTime.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }
        
        /**
         * Gets the total number of parse errors recorded.
         *
         * @return The total error count
         */
        public long getErrorCount() {
            return errorCount.sum();
        }
        
        /**
         * Gets the total number of fallback events recorded.
         *
         * @return The total fallback count
         */
        public long getFallbackCount() {
            return fallbackCount.sum();
        }
        
        /**
         * Resets all metrics to their initial state.
         * This method is useful for benchmarking or when switching
         * parser configurations.
         */
        public void reset() {
            parseCount.reset();
            totalParseTime.reset();
            errorCount.reset();
            fallbackCount.reset();
            maxParseTime.set(0);
            minParseTime.set(Long.MAX_VALUE);
        }
        
        /**
         * Returns a string representation of the adapter's current state.
         *
         * @return A string containing current parser type, configuration,
         *         and monitoring status
         */
        @Override
        public String toString() {
            return String.format(
                "PerformanceMetrics{parseCount=%d, avgTime=%dms, maxTime=%dms, minTime=%dms, errors=%d, fallbacks=%d}",
                getParseCount(),
                getAverageParseTimeNanos() / 1_000_000,
                getMaxParseTimeNanos() / 1_000_000,
                getMinParseTimeNanos() / 1_000_000,
                getErrorCount(),
                getFallbackCount()
            );
        }
    }
    
    // Instance fields
    private final SourceParser sourceParser;
    private final ParserType configuredType;
    private final boolean performanceMonitoringEnabled;
    private final boolean fallbackEnabled;
    private final long performanceThresholdNanos;
    
    // Parser instances (lazily initialized)
    private volatile ParserBehavior currentParser;
    private volatile ParserBehavior legacyParser;
    private volatile ParserBehavior prattParser;
    
    // Performance tracking
    private final PerformanceMetrics legacyMetrics = new PerformanceMetrics();
    private final PerformanceMetrics prattMetrics = new PerformanceMetrics();
    
    // Capability cache
    private final Map<ParserCapability, Boolean> legacyCapabilities = new ConcurrentHashMap<>();
    private final Map<ParserCapability, Boolean> prattCapabilities = new ConcurrentHashMap<>();
    
    /**
     * Creates a new KotlinParserAdapter with the specified source parser.
     *
     * <p>The adapter initializes with configuration from BlueJ properties:
     * <ul>
     *   <li>kotlin.parser.type - Parser selection strategy</li>
     *   <li>kotlin.parser.performance.monitoring - Enable metrics collection</li>
     *   <li>kotlin.parser.fallback.enabled - Enable automatic fallback</li>
     *   <li>kotlin.parser.performance.threshold - Performance threshold in ms</li>
     * </ul>
     *
     * @param sourceParser The source parser to use for token stream and callbacks
     * @throws NullPointerException if sourceParser is null
     */
    public KotlinParserAdapter(SourceParser sourceParser) {
        this.sourceParser = sourceParser;
        
        // Load configuration
        this.configuredType = loadParserType();
        this.performanceMonitoringEnabled = loadPerformanceMonitoring();
        this.fallbackEnabled = loadFallbackEnabled();
        this.performanceThresholdNanos = loadPerformanceThreshold();
        
        // Initialize capability maps
        initializeCapabilities();
        
        // Select initial parser
        this.currentParser = selectParser(null);
        
        LOGGER.log(Level.INFO, "KotlinParserAdapter initialized with type: {0}, monitoring: {1}, fallback: {2}",
                   new Object[]{configuredType, performanceMonitoringEnabled, fallbackEnabled});
    }
    
    /**
     * Loads the parser type from configuration.
     *
     * @return The configured parser type, defaults to AUTO if not specified
     */
    private ParserType loadParserType() {
        String type = Config.getPropString("kotlin.parser.type", "auto");
        return ParserType.fromConfig(type);
    }
    
    /**
     * Loads the performance monitoring setting from configuration.
     *
     * @return true if performance monitoring is enabled, false otherwise
     */
    private boolean loadPerformanceMonitoring() {
        return Config.getPropBoolean("kotlin.parser.performance.monitoring", false);
    }
    
    /**
     * Loads the fallback enabled setting from configuration.
     *
     * @return true if fallback is enabled (default), false otherwise
     */
    private boolean loadFallbackEnabled() {
        return Config.getPropBoolean("kotlin.parser.fallback.enabled", true);
    }
    
    /**
     * Loads the performance threshold from configuration.
     *
     * @return The performance threshold in nanoseconds
     */
    private long loadPerformanceThreshold() {
        int thresholdMs = Config.getPropInteger("kotlin.parser.performance.threshold", 100);
        return thresholdMs * 1_000_000L; // Convert to nanoseconds
    }
    
    /**
     * Initializes the capability maps for both parsers.
     */
    private void initializeCapabilities() {
        // Legacy parser capabilities
        legacyCapabilities.put(ParserCapability.KOTLIN_1_9_FEATURES, false);
        legacyCapabilities.put(ParserCapability.CONTEXT_RECEIVERS, false);
        legacyCapabilities.put(ParserCapability.VALUE_CLASSES, false);
        legacyCapabilities.put(ParserCapability.SEALED_WHEN_EXHAUSTIVENESS, true);
        legacyCapabilities.put(ParserCapability.DEFINITELY_NON_NULLABLE, false);
        legacyCapabilities.put(ParserCapability.ADVANCED_TYPE_INFERENCE, false);
        legacyCapabilities.put(ParserCapability.INLINE_CLASSES, true);
        legacyCapabilities.put(ParserCapability.CONTRACTS, false);
        legacyCapabilities.put(ParserCapability.COROUTINES, true);
        legacyCapabilities.put(ParserCapability.MULTIPLATFORM, false);
        
        // Pratt parser capabilities (more advanced)
        prattCapabilities.put(ParserCapability.KOTLIN_1_9_FEATURES, true);
        prattCapabilities.put(ParserCapability.CONTEXT_RECEIVERS, true);
        prattCapabilities.put(ParserCapability.VALUE_CLASSES, true);
        prattCapabilities.put(ParserCapability.SEALED_WHEN_EXHAUSTIVENESS, true);
        prattCapabilities.put(ParserCapability.DEFINITELY_NON_NULLABLE, true);
        prattCapabilities.put(ParserCapability.ADVANCED_TYPE_INFERENCE, true);
        prattCapabilities.put(ParserCapability.INLINE_CLASSES, true);
        prattCapabilities.put(ParserCapability.CONTRACTS, true);
        prattCapabilities.put(ParserCapability.COROUTINES, true);
        prattCapabilities.put(ParserCapability.MULTIPLATFORM, true);
    }
    
    /**
     * Selects the appropriate parser based on configuration and context.
     * 
     * @param requiredCapabilities Optional set of required capabilities
     * @return The selected parser instance
     */
    private ParserBehavior selectParser(Map<ParserCapability, Boolean> requiredCapabilities) {
        ParserType typeToUse = configuredType;
        
        if (configuredType == ParserType.AUTO) {
            typeToUse = determineOptimalParser(requiredCapabilities);
        }
        
        switch (typeToUse) {
            case PRATT:
                return getPrattParser();
            case LEGACY:
            default:
                return getLegacyParser();
        }
    }
    
    /**
     * Determines the optimal parser based on required capabilities and performance.
     *
     * <p>Selection algorithm:
     * <ol>
     *   <li>Check capability requirements</li>
     *   <li>If both meet requirements, use performance metrics</li>
     *   <li>Default to Pratt for new features</li>
     * </ol>
     *
     * @param requiredCapabilities Map of required parser capabilities
     * @return The optimal parser type for the given requirements
     */
    private ParserType determineOptimalParser(Map<ParserCapability, Boolean> requiredCapabilities) {
        if (requiredCapabilities != null && !requiredCapabilities.isEmpty()) {
            boolean legacyMeets = meetsCapabilities(legacyCapabilities, requiredCapabilities);
            boolean prattMeets = meetsCapabilities(prattCapabilities, requiredCapabilities);
            
            if (prattMeets && !legacyMeets) {
                return ParserType.PRATT;
            }
            if (legacyMeets && !prattMeets) {
                return ParserType.LEGACY;
            }
        }
        
        // If both meet requirements or no requirements, use performance metrics
        if (performanceMonitoringEnabled) {
            long legacyAvg = legacyMetrics.getAverageParseTimeNanos();
            long prattAvg = prattMetrics.getAverageParseTimeNanos();
            
            if (legacyMetrics.getParseCount() > 10 && prattMetrics.getParseCount() > 10) {
                // Both have sufficient samples
                if (prattAvg < legacyAvg * 0.8) { // Pratt is 20% faster
                    return ParserType.PRATT;
                }
                if (legacyAvg < prattAvg * 0.8) { // Legacy is 20% faster
                    return ParserType.LEGACY;
                }
            }
        }
        
        // Default to Pratt for new features
        return ParserType.PRATT;
    }
    
    /**
     * Checks if a parser meets the required capabilities.
     *
     * @param parserCaps The parser's available capabilities
     * @param requiredCaps The required capabilities
     * @return true if all required capabilities are met, false otherwise
     */
    private boolean meetsCapabilities(Map<ParserCapability, Boolean> parserCaps,
                                     Map<ParserCapability, Boolean> requiredCaps) {
        for (Map.Entry<ParserCapability, Boolean> entry : requiredCaps.entrySet()) {
            if (entry.getValue() && !parserCaps.getOrDefault(entry.getKey(), false)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Gets or creates the legacy parser instance.
     * Uses double-checked locking for thread-safe lazy initialization.
     *
     * @return The legacy KotlinParser instance
     */
    private ParserBehavior getLegacyParser() {
        if (legacyParser == null) {
            synchronized (this) {
                if (legacyParser == null) {
                    legacyParser = new KotlinParser(sourceParser);
                }
            }
        }
        return legacyParser;
    }
    
    /**
     * Gets or creates the Pratt parser instance.
     * Uses double-checked locking for thread-safe lazy initialization.
     *
     * @return The KotlinPrattParser instance
     */
    private ParserBehavior getPrattParser() {
        if (prattParser == null) {
            synchronized (this) {
                if (prattParser == null) {
                    prattParser = new KotlinPrattParser(sourceParser);
                }
            }
        }
        return prattParser;
    }
    
    /**
     * Executes a parsing operation with performance monitoring and fallback.
     *
     * <p>This method provides:
     * <ul>
     *   <li>Performance metrics collection</li>
     *   <li>Slow operation detection and logging</li>
     *   <li>Automatic fallback to legacy parser on errors</li>
     * </ul>
     *
     * @param <T> The return type of the operation
     * @param operation The parsing operation to execute
     * @param operationName The operation name for logging
     * @return The result of the parsing operation
     * @throws RuntimeException if the operation fails and fallback is disabled
     */
    private <T> T executeWithMonitoring(Supplier<T> operation, String operationName) {
        if (!performanceMonitoringEnabled) {
            return operation.get();
        }
        
        long startTime = System.nanoTime();
        PerformanceMetrics metrics = currentParser == legacyParser ? legacyMetrics : prattMetrics;
        
        try {
            T result = operation.get();
            long elapsed = System.nanoTime() - startTime;
            metrics.recordParse(elapsed);
            
            if (elapsed > performanceThresholdNanos) {
                LOGGER.log(Level.WARNING, "Slow parse operation {0}: {1}ms", 
                          new Object[]{operationName, elapsed / 1_000_000});
            }
            
            return result;
        } catch (Exception e) {
            metrics.recordError();
            
            if (fallbackEnabled && currentParser != legacyParser) {
                LOGGER.log(Level.INFO, "Falling back to legacy parser for {0}", operationName);
                metrics.recordFallback();
                currentParser = getLegacyParser();
                return operation.get();
            }
            
            throw e;
        }
    }
    
    // ==================== ParserBehavior Implementation ====================
    
    /**
     * {@inheritDoc}
     *
     * <p>Parses a complete compilation unit, delegating to the current
     * parser implementation with performance monitoring.
     */
    @Override
    public void parseCU() {
        executeWithMonitoring(() -> {
            currentParser.parseCU();
            return null;
        }, "parseCU");
    }
    
    /**
     * {@inheritDoc}
     *
     * <p>Parses a part of a compilation unit, starting from the given state.
     */
    @Override
    public int parseCUpart(int state) {
        return executeWithMonitoring(() ->
            currentParser.parseCUpart(state),
            "parseCUpart"
        );
    }
    
    /**
     * {@inheritDoc}
     *
     * <p>Parses a package statement starting from the given token.
     */
    @Override
    public LocatableToken parsePackageStmt(LocatableToken token) {
        return executeWithMonitoring(() -> 
            currentParser.parsePackageStmt(token), 
            "parsePackageStmt"
        );
    }
    
    /**
     * {@inheritDoc}
     *
     * <p>Parses an import statement from the current token position.
     */
    @Override
    public void parseImportStatement() {
        executeWithMonitoring(() -> {
            currentParser.parseImportStatement();
            return null;
        }, "parseImportStatement");
    }
    
    /**
     * {@inheritDoc}
     *
     * <p>Parses an import statement starting from the given import token.
     */
    @Override
    public void parseImportStatement(LocatableToken importToken) {
        executeWithMonitoring(() -> {
            currentParser.parseImportStatement(importToken);
            return null;
        }, "parseImportStatement");
    }
    
    /**
     * {@inheritDoc}
     *
     * <p>Parses a type definition (class, interface, enum, etc.).
     */
    @Override
    public void parseTypeDef() {
        executeWithMonitoring(() -> {
            currentParser.parseTypeDef();
            return null;
        }, "parseTypeDef");
    }
    
    /**
     * {@inheritDoc}
     *
     * <p>Parses a type definition starting from the given token.
     */
    @Override
    public void parseTypeDef(LocatableToken firstToken) {
        executeWithMonitoring(() -> {
            currentParser.parseTypeDef(firstToken);
            return null;
        }, "parseTypeDef");
    }
    
    /**
     * {@inheritDoc}
     *
     * <p>Parses the body of a type definition.
     */
    @Override
    public LocatableToken parseTypeBody(int tdType, LocatableToken token) {
        return executeWithMonitoring(() -> 
            currentParser.parseTypeBody(tdType, token), 
            "parseTypeBody"
        );
    }
    
    /**
     * {@inheritDoc}
     *
     * <p>Parses the beginning of a type definition.
     */
    @Override
    public int parseTypeDefBegin() {
        return executeWithMonitoring(() ->
            currentParser.parseTypeDefBegin(),
            "parseTypeDefBegin"
        );
    }
    
    /**
     * {@inheritDoc}
     *
     * <p>Parses the second part of a type definition.
     */
    @Override
    public LocatableToken parseTypeDefPart2(boolean b) {
        return executeWithMonitoring(() ->
            currentParser.parseTypeDefPart2(b),
            "parseTypeDefPart2"
        );
    }
    
    /**
     * {@inheritDoc}
     *
     * <p>Parses a class element (field, method, nested type, etc.).
     */
    @Override
    public void parseClassElement(LocatableToken token) {
        executeWithMonitoring(() -> {
            currentParser.parseClassElement(token);
            return null;
        }, "parseClassElement");
    }
    
    /**
     * {@inheritDoc}
     *
     * <p>Parses the entire body of a class definition.
     */
    @Override
    public void parseClassBody() {
        executeWithMonitoring(() -> {
            currentParser.parseClassBody();
            return null;
        }, "parseClassBody");
    }
    
    /**
     * {@inheritDoc}
     *
     * <p>Parses a statement as defined in the interface.
     */
    @Override
    public LocatableToken parseStatement(LocatableToken last, boolean b) {
        return executeWithMonitoring(() ->
            currentParser.parseStatement(last, b),
            "parseStatement"
        );
    }
    
    /**
     * {@inheritDoc}
     *
     * <p>Parses a type specification with array processing.
     */
    @Override
    public boolean parseTypeSpec(boolean processArray) {
        return executeWithMonitoring(() ->
            currentParser.parseTypeSpec(processArray),
            "parseTypeSpec"
        );
    }
    
    /**
     * {@inheritDoc}
     *
     * <p>Parses a type specification with multiple parameters.
     */
    @Override
    public boolean parseTypeSpec(boolean b, boolean b1, java.util.List<LocatableToken> ll) {
        return executeWithMonitoring(() ->
            currentParser.parseTypeSpec(b, b1, ll),
            "parseTypeSpec"
        );
    }
    
    /**
     * {@inheritDoc}
     *
     * <p>Parses an expression.
     */
    @Override
    public void parseExpression() {
        executeWithMonitoring(() -> {
            currentParser.parseExpression();
            return null;
        }, "parseExpression");
    }
    
    
    /**
     * {@inheritDoc}
     *
     * <p>Parses variable declarations.
     */
    @Override
    public LocatableToken parseVariableDeclarations() {
        return executeWithMonitoring(() -> 
            currentParser.parseVariableDeclarations(), 
            "parseVariableDeclarations"
        );
    }
    
    
    /**
     * {@inheritDoc}
     *
     * <p>Parses method parameters and body.
     */
    @Override
    public void parseMethodParamsBody() {
        executeWithMonitoring(() -> {
            currentParser.parseMethodParamsBody();
            return null;
        }, "parseMethodParamsBody");
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Gets the current parser type being used.
     * 
     * @return The current parser type
     */
    public ParserType getCurrentParserType() {
        if (currentParser == legacyParser) {
            return ParserType.LEGACY;
        }
        if (currentParser == prattParser) {
            return ParserType.PRATT;
        }
        return ParserType.AUTO;
    }
    
    /**
     * Switches to a specific parser type.
     * 
     * @param type The parser type to switch to
     */
    public void switchParser(ParserType type) {
        switch (type) {
            case LEGACY:
                currentParser = getLegacyParser();
                break;
            case PRATT:
                currentParser = getPrattParser();
                break;
            case AUTO:
                currentParser = selectParser(null);
                break;
        }
        LOGGER.log(Level.INFO, "Switched to parser type: {0}", type);
    }
    
    /**
     * Gets performance metrics for the legacy parser.
     * 
     * @return The legacy parser performance metrics
     */
    public PerformanceMetrics getLegacyMetrics() {
        return legacyMetrics;
    }
    
    /**
     * Gets performance metrics for the Pratt parser.
     *
     * @return The Pratt parser performance metrics
     */
    public PerformanceMetrics getPrattMetrics() {
        return prattMetrics;
    }
    
    /**
     * Gets combined performance metrics from both parsers.
     *
     * <p>This method aggregates metrics from both legacy and Pratt parsers
     * to provide an overall view of parsing performance.
     *
     * @return Combined performance metrics
     */
    public PerformanceMetrics getCombinedMetrics() {
        PerformanceMetrics combined = new PerformanceMetrics();
        
        // Aggregate parse counts and times
        long legacyCount = legacyMetrics.getParseCount();
        long prattCount = prattMetrics.getParseCount();
        
        if (legacyCount > 0) {
            combined.parseCount.add(legacyCount);
            combined.totalParseTime.add(legacyMetrics.getAverageParseTimeNanos() * legacyCount);
        }
        
        if (prattCount > 0) {
            combined.parseCount.add(prattCount);
            combined.totalParseTime.add(prattMetrics.getAverageParseTimeNanos() * prattCount);
        }
        
        // Aggregate errors and fallbacks
        combined.errorCount.add(legacyMetrics.getErrorCount());
        combined.errorCount.add(prattMetrics.getErrorCount());
        combined.fallbackCount.add(legacyMetrics.getFallbackCount());
        combined.fallbackCount.add(prattMetrics.getFallbackCount());
        
        return combined;
    }
    
    /**
     * Resets all performance metrics for both parsers.
     *
     * <p>This is useful for:
     * <ul>
     *   <li>Starting fresh benchmarking sessions</li>
     *   <li>Clearing metrics after configuration changes</li>
     *   <li>Resetting after error recovery</li>
     * </ul>
     */
    public void resetMetrics() {
        legacyMetrics.reset();
        prattMetrics.reset();
        LOGGER.log(Level.INFO, "Performance metrics reset");
    }
    
    /**
     * Checks if a specific capability is supported by the current parser.
     * 
     * @param capability The capability to check
     * @return true if the capability is supported, false otherwise
     */
    public boolean supportsCapability(ParserCapability capability) {
        Map<ParserCapability, Boolean> caps = currentParser == legacyParser ? 
            legacyCapabilities : prattCapabilities;
        return caps.getOrDefault(capability, false);
    }
    
    /**
     * Gets all supported capabilities of the current parser.
     *
     * <p>The returned map is unmodifiable to prevent external
     * modification of capability declarations.
     *
     * @return An unmodifiable map of capabilities and their support status
     */
    public Map<ParserCapability, Boolean> getSupportedCapabilities() {
        Map<ParserCapability, Boolean> caps = currentParser == legacyParser ? 
            legacyCapabilities : prattCapabilities;
        return Collections.unmodifiableMap(new HashMap<>(caps));
    }
    
    @Override
    public String toString() {
        return String.format(
            "KotlinParserAdapter{current=%s, configured=%s, monitoring=%s, fallback=%s}",
            getCurrentParserType(),
            configuredType,
            performanceMonitoringEnabled,
            fallbackEnabled
        );
    }
}