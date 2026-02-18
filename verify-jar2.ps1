$jarPath = "C:\claude-project\english-word-app\english-word-backend\target\english-word-backend-1.0.0.jar"
$tempDir = "C:\claude-project\english-word-app\temp-jar-extract"

# Clean up and create temp dir
if (Test-Path $tempDir) { Remove-Item -Recurse -Force $tempDir }
New-Item -ItemType Directory -Path $tempDir | Out-Null

# Extract jar
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::ExtractToDirectory($jarPath, $tempDir)

# Check class file for prompt strings using byte search
$classFile = Join-Path $tempDir "BOOT-INF\classes\com\englishword\service\ZhipuAIService.class"
$bytes = [System.IO.File]::ReadAllBytes($classFile)

# Convert to string using default encoding
$encoder = [System.Text.Encoding]::GetEncoding("iso-8859-1")
$content = $encoder.GetString($bytes)

Write-Output "=== Verifying JAR contains new prompt ==="
Write-Output "Class file size: $($bytes.Length) bytes"
Write-Output ""

# Search for key ASCII strings that should be in any version of the prompt
$checks = @(
    @{Name="Examiner"; Pattern="Examiner"},
    @{Name="complex"; Pattern="complex"},
    @{Name="Cities today"; Pattern="Cities today"},
    @{Name="Let's start"; Pattern="Let's start"},
    @{Name="IELTS"; Pattern="IELTS"},
    @{Name="scenario"; Pattern="scenario"}
)

$allPassed = $true
foreach ($check in $checks) {
    $found = $content -match [regex]::Escape($check.Pattern)
    $status = if ($found) { "[OK]" } else { "[MISSING]" }
    Write-Output "$($check.Name): $status"
    if (-not $found) { $allPassed = $false }
}

Write-Output ""
if ($allPassed) {
    Write-Output "JAR verification PASSED"
} else {
    Write-Output "JAR verification FAILED"
}

# Cleanup
Remove-Item -Recurse -Force $tempDir
