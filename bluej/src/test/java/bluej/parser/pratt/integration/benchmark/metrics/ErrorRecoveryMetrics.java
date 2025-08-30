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
package bluej.parser.pratt.integration.benchmark.metrics;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

/**
 * Error recovery metrics for parser integration strategy benchmarking.
 * 
 * Measures how well each strategy handles malformed or incomplete code:
 * - Error detection speed and accuracy
 * - Recovery success rates and techniques
 * - Callback pairing guarantees during errors
 * - Performance impact of error handling
 * 
 * These metrics help distinguish between strategies like:
 * - Direct Callback (AutoCloseable guarantees callback pairing)
 * - Hybrid Result (sophisticated error accumulation)
 * - K2 Parser (excellent diagnostics and recovery)
 * - Error Recovery Integration (specialized error handling)
 */
public class ErrorRecoveryMetrics {
    private final double errorDetectionTimeMs;
    private final double recoveryTimeMs;
    private final double callbackPairingRate;
    private final int errorsDetected;
    private final int errorsRecovered;
    private final double performanceImpactPercent;
    private final List<ErrorCategory> errorTypesHandled;
    private final boolean maintainsCallbackIntegrity;
    
    public ErrorRecoveryMetrics(double errorDetectionTimeMs,
                               double recoveryTimeMs,
                               double callbackPairingRate,
                               int errorsDetected,
                               int errorsRecovered,
                               double performanceImpactPercent,
                               List<ErrorCategory> errorTypesHandled,
                               boolean maintainsCallbackIntegrity) {
        this.errorDetectionTimeMs = errorDetectionTimeMs;
        this.recoveryTimeMs = recoveryTimeMs;
        this.callbackPairingRate = callbackPairingRate;
        this.errorsDetected = errorsDetected;
        this.errorsRecovered = errorsRecovered;
        this.performanceImpactPercent = performanceImpactPercent;
        this.errorTypesHandled = errorTypesHandled;
        this.maintainsCallbackIntegrity = maintainsCallbackIntegrity;
    }
    
    /**
     * Time to detect syntax errors in milliseconds.
     * Lower values indicate faster error detection.
     */
    public double getErrorDetectionTimeMs() {
        return errorDetectionTimeMs;
    }
    
    /**
     * Time to recover from errors and continue parsing in milliseconds.
     */
    public double getRecoveryTimeMs() {
        return recoveryTimeMs;
    }
    
    /**
     * Rate of proper callback pairing during error conditions (0.0 to 1.0).
     * 1.0 means all begin callbacks had matching end callbacks even with errors.
     */
    public double getCallbackPairingRate() {
        return callbackPairingRate;
    }
    
    /**
     * Total number of errors detected during testing.
     */
    public int getErrorsDetected() {
        return errorsDetected;
    }
    
    /**
     * Number of errors successfully recovered from.
     */
    public int getErrorsRecovered() {
        return errorsRecovered;
    }
    
    /**
     * Performance impact of error handling as percentage overhead.
     */
    public double getPerformanceImpactPercent() {
        return performanceImpactPercent;
    }
    
    /**
     * Types of errors this strategy can handle.
     */
    public List<ErrorCategory> getErrorTypesHandled() {
        return errorTypesHandled;
    }
    
    /**
     * Whether callback integrity is maintained during errors.
     */
    public boolean getMaintainsCallbackIntegrity() {
        return maintainsCallbackIntegrity;
    }
    
    /**
     * Calculate recovery success rate (0.0 to 1.0).
     */
    public double getRecoverySuccessRate() {
        return errorsDetected > 0 ? (double) errorsRecovered / errorsDetected : 1.0;
    }
    
    /**
     * Calculate overall error handling score (higher is better).
     * Combines detection speed, recovery rate, and callback integrity.
     */
    public double getErrorHandlingScore() {
        double detectionScore = Math.max(0, 30 - Math.log(errorDetectionTimeMs + 1) * 5);
        double recoveryScore = getRecoverySuccessRate() * 40;
        double pairingScore = callbackPairingRate * 20;
        double impactScore = Math.max(0, 10 - performanceImpactPercent * 0.5);
        
        return detectionScore + recoveryScore + pairingScore + impactScore;
    }
    
    /**
     * Check if error recovery is considered robust.
     */
    public boolean isRobustErrorRecovery() {
        return getRecoverySuccessRate() > 0.8 && 
               callbackPairingRate > 0.95 && 
               maintainsCallbackIntegrity;
    }
    
    /**
     * Check if error handling has minimal performance impact.
     */
    public boolean hasLowPerformanceImpact() {
        return performanceImpactPercent < 10.0;
    }
    
    /**
     * Get error recovery category for this strategy.
     */
    public ErrorRecoveryCategory getCategory() {
        if (isRobustErrorRecovery() && hasLowPerformanceImpact()) {
            return ErrorRecoveryCategory.EXCELLENT;
        } else if (isRobustErrorRecovery() || hasLowPerformanceImpact()) {
            return ErrorRecoveryCategory.GOOD;
        } else if (callbackPairingRate > 0.8) {
            return ErrorRecoveryCategory.MODERATE;
        } else {
            return ErrorRecoveryCategory.LIMITED;
        }
    }
    
    /**
     * Categories of parsing errors that strategies can handle.
     */
    public enum ErrorCategory {
        SYNTAX_ERROR("Missing semicolons, brackets, etc."),
        INCOMPLETE_STATEMENT("Truncated expressions or declarations"),
        INVALID_TOKEN("Unrecognized characters or keywords"),
        TYPE_MISMATCH("Semantic type errors"),
        MISSING_IMPORT("Unresolved references"),
        NESTED_ERROR("Errors within nested structures");
        
        private final String description;
        
        ErrorCategory(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Alias for ErrorCategory to maintain compatibility.
     * @deprecated Use ErrorCategory instead
     */
    @Deprecated
    public enum ErrorType {
        SYNTAX_ERROR("Missing semicolons, brackets, etc."),
        INCOMPLETE_STATEMENT("Truncated expressions or declarations"),
        INVALID_TOKEN("Unrecognized characters or keywords"),
        TYPE_MISMATCH("Semantic type errors"),
        MISSING_IMPORT("Unresolved references"),
        NESTED_ERROR("Errors within nested structures"),
        SEMANTIC_ERROR("General semantic errors"),
        SCOPE_ERROR("Variable scope and visibility errors");
        
        private final String description;
        
        ErrorType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Error recovery capability categories.
     */
    public enum ErrorRecoveryCategory {
        EXCELLENT("Excellent error recovery with minimal impact"),
        GOOD("Good error recovery with some limitations"),
        MODERATE("Basic error recovery capabilities"),
        LIMITED("Limited error recovery, may fail on complex errors");
        
        private final String description;
        
        ErrorRecoveryCategory(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    @Override
    public String toString() {
        return String.format("ErrorRecoveryMetrics{recovery=%.1f%%, pairing=%.3f, category=%s}",
                getRecoverySuccessRate() * 100, callbackPairingRate, getCategory());
    }
    
    /**
     * Builder for ErrorRecoveryMetrics.
     */
    public static class Builder {
        private double errorDetectionTimeMs = 0.0;
        private double recoveryTimeMs = 0.0;
        private double callbackPairingRate = 1.0;
        private int errorsDetected = 0;
        private int errorsRecovered = 0;
        private double performanceImpactPercent = 0.0;
        private List<ErrorCategory> errorTypesHandled = new ArrayList<>();
        private boolean maintainsCallbackIntegrity = true;
        
        public Builder errorDetectionTimeMs(double errorDetectionTimeMs) {
            this.errorDetectionTimeMs = errorDetectionTimeMs;
            return this;
        }
        
        public Builder recoveryTimeMs(double recoveryTimeMs) {
            this.recoveryTimeMs = recoveryTimeMs;
            return this;
        }
        
        public Builder callbackPairingRate(double callbackPairingRate) {
            this.callbackPairingRate = callbackPairingRate;
            return this;
        }
        
        public Builder errorsDetected(int errorsDetected) {
            this.errorsDetected = errorsDetected;
            return this;
        }
        
        public Builder errorsRecovered(int errorsRecovered) {
            this.errorsRecovered = errorsRecovered;
            return this;
        }
        
        public Builder performanceImpactPercent(double performanceImpactPercent) {
            this.performanceImpactPercent = performanceImpactPercent;
            return this;
        }
        
        public Builder addErrorType(ErrorCategory errorType) {
            this.errorTypesHandled.add(errorType);
            return this;
        }
        
        public Builder errorTypesHandled(List<ErrorCategory> errorTypesHandled) {
            this.errorTypesHandled = new ArrayList<>(errorTypesHandled);
            return this;
        }
        
        public Builder errorTypesHandled(ErrorCategory... errorTypes) {
            this.errorTypesHandled = Arrays.asList(errorTypes);
            return this;
        }
        
        public Builder maintainsCallbackIntegrity(boolean maintainsCallbackIntegrity) {
            this.maintainsCallbackIntegrity = maintainsCallbackIntegrity;
            return this;
        }
        
        // Compatibility aliases for builder methods
        public Builder errorDetectionTime(double errorDetectionTimeMs) {
            return errorDetectionTimeMs(errorDetectionTimeMs);
        }
        
        public Builder recoveryTime(double recoveryTimeMs) {
            return recoveryTimeMs(recoveryTimeMs);
        }
        
        public Builder performanceImpact(double performanceImpactPercent) {
            this.performanceImpactPercent = performanceImpactPercent;
            return this;
        }
        
        public Builder errorTypesHandled(ErrorType... errorTypes) {
            this.errorTypesHandled.clear();
            for (ErrorType errorType : errorTypes) {
                ErrorCategory category = convertErrorType(errorType);
                this.errorTypesHandled.add(category);
            }
            return this;
        }
        
        private ErrorCategory convertErrorType(ErrorType errorType) {
            switch (errorType) {
                case SYNTAX_ERROR: return ErrorCategory.SYNTAX_ERROR;
                case INCOMPLETE_STATEMENT: return ErrorCategory.INCOMPLETE_STATEMENT;
                case INVALID_TOKEN: return ErrorCategory.INVALID_TOKEN;
                case TYPE_MISMATCH: return ErrorCategory.TYPE_MISMATCH;
                case MISSING_IMPORT: return ErrorCategory.MISSING_IMPORT;
                case NESTED_ERROR: return ErrorCategory.NESTED_ERROR;
                case SEMANTIC_ERROR: return ErrorCategory.TYPE_MISMATCH; // Map to closest equivalent
                case SCOPE_ERROR: return ErrorCategory.TYPE_MISMATCH; // Map to closest equivalent
                default: return ErrorCategory.SYNTAX_ERROR;
            }
        }
        
        public ErrorRecoveryMetrics build() {
            return new ErrorRecoveryMetrics(errorDetectionTimeMs, recoveryTimeMs, callbackPairingRate,
                                          errorsDetected, errorsRecovered, performanceImpactPercent,
                                          errorTypesHandled, maintainsCallbackIntegrity);
        }
    }
}