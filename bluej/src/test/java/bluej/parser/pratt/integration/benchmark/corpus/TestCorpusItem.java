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
package bluej.parser.pratt.integration.benchmark.corpus;

import bluej.parser.pratt.integration.benchmark.BenchmarkConfiguration;
import java.util.Objects;
import java.util.List;

/**
 * Represents a single test corpus item for parser integration benchmarking.
 * 
 * This class encapsulates a piece of Java source code along with its metadata
 * for use in parser strategy benchmarking. Each corpus item includes:
 * 
 * - The source code to be parsed
 * - Complexity level classification
 * - Descriptive information
 * - Flags indicating special characteristics (errors, incomplete code)
 * 
 * Used extensively by the benchmark framework to provide consistent,
 * well-characterized test data across different parser strategies.
 */
public class TestCorpusItem {
    
    private final String sourceCode;
    private final BenchmarkConfiguration.ComplexityLevel complexityLevel;
    private final String description;
    private final boolean hasErrors;
    private final boolean isIncomplete;
    private final long estimatedComplexityScore;
    
    /**
     * Create a test corpus item with basic information.
     */
    public TestCorpusItem(String sourceCode, 
                         BenchmarkConfiguration.ComplexityLevel complexityLevel,
                         String description) {
        this(sourceCode, complexityLevel, description, false, false);
    }
    
    /**
     * Create a test corpus item with error/incomplete flags.
     */
    public TestCorpusItem(String sourceCode,
                         BenchmarkConfiguration.ComplexityLevel complexityLevel,
                         String description,
                         boolean hasErrors,
                         boolean isIncomplete) {
        this.sourceCode = sourceCode;
        this.complexityLevel = complexityLevel;
        this.description = description;
        this.hasErrors = hasErrors;
        this.isIncomplete = isIncomplete;
        this.estimatedComplexityScore = calculateComplexityScore();
    }
    
    /**
     * Get the Java source code for this corpus item.
     */
    public String getSourceCode() {
        return sourceCode;
    }
    
    /**
     * Get the complexity level classification.
     */
    public BenchmarkConfiguration.ComplexityLevel getComplexityLevel() {
        return complexityLevel;
    }
    
    /**
     * Get the human-readable description.
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if this corpus item contains intentional errors.
     */
    public boolean hasErrors() {
        return hasErrors;
    }
    
    /**
     * Check if this corpus item represents incomplete code.
     */
    public boolean isIncomplete() {
        return isIncomplete;
    }
    
    /**
     * Get the estimated complexity score (calculated metric).
     */
    public long getEstimatedComplexityScore() {
        return estimatedComplexityScore;
    }
    
    /**
     * Get the size of the source code in characters.
     */
    public int getSourceSize() {
        return sourceCode != null ? sourceCode.length() : 0;
    }
    
    /**
     * Get the number of lines in the source code.
     */
    public int getLineCount() {
        if (sourceCode == null || sourceCode.isEmpty()) {
            return 0;
        }
        return sourceCode.split("\r\n|\r|\n").length;
    }
    
    /**
     * Check if this corpus item is suitable for the given parser strategy.
     * Some strategies might not support error cases or incomplete code.
     */
    public boolean isSuitableFor(String strategyName) {
        // Error recovery strategies can handle errors and incomplete code
        if (strategyName.contains("ErrorRecovery")) {
            return true;
        }
        
        // Other strategies might not handle errors well in testing
        if (hasErrors || isIncomplete) {
            return strategyName.contains("Robust") || strategyName.contains("Recovery");
        }
        
        return true;
    }
    
    /**
     * Create a clean version of this corpus item (without errors or incomplete parts).
     */
    public TestCorpusItem createCleanVersion() {
        if (!hasErrors && !isIncomplete) {
            return this;
        }
        
        String cleanCode = sourceCode;
        
        // Basic error cleanup (this is simplistic)
        if (hasErrors) {
            cleanCode = cleanCode
                .replace("pubblic", "public")
                .replace("StringMismatch", "String");
        }
        
        return new TestCorpusItem(
            cleanCode,
            complexityLevel,
            description + " (cleaned)",
            false,
            false
        );
    }
    
    /**
     * Calculate an estimated complexity score based on source code characteristics.
     */
    private long calculateComplexityScore() {
        if (sourceCode == null || sourceCode.isEmpty()) {
            return 0;
        }
        
        long score = 0;
        
        // Base score from size
        score += sourceCode.length();
        
        // Additional complexity factors
        score += countOccurrences(sourceCode, "class") * 50;
        score += countOccurrences(sourceCode, "interface") * 60;
        score += countOccurrences(sourceCode, "method") * 30;
        score += countOccurrences(sourceCode, "public") * 10;
        score += countOccurrences(sourceCode, "private") * 10;
        score += countOccurrences(sourceCode, "protected") * 10;
        
        // Generic complexity
        score += countOccurrences(sourceCode, "<") * 20;
        score += countOccurrences(sourceCode, "extends") * 25;
        score += countOccurrences(sourceCode, "implements") * 25;
        
        // Control flow complexity
        score += countOccurrences(sourceCode, "if") * 15;
        score += countOccurrences(sourceCode, "for") * 20;
        score += countOccurrences(sourceCode, "while") * 20;
        score += countOccurrences(sourceCode, "switch") * 25;
        score += countOccurrences(sourceCode, "try") * 30;
        
        // Modern Java features
        score += countOccurrences(sourceCode, "->") * 25;  // Lambda
        score += countOccurrences(sourceCode, "::") * 20;  // Method reference
        score += countOccurrences(sourceCode, "stream") * 30;
        score += countOccurrences(sourceCode, "@") * 15;   // Annotations
        
        // Complexity level multiplier
        switch (complexityLevel) {
            case SIMPLE:
                score = (long) (score * 1.0);
                break;
            case MODERATE:
                score = (long) (score * 1.5);
                break;
            case COMPLEX:
                score = (long) (score * 2.0);
                break;
        }
        
        // Error/incomplete penalty/bonus
        if (hasErrors) {
            score = (long) (score * 1.3);  // Errors increase complexity
        }
        if (isIncomplete) {
            score = (long) (score * 0.8);  // Incomplete reduces complexity
        }
        
        return score;
    }
    
    /**
     * Helper method to count occurrences of a substring.
     */
    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
    
    /**
     * Get summary information about this corpus item.
     */
    public String getSummary() {
        return String.format(
            "TestCorpusItem{level=%s, size=%d chars, lines=%d, score=%d, errors=%s, incomplete=%s}",
            complexityLevel.name(),
            getSourceSize(),
            getLineCount(),
            estimatedComplexityScore,
            hasErrors,
            isIncomplete
        );
    }
    
    @Override
    public String toString() {
        return String.format(
            "TestCorpusItem{%s: %s}",
            complexityLevel.name(),
            description
        );
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TestCorpusItem that = (TestCorpusItem) obj;
        return hasErrors == that.hasErrors &&
               isIncomplete == that.isIncomplete &&
               Objects.equals(sourceCode, that.sourceCode) &&
               complexityLevel == that.complexityLevel &&
               Objects.equals(description, that.description);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(sourceCode, complexityLevel, description, hasErrors, isIncomplete);
    }
    
    // ==================== Compatibility Methods ====================
    
    /**
     * Get code samples as a list for compatibility with legacy tests.
     * Returns the single source code string wrapped in a list.
     */
    public List<String> getCodeSamples() {
        return java.util.Collections.singletonList(sourceCode);
    }
}