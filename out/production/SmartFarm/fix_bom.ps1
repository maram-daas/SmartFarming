# fix_bom.ps1 - Run this in your SmartFarming/src/ folder

Write-Host "Fixing BOM characters in all Java files..." -ForegroundColor Cyan

# Get all Java files
$files = Get-ChildItem -Recurse -Filter "*.java"

$fixedCount = 0

foreach ($file in $files) {
    # Read file content as bytes to detect BOM
    $bytes = [System.IO.File]::ReadAllBytes($file.FullName)

    # Check for BOM (EF BB BF for UTF-8 BOM)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        # Remove first 3 bytes (BOM)
        $newBytes = $bytes[3..$($bytes.Length - 1)]
        [System.IO.File]::WriteAllBytes($file.FullName, $newBytes)
        Write-Host "  Fixed: $($file.Name)" -ForegroundColor Green
        $fixedCount++
    }
}

Write-Host "`nFixed $fixedCount files!" -ForegroundColor Green
Write-Host "Now rebuild your project in IntelliJ: Build -> Rebuild Project" -ForegroundColor Yellow