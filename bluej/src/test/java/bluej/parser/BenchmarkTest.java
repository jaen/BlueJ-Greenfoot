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

/**
 * JUnit4 category interface for marking benchmark tests.
 *
 * <p>Tests annotated with {@code @Category(BenchmarkTest.class)} are considered
 * performance benchmarks and are excluded from regular test runs by default.
 * They can be run separately using a dedicated Gradle task.</p>
 *
 * <p>Benchmark tests typically:</p>
 * <ul>
 *   <li>Take longer to execute than regular unit tests</li>
 *   <li>Measure performance characteristics</li>
 *   <li>May be sensitive to system load and hardware</li>
 *   <li>Are not required for basic functionality validation</li>
 * </ul>
 *
 * @author BlueJ Team
 * @see org.junit.experimental.categories.Category
 */
public interface BenchmarkTest {
    // Marker interface - no methods needed
}
