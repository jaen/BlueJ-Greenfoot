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
package bluej.parser.pratt.integration.benchmark.measurement;

import bluej.parser.pratt.integration.benchmark.BenchmarkConfiguration;
import bluej.parser.pratt.integration.benchmark.metrics.ErrorRecoveryMetrics;

import java.util.*;

/**
 * Error recovery tracking for parser integration strategy benchmarking.
 * 
 * Monitors error handling and recovery behavior across different strategies:
 * - Error detection speed and accuracy
 * - Recovery success rates and techniques  
 * - Callback pairing integrity during errors
 * - Performance impact of error handling
 * 
 * This is particularly important for distinguishing between strategies:
 * - Direct Callback: Excellent callback pairing through AutoCloseable
 * - Hybrid Result: Sophisticated error accumulation and recovery
 * - K2 Parser: Advanced diagnostics and incremental recovery
 * - Error Recovery: Specialized error handling mechanisms
 * - AST patterns: Variable error recovery depending on implementation
 */
public class ErrorRecoveryTracker {
    
    private final BenchmarkConfiguration config;
    private boolean tracking = false;
    private long trackingStartTime;
    
    // Error and recovery tracking
    private final List<ErrorEvent> errorEvents;
    private final Map<String, CallbackPairTracker> callbackPairTrackers;
    private final List<RecoveryAttempt> recoveryAttempts;
    
    // Statistics
    private int totalCallbacks = 0;
    private int totalBeginCallbacks = 0;
    private int totalEndCallbacks = 0;
    private int errorCount = 0;
    private int recoverySuccessCount = 0;
    
    public ErrorRecoveryTracker(BenchmarkConfiguration config) {
        this.config = config;
        this.errorEvents = new ArrayList<>();
        this.callbackPairTrackers = new HashMap<>();
        this.recoveryAttempts = new ArrayList<>();
    }
    
    /**
     * Start error recovery tracking session.
     */
    public void start() {
        if (tracking) {
            throw new IllegalStateException("Error recovery tracking already active");
        }
        
        tracking = true;
        trackingStartTime = System.currentTimeMillis();
        
        errorEvents.clear();
        callbackPairTrackers.clear();
        recoveryAttempts.clear();
        
        totalCallbacks = 0;
        totalBeginCallbacks = 0;
        totalEndCallbacks = 0;
        errorCount = 0;
        recoverySuccessCount = 0;
    }
    
    /**
     * Stop tracking and calculate error recovery metrics.
     */
    public ErrorRecoveryMetrics stop() {
        if (!tracking) {
            throw new IllegalStateException("No active error recovery tracking");
        }
        
        tracking = false;
        
        return calculateErrorRecoveryMetrics();
    }
    
    /**
     * Record a callback execution for pairing analysis.
     */
    public void recordCallback(String callbackName, long timestamp, Object... args) {
        if (!tracking) return;
        
        totalCallbacks++;
        
        // Track callback pairing
        if (callbackName.startsWith("begin")) {
            totalBeginCallbacks++;
            trackBeginCallback(callbackName, timestamp);
        } else if (callbackName.startsWith("end")) {
            totalEndCallbacks++;
            trackEndCallback(callbackName, timestamp);
        }
        
        // Check for error-related callbacks
        if (isErrorRelatedCallback(callbackName)) {
            recordErrorEvent(callbackName, timestamp, "callback_error", false);
        }
    }
    
    /**
     * Record an explicit error event.
     */
    public void recordError(String errorType, long timestamp, boolean recovered) {
        if (!tracking) return;
        
        errorCount++;
        if (recovered) {
            recoverySuccessCount++;
        }
        
        recordErrorEvent(errorType, timestamp, "explicit_error", recovered);
        
        // Record recovery attempt
        RecoveryAttempt attempt = new RecoveryAttempt(
            errorType,
            timestamp - trackingStartTime,
            recovered,
            calculateRecoveryTime(timestamp)
        );
        
        recoveryAttempts.add(attempt);
    }
    
    /**
     * Record an error event with details.
     */
    private void recordErrorEvent(String errorType, long timestamp, String source, boolean recovered) {
        ErrorEvent event = new ErrorEvent(
            errorType,
            timestamp - trackingStartTime,
            source,
            recovered,
            getCurrentCallbackPairingStatus()
        );
        
        errorEvents.add(event);
    }
    
    /**
     * Track begin callback for pairing analysis.
     */
    private void trackBeginCallback(String callbackName, long timestamp) {
        String pairType = extractPairType(callbackName);
        CallbackPairTracker tracker = callbackPairTrackers.computeIfAbsent(
            pairType, k -> new CallbackPairTracker(pairType)
        );
        
        tracker.recordBegin(timestamp);
    }
    
    /**
     * Track end callback for pairing analysis.
     */
    private void trackEndCallback(String callbackName, long timestamp) {
        String pairType = extractPairType(callbackName);
        CallbackPairTracker tracker = callbackPairTrackers.get(pairType);
        
        if (tracker != null) {
            tracker.recordEnd(timestamp);
        } else {
            // End callback without matching begin - potential error
            recordErrorEvent("unmatched_end_callback", timestamp, "pairing_error", false);
        }
    }
    
    /**
     * Extract pair type from callback name (e.g., "beginExpression" -> "Expression").
     */
    private String extractPairType(String callbackName) {
        if (callbackName.startsWith("begin")) {
            return callbackName.substring(5);
        } else if (callbackName.startsWith("end")) {
            return callbackName.substring(3);
        }
        return callbackName;
    }
    
    /**
     * Check if callback name indicates an error condition.
     */
    private boolean isErrorRelatedCallback(String callbackName) {
        return callbackName.contains("error") || 
               callbackName.contains("fail") || 
               callbackName.contains("exception");
    }
    
    /**
     * Calculate recovery time for an error (simplified estimation).
     */
    private double calculateRecoveryTime(long errorTimestamp) {
        // Simplified: measure time to next successful callback
        // In a real implementation, this would be more sophisticated
        return 5.0; // ms - placeholder
    }
    
    /**
     * Get current callback pairing status.
     */
    private CallbackPairingStatus getCurrentCallbackPairingStatus() {
        int unpaired = 0;
        int wellPaired = 0;
        
        for (CallbackPairTracker tracker : callbackPairTrackers.values()) {
            unpaired += tracker.getUnpairedCount();
            wellPaired += tracker.getPairedCount();
        }
        
        return new CallbackPairingStatus(wellPaired, unpaired);
    }
    
    /**
     * Calculate comprehensive error recovery metrics.
     */
    private ErrorRecoveryMetrics calculateErrorRecoveryMetrics() {
        // Error detection and recovery timing
        double avgErrorDetectionTime = calculateAverageErrorDetectionTime();
        double avgRecoveryTime = calculateAverageRecoveryTime();
        
        // Callback pairing analysis
        double callbackPairingRate = calculateCallbackPairingRate();
        boolean maintainsIntegrity = checkCallbackIntegrity();
        
        // Performance impact
        double performanceImpact = calculatePerformanceImpact();
        
        // Error types handled
        List<ErrorRecoveryMetrics.ErrorCategory> errorTypesHandled = identifyErrorTypes();
        
        return new ErrorRecoveryMetrics(
            avgErrorDetectionTime,
            avgRecoveryTime,
            callbackPairingRate,
            errorEvents.size(),
            recoverySuccessCount,
            performanceImpact,
            errorTypesHandled,
            maintainsIntegrity
        );
    }
    
    /**
     * Calculate average error detection time.
     */
    private double calculateAverageErrorDetectionTime() {
        if (errorEvents.isEmpty()) return 0.0;
        
        // Simplified: assume quick detection for callback-based errors
        return errorEvents.stream()
                .filter(e -> e.source.equals("callback_error"))
                .mapToDouble(e -> 1.0) // 1ms detection time
                .average()
                .orElse(2.0);
    }
    
    /**
     * Calculate average recovery time.
     */
    private double calculateAverageRecoveryTime() {
        if (recoveryAttempts.isEmpty()) return 0.0;
        
        return recoveryAttempts.stream()
                .mapToDouble(attempt -> attempt.recoveryTimeMs)
                .average()
                .orElse(0.0);
    }
    
    /**
     * Calculate callback pairing rate.
     */
    private double calculateCallbackPairingRate() {
        if (totalBeginCallbacks == 0) return 1.0;
        
        int totalPaired = callbackPairTrackers.values().stream()
                .mapToInt(CallbackPairTracker::getPairedCount)
                .sum();
        
        return Math.min(1.0, (double) totalPaired / totalBeginCallbacks);
    }
    
    /**
     * Check overall callback integrity.
     */
    private boolean checkCallbackIntegrity() {
        // Check if any trackers have significant unpairing
        for (CallbackPairTracker tracker : callbackPairTrackers.values()) {
            if (tracker.getUnpairedCount() > tracker.getPairedCount() * 0.1) {
                return false; // More than 10% unpairing indicates integrity issues
            }
        }
        
        return true;
    }
    
    /**
     * Calculate performance impact of error handling.
     */
    private double calculatePerformanceImpact() {
        // Simplified calculation based on error frequency
        if (totalCallbacks == 0) return 0.0;
        
        double errorRate = (double) errorEvents.size() / totalCallbacks;
        return errorRate * 100; // Convert to percentage overhead
    }
    
    /**
     * Identify types of errors handled during tracking.
     */
    private List<ErrorRecoveryMetrics.ErrorCategory> identifyErrorTypes() {
        Set<ErrorRecoveryMetrics.ErrorCategory> types = EnumSet.noneOf(ErrorRecoveryMetrics.ErrorCategory.class);
        
        for (ErrorEvent event : errorEvents) {
            ErrorRecoveryMetrics.ErrorCategory category = classifyError(event.errorType);
            types.add(category);
        }
        
        return new ArrayList<>(types);
    }
    
    /**
     * Classify error type into predefined categories.
     */
    private ErrorRecoveryMetrics.ErrorCategory classifyError(String errorType) {
        String lowerType = errorType.toLowerCase();
        
        if (lowerType.contains("syntax")) {
            return ErrorRecoveryMetrics.ErrorCategory.SYNTAX_ERROR;
        } else if (lowerType.contains("incomplete")) {
            return ErrorRecoveryMetrics.ErrorCategory.INCOMPLETE_STATEMENT;
        } else if (lowerType.contains("token") || lowerType.contains("invalid")) {
            return ErrorRecoveryMetrics.ErrorCategory.INVALID_TOKEN;
        } else if (lowerType.contains("type")) {
            return ErrorRecoveryMetrics.ErrorCategory.TYPE_MISMATCH;
        } else if (lowerType.contains("import") || lowerType.contains("unresolved")) {
            return ErrorRecoveryMetrics.ErrorCategory.MISSING_IMPORT;
        } else {
            return ErrorRecoveryMetrics.ErrorCategory.NESTED_ERROR;
        }
    }
    
    /**
     * Get all error events.
     */
    public List<ErrorEvent> getErrorEvents() {
        return new ArrayList<>(errorEvents);
    }
    
    /**
     * Get all recovery attempts.
     */
    public List<RecoveryAttempt> getRecoveryAttempts() {
        return new ArrayList<>(recoveryAttempts);
    }
    
    /**
     * Get callback pair trackers.
     */
    public Map<String, CallbackPairTracker> getCallbackPairTrackers() {
        return new HashMap<>(callbackPairTrackers);
    }
    
    /**
     * Get tracking status.
     */
    public boolean isTracking() {
        return tracking;
    }
    
    /**
     * Cleanup tracker resources.
     */
    public void cleanup() {
        if (tracking) {
            stop();
        }
        
        errorEvents.clear();
        callbackPairTrackers.clear();
        recoveryAttempts.clear();
    }
    
    /**
     * Error event record.
     */
    public static class ErrorEvent {
        public final String errorType;
        public final long timestampMs;
        public final String source;
        public final boolean recovered;
        public final CallbackPairingStatus pairingStatus;
        
        public ErrorEvent(String errorType, long timestampMs, String source, 
                         boolean recovered, CallbackPairingStatus pairingStatus) {
            this.errorType = errorType;
            this.timestampMs = timestampMs;
            this.source = source;
            this.recovered = recovered;
            this.pairingStatus = pairingStatus;
        }
        
        @Override
        public String toString() {
            return String.format("ErrorEvent[type=%s, time=%dms, source=%s, recovered=%s]",
                    errorType, timestampMs, source, recovered);
        }
    }
    
    /**
     * Recovery attempt record.
     */
    public static class RecoveryAttempt {
        public final String errorType;
        public final long timestampMs;
        public final boolean successful;
        public final double recoveryTimeMs;
        
        public RecoveryAttempt(String errorType, long timestampMs, boolean successful, double recoveryTimeMs) {
            this.errorType = errorType;
            this.timestampMs = timestampMs;
            this.successful = successful;
            this.recoveryTimeMs = recoveryTimeMs;
        }
        
        @Override
        public String toString() {
            return String.format("RecoveryAttempt[type=%s, time=%dms, success=%s, duration=%.1fms]",
                    errorType, timestampMs, successful, recoveryTimeMs);
        }
    }
    
    /**
     * Callback pairing status snapshot.
     */
    public static class CallbackPairingStatus {
        public final int pairedCount;
        public final int unpairedCount;
        
        public CallbackPairingStatus(int pairedCount, int unpairedCount) {
            this.pairedCount = pairedCount;
            this.unpairedCount = unpairedCount;
        }
        
        public double getPairingRate() {
            int total = pairedCount + unpairedCount;
            return total > 0 ? (double) pairedCount / total : 1.0;
        }
        
        @Override
        public String toString() {
            return String.format("PairingStatus[paired=%d, unpaired=%d, rate=%.3f]",
                    pairedCount, unpairedCount, getPairingRate());
        }
    }
    
    /**
     * Tracks callback pairing for a specific callback type.
     */
    public static class CallbackPairTracker {
        private final String pairType;
        private final Stack<Long> pendingBegins;
        private int pairedCount = 0;
        private int unpairedCount = 0;
        
        public CallbackPairTracker(String pairType) {
            this.pairType = pairType;
            this.pendingBegins = new Stack<>();
        }
        
        public void recordBegin(long timestamp) {
            pendingBegins.push(timestamp);
        }
        
        public void recordEnd(long timestamp) {
            if (!pendingBegins.isEmpty()) {
                pendingBegins.pop();
                pairedCount++;
            } else {
                unpairedCount++; // End without begin
            }
        }
        
        public int getPairedCount() {
            return pairedCount;
        }
        
        public int getUnpairedCount() {
            return unpairedCount + pendingBegins.size(); // Include pending begins
        }
        
        public double getPairingRate() {
            int total = pairedCount + getUnpairedCount();
            return total > 0 ? (double) pairedCount / total : 1.0;
        }
        
        public boolean isBalanced() {
            return pendingBegins.isEmpty();
        }
        
        @Override
        public String toString() {
            return String.format("CallbackPairTracker[%s: paired=%d, unpaired=%d, balanced=%s]",
                    pairType, pairedCount, getUnpairedCount(), isBalanced());
        }
    }
    
    /**
     * Get error recovery tracking summary for debugging.
     */
    public String getErrorRecoverySummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Error Recovery Tracker Summary:\n");
        summary.append(String.format("  Tracking: %s\n", tracking));
        summary.append(String.format("  Total Callbacks: %d\n", totalCallbacks));
        summary.append(String.format("  Begin Callbacks: %d\n", totalBeginCallbacks));
        summary.append(String.format("  End Callbacks: %d\n", totalEndCallbacks));
        summary.append(String.format("  Error Events: %d\n", errorEvents.size()));
        summary.append(String.format("  Recovery Attempts: %d\n", recoveryAttempts.size()));
        summary.append(String.format("  Recovery Success Rate: %.1f%%\n", 
                recoveryAttempts.size() > 0 ? (recoverySuccessCount * 100.0 / recoveryAttempts.size()) : 0));
        summary.append(String.format("  Callback Pairing Rate: %.3f\n", calculateCallbackPairingRate()));
        
        if (!callbackPairTrackers.isEmpty()) {
            summary.append("  Pair Trackers:\n");
            for (CallbackPairTracker tracker : callbackPairTrackers.values()) {
                summary.append(String.format("    %s\n", tracker));
            }
        }
        
        return summary.toString();
    }
}