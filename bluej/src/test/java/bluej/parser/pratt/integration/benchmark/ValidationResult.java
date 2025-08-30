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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Result of validating a parser strategy adapter for benchmarking.
 * 
 * Contains information about whether the adapter is properly configured
 * and ready for benchmarking, along with any issues that were discovered
 * during validation.
 */
public class ValidationResult {
    private final boolean success;
    private final List<String> issues;
    private final List<String> warnings;
    private final String summary;
    
    private ValidationResult(boolean success, List<String> issues, List<String> warnings, String summary) {
        this.success = success;
        this.issues = new ArrayList<>(issues);
        this.warnings = new ArrayList<>(warnings);
        this.summary = summary;
    }
    
    /**
     * Check if validation was successful.
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Get list of validation issues (errors that prevent benchmarking).
     */
    public List<String> getIssues() {
        return new ArrayList<>(issues);
    }
    
    /**
     * Get list of validation warnings (potential problems but not blocking).
     */
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }
    
    /**
     * Get summary of validation results.
     */
    public String getSummary() {
        return summary;
    }
    
    /**
     * Check if there are any issues.
     */
    public boolean hasIssues() {
        return !issues.isEmpty();
    }
    
    /**
     * Check if there are any warnings.
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    /**
     * Get total count of issues and warnings.
     */
    public int getTotalProblemsCount() {
        return issues.size() + warnings.size();
    }
    
    /**
     * Create a successful validation result.
     */
    public static ValidationResult success() {
        return success("Validation passed successfully");
    }
    
    /**
     * Create a successful validation result with custom summary.
     */
    public static ValidationResult success(String summary) {
        return new ValidationResult(true, new ArrayList<>(), new ArrayList<>(), summary);
    }
    
    /**
     * Create a failed validation result with a single issue.
     */
    public static ValidationResult failure(String issue) {
        List<String> issues = new ArrayList<>();
        issues.add(issue);
        return new ValidationResult(false, issues, new ArrayList<>(), 
                "Validation failed: " + issue);
    }
    
    /**
     * Create a failed validation result with multiple issues.
     */
    public static ValidationResult failure(List<String> issues) {
        return new ValidationResult(false, issues, new ArrayList<>(), 
                "Validation failed with " + issues.size() + " issue(s)");
    }
    
    /**
     * Create a validation result with both issues and warnings.
     */
    public static ValidationResult withProblems(List<String> issues, List<String> warnings) {
        boolean success = issues.isEmpty();
        String summary;
        if (success) {
            summary = warnings.isEmpty() ? "Validation passed" : 
                      "Validation passed with " + warnings.size() + " warning(s)";
        } else {
            summary = "Validation failed with " + issues.size() + " issue(s)" +
                     (warnings.isEmpty() ? "" : " and " + warnings.size() + " warning(s)");
        }
        return new ValidationResult(success, issues, warnings, summary);
    }
    
    /**
     * Builder for ValidationResult.
     */
    public static class Builder {
        private final List<String> issues = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private String summary;
        
        public Builder addIssue(String issue) {
            issues.add(issue);
            return this;
        }
        
        public Builder addWarning(String warning) {
            warnings.add(warning);
            return this;
        }
        
        public Builder addIssues(List<String> issues) {
            this.issues.addAll(issues);
            return this;
        }
        
        public Builder addWarnings(List<String> warnings) {
            this.warnings.addAll(warnings);
            return this;
        }
        
        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }
        
        public ValidationResult build() {
            boolean success = issues.isEmpty();
            String finalSummary = summary;
            
            if (finalSummary == null) {
                if (success) {
                    finalSummary = warnings.isEmpty() ? "Validation passed" : 
                                  "Validation passed with " + warnings.size() + " warning(s)";
                } else {
                    finalSummary = "Validation failed with " + issues.size() + " issue(s)" +
                                 (warnings.isEmpty() ? "" : " and " + warnings.size() + " warning(s)");
                }
            }
            
            return new ValidationResult(success, issues, warnings, finalSummary);
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ValidationResult that = (ValidationResult) obj;
        return success == that.success &&
               Objects.equals(issues, that.issues) &&
               Objects.equals(warnings, that.warnings) &&
               Objects.equals(summary, that.summary);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(success, issues, warnings, summary);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ValidationResult{")
          .append("success=").append(success)
          .append(", summary='").append(summary).append("'");
        
        if (!issues.isEmpty()) {
            sb.append(", issues=").append(issues);
        }
        
        if (!warnings.isEmpty()) {
            sb.append(", warnings=").append(warnings);
        }
        
        sb.append("}");
        return sb.toString();
    }
}