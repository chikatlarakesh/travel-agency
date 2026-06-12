#!/usr/bin/env pwsh
# Run the Spring Boot fat-JAR locally with all required JVM flags.
# Usage: .\run.ps1

$jar = Get-ChildItem -Path "target" -Filter "*.jar" | Where-Object { $_.Name -notlike "*original*" } | Select-Object -First 1

if (-not $jar) {
    Write-Error "No JAR found in target/. Run 'mvn package -DskipTests' first."
    exit 1
}

java `
    --enable-native-access=ALL-UNNAMED `
    -jar "target\$($jar.Name)"

