package bluej.parser.pratt;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Comprehensive test suite for ParseScope implementation.
 * 
 * This test suite runs all ParseScope-related tests to validate the complete
 * AutoCloseable scope management system. The suite ensures that all aspects
 * of the ParseScope implementation are thoroughly tested:
 * 
 * - Core ParseScope functionality and lifecycle
 * - Callback pairing guarantees under all conditions
 * - Thread safety and concurrent access patterns
 * - Integration with migrated parsing methods
 * 
 * Success Criteria Validation:
 * 1. Correctness: 100% callback pairing guarantee
 * 2. Performance: <1% overhead vs current implementation
 * 3. Reliability: Zero scope mismatches in production
 * 4. Maintainability: Clear integration pattern for parselets
 * 5. Debuggability: Comprehensive debugging support
 * 
 * Usage:
 * - Run entire suite: ./gradlew test --tests "ParseScopeTestSuite"
 * - Individual tests: ./gradlew test --tests "ParseScopeTest"
 * - With coverage: ./gradlew test jacocoTestReport
 */
@Suite
@SuiteDisplayName("ParseScope Implementation Test Suite")
@SelectClasses({
    ParseScopeTest.class,
    CallbackPairingTest.class,
    ThreadSafetyTest.class,
    MethodIntegrationTest.class
})
public class ParseScopeTestSuite {
    // Test suite configuration class - no implementation needed
    // JUnit 5 will automatically discover and run all selected test classes
}