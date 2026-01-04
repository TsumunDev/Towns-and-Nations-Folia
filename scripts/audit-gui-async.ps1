# ========================================
# GUI Async Pattern Audit Script
# ========================================
# Détecte les GUI non migrés vers le pattern async
# Usage: .\audit-gui-async.ps1
# Output: gui-audit-report.txt

$ErrorActionPreference = "Stop"
$guiPath = "tan-core\src\main\java\org\leralix\tan\gui"
$reportFile = "gui-audit-report.txt"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  GUI Async Pattern Audit - Towns & Nations" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Initialize report
$report = @()
$report += "GUI ASYNC MIGRATION AUDIT REPORT"
$report += "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
$report += "="*60
$report += ""

# Stats
$totalGUI = 0
$asyncCompliant = 0
$nonCompliant = 0
$suspicious = 0

# Pattern checks
$report += "SCANNING: $guiPath"
$report += ""

Get-ChildItem -Path $guiPath -Filter "*.java" -Recurse | ForEach-Object {
    $file = $_.FullName
    $relativePath = $file.Replace((Get-Location).Path + "\", "")
    $content = Get-Content $file -Raw
    $className = $_.BaseName
    
    # Skip if not extending BasicGui
    if ($content -notmatch "extends\s+BasicGui") {
        return
    }
    
    $totalGUI++
    $issues = @()
    $severity = "OK"
    
    # Check 1: Public constructor (should be private)
    if ($content -match "public\s+$className\s*\(") {
        $issues += "  [CRITICAL] Public constructor (should be private)"
        $severity = "CRITICAL"
    }
    
    # Check 2: Missing static open() method
    if ($content -notmatch "public\s+static\s+void\s+open\s*\(") {
        $issues += "  [CRITICAL] Missing 'public static void open(Player)' method"
        $severity = "CRITICAL"
    }
    
    # Check 3: getSync() usage in constructor
    if ($content -match "getSync\s*\(") {
        $issues += "  [CRITICAL] Uses getSync() - BLOCKS THREAD!"
        $severity = "CRITICAL"
    }
    
    # Check 4: Missing CompletableFuture pattern
    if ($content -notmatch "CompletableFuture") {
        $issues += "  [WARNING] No CompletableFuture usage"
        if ($severity -eq "OK") { $severity = "WARNING" }
    }
    
    # Check 5: Missing FoliaScheduler.runTask in open()
    if ($content -match "public\s+static\s+void\s+open\s*\(") {
        if ($content -notmatch "FoliaScheduler") {
            $issues += "  [WARNING] Static open() exists but no FoliaScheduler.runTask()"
            if ($severity -eq "OK") { $severity = "WARNING" }
        }
    }
    
    # Check 6: Direct open call without async
    $openMethodMatch = [regex]::Match($content, "public\s+static\s+void\s+open\s*\([^)]*\)\s*\{")
    if ($openMethodMatch.Success) {
        $afterOpen = $content.Substring($openMethodMatch.Index, [Math]::Min(500, $content.Length - $openMethodMatch.Index))
        if ($afterOpen -match "new\s+$className\([^)]*\)\.open\(\)" -and $afterOpen -notmatch "FoliaScheduler") {
            $issues += "  [WARNING] Direct .open() call without FoliaScheduler"
            if ($severity -eq "OK") { $severity = "WARNING" }
        }
    }
    
    # Classify result
    if ($issues.Count -eq 0) {
        $asyncCompliant++
        $report += "[OK] $relativePath"
        $report += "   Status: ASYNC COMPLIANT"
    } else {
        if ($severity -eq "CRITICAL") {
            $nonCompliant++
            $report += ""
            $report += "[CRITICAL] $relativePath"
        } elseif ($severity -eq "WARNING") {
            $suspicious++
            $report += ""
            $report += "[WARNING] $relativePath"
        }
        $report += $issues
    }
    $report += ""
}

# Summary
$report += ""
$report += "="*60
$report += "SUMMARY"
$report += "="*60
$report += "Total GUI files: $totalGUI"
$report += "[OK] Async compliant: $asyncCompliant ($('{0:P0}' -f ($asyncCompliant / $totalGUI)))"
$report += "[CRITICAL] Non-compliant: $nonCompliant"
$report += "[WARNING] Suspicious: $suspicious"
$report += ""

# Priority fixes
if ($nonCompliant -gt 0) {
    $report += "="*60
    $report += "PRIORITY FIXES REQUIRED"
    $report += "="*60
    $report += "1. Fix CRITICAL issues first (blocking thread with getSync())"
    $report += "2. Migrate public constructors to private"
    $report += "3. Add 'public static void open(Player)' with async pattern"
    $report += "4. Wrap GUI open in FoliaScheduler.runTask()"
    $report += ""
    $report += "Example async pattern:"
    $report += "  private MyGUI(Player player, TownData town) {"
    $report += "      super(player, tanPlayer, Lang.TITLE, 3);"
    $report += "  }"
    $report += ""
    $report += "  public static void open(Player player) {"
    $report += "      PlayerDataStorage.getInstance().get(player)"
    $report += "          .thenCompose(tanPlayer -> TownDataStorage.getInstance().get(townId)"
    $report += "              .thenApply(town -> new Object[]{tanPlayer, town}))"
    $report += "          .thenAccept(data -> {"
    $report += "              FoliaScheduler.runTask(plugin, player.getLocation(), () -> {"
    $report += "                  new MyGUI(player, (TownData)data[1]).open();"
    $report += "              });"
    $report += "          });"
    $report += "  }"
    $report += ""
}

# Write report
$report | Out-File -FilePath $reportFile -Encoding UTF8
Write-Host "`n[OK] Report generated: $reportFile`n" -ForegroundColor Green

# Display summary in console
Write-Host "="*60 -ForegroundColor Yellow
Write-Host "SUMMARY" -ForegroundColor Yellow
Write-Host "="*60 -ForegroundColor Yellow
Write-Host "Total GUI files: $totalGUI"
Write-Host "[OK] Async compliant: $asyncCompliant ($('{0:P0}' -f ($asyncCompliant / $totalGUI)))" -ForegroundColor Green
Write-Host "[CRITICAL] Non-compliant: $nonCompliant" -ForegroundColor Red
Write-Host "[WARNING] Suspicious: $suspicious" -ForegroundColor Yellow
Write-Host ""

if ($nonCompliant -gt 0) {
    Write-Host "ACTION REQUIRED: $nonCompliant GUI files need async migration!" -ForegroundColor Red
    Write-Host "See $reportFile for details.`n" -ForegroundColor Red
} else {
    Write-Host "All GUI files are async compliant!`n" -ForegroundColor Green
}
