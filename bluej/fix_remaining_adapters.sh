#!/bin/bash

# Fix HybridResultPatternAdapter
echo "Fixing HybridResultPatternAdapter..."
sed -i '
/^[[:space:]]*case.*:$/,/^[[:space:]]*break;$/ {
    s/\(// External delegate uses different method signatures - removed incorrect call\)$/\1/
    /^[[:space:]]*}$/d
}' src/test/java/bluej/parser/pratt/integration/benchmark/adapters/HybridResultPatternAdapter.java

# Fix ErrorRecoveryIntegrationAdapter
echo "Fixing ErrorRecoveryIntegrationAdapter..."
sed -i '
/^[[:space:]]*case.*:$/,/^[[:space:]]*break;$/ {
    s/\(// External delegate uses different method signatures - removed incorrect call\)$/\1/
    /^[[:space:]]*}$/d
}' src/test/java/bluej/parser/pratt/integration/benchmark/adapters/ErrorRecoveryIntegrationAdapter.java

# Fix K2ParserIntegrationAdapter (if needed)
echo "Fixing K2ParserIntegrationAdapter..."
sed -i '
/^[[:space:]]*case.*:$/,/^[[:space:]]*break;$/ {
    s/\(// External delegate uses different method signatures - removed incorrect call\)$/\1/
    /^[[:space:]]*}$/d
}' src/test/java/bluej/parser/pratt/integration/benchmark/adapters/K2ParserIntegrationAdapter.java

echo "Done fixing adapter files"