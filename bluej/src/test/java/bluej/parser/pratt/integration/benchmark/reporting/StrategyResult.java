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
package bluej.parser.pratt.integration.benchmark.reporting;

/**
 * Simplified strategy result for JMH integration.
 */
public class StrategyResult {
    
    private String strategyName;
    private double avgThroughput;
    private double avgLatency;
    private double memoryOverhead;
    private double scalabilityScore;
    
    public StrategyResult() {}
    
    public StrategyResult(String strategyName, double avgThroughput, double avgLatency, 
                         double memoryOverhead, double scalabilityScore) {
        this.strategyName = strategyName;
        this.avgThroughput = avgThroughput;
        this.avgLatency = avgLatency;
        this.memoryOverhead = memoryOverhead;
        this.scalabilityScore = scalabilityScore;
    }
    
    // Getters and setters
    public String getStrategyName() { return strategyName; }
    public void setStrategyName(String strategyName) { this.strategyName = strategyName; }
    
    public double getAvgThroughput() { return avgThroughput; }
    public void setAvgThroughput(double avgThroughput) { this.avgThroughput = avgThroughput; }
    
    public double getAvgLatency() { return avgLatency; }
    public void setAvgLatency(double avgLatency) { this.avgLatency = avgLatency; }
    
    public double getMemoryOverhead() { return memoryOverhead; }
    public void setMemoryOverhead(double memoryOverhead) { this.memoryOverhead = memoryOverhead; }
    
    public double getScalabilityScore() { return scalabilityScore; }
    public void setScalabilityScore(double scalabilityScore) { this.scalabilityScore = scalabilityScore; }
    
    @Override
    public String toString() {
        return String.format("StrategyResult[%s: throughput=%.2f, latency=%.2f, memory=%.2f, scalability=%.2f]",
                strategyName, avgThroughput, avgLatency, memoryOverhead, scalabilityScore);
    }
}