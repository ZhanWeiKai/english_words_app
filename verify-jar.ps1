$jarPath = "C:\claude-project\english-word-app\english-word-backend\target\english-word-backend-1.0.0.jar"
$tempDir = "C:\claude-project\english-word-app\temp-jar-extract"

# Clean up and create temp dir
if (Test-Path $tempDir) { Remove-Item -Recurse -Force $tempDir }
New-Item -ItemType Directory -Path $tempDir | Out-Null

# Extract jar
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::ExtractToDirectory($jarPath, $tempDir)

# Check class file for prompt strings
$classFile = Join-Path $tempDir "BOOT-INF\classes\com\englishword\service\ZhipuAIService.class"
$bytes = [System.IO.File]::ReadAllBytes($classFile)
$content = [System.Text.Encoding]::UTF8.GetString($bytes)

Write-Output "=== Verifying JAR contains new prompt ==="
Write-Output ""

$checks = @(
    @{Name="Examiner"; Pattern="Examiner"},
    @{Name="Chinese marker"; Pattern="中文："},
    @{Name="Collocations"; Pattern="常见搭配"},
    @{Name="Phonetic hint"; Pattern="音标"},
    @{Name="IELTS examiner"; Pattern="雅思口语考官"}
)

$allPassed = $true
foreach ($check in $checks) {
    $found = $content -match $check.Pattern
    $status = if ($found) { "✅ Found" } else { "❌ NOT Found" }
    Write-Output "$($check.Name): $status"
    if (-not $found) { $allPassed = $false }
}

Write-Output ""
if ($allPassed) {
    Write-Output "✅ JAR verification PASSED - contains new prompt"
} else {
    Write-Output "❌ JAR verification FAILED - missing prompt elements"
}

# Cleanup
Remove-Item -Recurse -Force $tempDir
