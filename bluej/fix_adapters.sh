#!/bin/bash

# Fix all incorrect external delegate calls in adapter files
for file in src/test/java/bluej/parser/pratt/integration/benchmark/adapters/*.java; do
    echo "Fixing $file..."
    # Remove all incorrect external delegate calls
    sed -i 's/if (externalDelegate != null) {/\/\/ External delegate uses different method signatures - removed incorrect call/g' "$file"
    sed -i '/externalDelegate\.got.*$/d' "$file"
    sed -i '/^[[:space:]]*}$/N;s/\/\/ External delegate uses different method signatures - removed incorrect call\n[[:space:]]*}/\/\/ External delegate uses different method signatures - removed incorrect call/g' "$file"
done

echo "Done fixing adapter files"