# Java Flight Recorder (JFR) Profiling Script for Towns & Nations
# Purpose: Capture 60-second performance profile with focus on async operations
# Usage: .\start-jfr-profiling.ps1 -ServerJar "path/to/server.jar" [-Duration 60]

param(
    [Parameter(Mandatory=$true)]
    [string]$ServerJar,
    
    [Parameter(Mandatory=$false)]
    [int]$Duration = 60,
    
    [Parameter(Mandatory=$false)]
    [string]$OutputDir = ".\jfr-recordings"
)

$ErrorActionPreference = "Stop"

# Create output directory
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$jfrFile = Join-Path $OutputDir "tan-profile-$timestamp.jfr"

Write-Host "🚀 Starting Java Flight Recorder profiling..." -ForegroundColor Green
Write-Host "📊 Duration: $Duration seconds" -ForegroundColor Cyan
Write-Host "📁 Output: $jfrFile" -ForegroundColor Cyan

# JFR configuration optimized for async operations
$jfrSettings = @(
    "jdk.ExecutionSample:enabled=true:period=10ms",  # CPU sampling every 10ms
    "jdk.ObjectAllocationSample:enabled=true:throttle=150/s",  # Memory allocations
    "jdk.ThreadPark:enabled=true:threshold=10ms",  # Thread parking (CompletableFuture waits)
    "jdk.JavaMonitorWait:enabled=true:threshold=10ms",  # Monitor waits
    "jdk.FileRead:enabled=true:threshold=10ms",  # File I/O
    "jdk.FileWrite:enabled=true:threshold=10ms",
    "jdk.SocketRead:enabled=true:threshold=10ms",  # Network I/O
    "jdk.SocketWrite:enabled=true:threshold=10ms",
    "jdk.ThreadSleep:enabled=true:threshold=10ms",  # Thread sleeps
    "jdk.JavaExceptionThrow:enabled=true:stackTrace=true",  # Exceptions
    "jdk.GarbageCollection:enabled=true",  # GC events
    "jdk.ThreadStart:enabled=true",  # Thread lifecycle
    "jdk.ThreadEnd:enabled=true"
)

$jfrOptions = "-XX:StartFlightRecording=duration=${Duration}s,filename=$jfrFile,settings=profile"

Write-Host "`n⚙️  JFR Settings:" -ForegroundColor Yellow
Write-Host "   - CPU Sampling: 10ms intervals"
Write-Host "   - Object Allocations: 150/s throttle"
Write-Host "   - I/O Threshold: 10ms"
Write-Host "   - Thread events: parking, waits, sleeps"
Write-Host "   - Exception tracking: enabled with stack traces"

Write-Host "`n📝 JFR command line option:" -ForegroundColor Yellow
Write-Host "   $jfrOptions" -ForegroundColor Gray

Write-Host "`n💡 To start server with JFR, run:" -ForegroundColor Magenta
Write-Host "   java $jfrOptions -jar $ServerJar nogui" -ForegroundColor White

Write-Host "`n⏳ After recording completes, analyze with:" -ForegroundColor Magenta
Write-Host "   jcmd <PID> JFR.dump filename=$jfrFile" -ForegroundColor White
Write-Host "   .\analyze-jfr.ps1 -JfrFile `"$jfrFile`"" -ForegroundColor White

Write-Host "`n📊 View in JDK Mission Control:" -ForegroundColor Magenta
Write-Host "   jmc $jfrFile" -ForegroundColor White

Write-Host "`n🔥 Generate flame graph:" -ForegroundColor Magenta
Write-Host "   .\generate-flamegraph.ps1 -JfrFile `"$jfrFile`"" -ForegroundColor White

# Save JFR command to file for easy reference
$commandFile = Join-Path $OutputDir "jfr-command-$timestamp.txt"
$fullCommand = "java $jfrOptions -Xms4G -Xmx4G -jar $ServerJar nogui"
Set-Content -Path $commandFile -Value $fullCommand

Write-Host "`n✅ JFR command saved to: $commandFile" -ForegroundColor Green
Write-Host "`n🎯 Hot paths to monitor:" -ForegroundColor Yellow
Write-Host "   - BlockBreakEvent handler (target: <5ms)" -ForegroundColor Gray
Write-Host "   - PlayerInteractEvent handler (target: <5ms)" -ForegroundColor Gray
Write-Host "   - GUI open operations (target: <10ms)" -ForegroundColor Gray
Write-Host "   - PermissionService.canPlayerDoAction (should be async)" -ForegroundColor Gray
Write-Host "   - PlayerDataStorage.get() cache hits (should be >95%)" -ForegroundColor Gray

Write-Host "`n📈 Key metrics to validate:" -ForegroundColor Cyan
Write-Host "   ✓ Chunk permission checks: <5ms p95"
Write-Host "   ✓ GUI open latency: <10ms p95"
Write-Host "   ✓ Command execution: <10ms p95"
Write-Host "   ✓ Cache hit rate: >95%"
Write-Host "   ✓ Database queries: <50ms p95"
Write-Host "   ✓ Zero blocking on CompletableFuture.join() in hot paths"

Write-Host "`n🎮 Start the server and perform these actions during the $Duration-second recording:" -ForegroundColor Yellow
Write-Host "   1. Break/place 50+ blocks in claimed chunks"
Write-Host "   2. Open town/region GUI menus 10+ times"
Write-Host "   3. Run /coconation commands (info, claim, unclaim)"
Write-Host "   4. Interact with chests, doors, buttons in claimed territory"
Write-Host "   5. Trigger property operations (buy/sell/rent)"

Write-Host "`n✨ Ready to start profiling!`n" -ForegroundColor Green
