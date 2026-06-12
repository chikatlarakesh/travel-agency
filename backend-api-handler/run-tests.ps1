# Set JDK 17
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:PATH = "C:\Program Files\Java\jdk-17\bin;" + $env:PATH

# Navigate to project
Set-Location "C:\Users\BhagyashreeDeshpande\Desktop\pbe\api-handler"

Write-Output "=== Running Tests with JDK 17 ==="
Write-Output "JAVA_HOME: $env:JAVA_HOME"
Write-Output ""

# Run compilation
Write-Output "[1/2] Compiling project..."
$compileResult = mvn clean compile -q
if ($LASTEXITCODE -eq 0) {
    Write-Output "✓ Compilation successful"
} else {
    Write-Output "✗ Compilation failed with exit code $LASTEXITCODE"
    exit 1
}

Write-Output ""

# Run tests
Write-Output "[2/2] Running tests..."
$testResult = mvn test -q
if ($LASTEXITCODE -eq 0) {
    Write-Output "✓ All tests passed"
} else {
    Write-Output "✗ Tests failed with exit code $LASTEXITCODE"
    exit 1
}

Write-Output ""
Write-Output "=== Test Summary ==="
mvn test -q 2>&1 | Select-String "Tests run" | Select-Object -Last 10

