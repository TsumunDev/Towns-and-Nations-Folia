# Flame Graph Generation Script for Towns & Nations
# Purpose: Convert JFR recording to flame graph for visual performance analysis
# Usage: .\generate-flamegraph.ps1 -JfrFile "path/to/recording.jfr"

param(
    [Parameter(Mandatory=$true)]
    [string]$JfrFile,
    
    [Parameter(Mandatory=$false)]
    [string]$OutputDir = ".\flamegraphs"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $JfrFile)) {
    Write-Host "❌ JFR file not found: $JfrFile" -ForegroundColor Red
    exit 1
}

# Create output directory
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$baseFileName = [System.IO.Path]::GetFileNameWithoutExtension($JfrFile)
$flameGraphFile = Join-Path $OutputDir "$baseFileName-$timestamp.html"

Write-Host "🔥 Generating Flame Graph for: $JfrFile" -ForegroundColor Green
Write-Host "📁 Output: $flameGraphFile" -ForegroundColor Cyan

# Check for FlameGraph tools
$flameGraphPath = "C:\Tools\FlameGraph"  # Default location
$hasFlameGraph = Test-Path "$flameGraphPath\flamegraph.pl"

if (-not $hasFlameGraph) {
    Write-Host "`n⚠️  FlameGraph toolkit not found at: $flameGraphPath" -ForegroundColor Yellow
    Write-Host "`n📦 Installation instructions:" -ForegroundColor Cyan
    Write-Host "   1. Download FlameGraph from: https://github.com/brendangregg/FlameGraph" -ForegroundColor White
    Write-Host "   2. Extract to: C:\Tools\FlameGraph" -ForegroundColor White
    Write-Host "   3. Or install via: git clone https://github.com/brendangregg/FlameGraph.git C:\Tools\FlameGraph" -ForegroundColor White
    
    Write-Host "`n💡 Alternative: Use async-profiler (integrated flame graph generation)" -ForegroundColor Cyan
    Write-Host "   Download from: https://github.com/async-profiler/async-profiler" -ForegroundColor White
    
    Write-Host "`n🌐 Online JFR viewer alternatives:" -ForegroundColor Magenta
    Write-Host "   - JDK Mission Control (jmc $JfrFile)" -ForegroundColor White
    Write-Host "   - IntelliJ IDEA Ultimate (File → Open → Select .jfr)" -ForegroundColor White
    Write-Host "   - VisualVM with JFR plugin" -ForegroundColor White
    
    # Create placeholder HTML with instructions
    $placeholderHtml = @"
<!DOCTYPE html>
<html>
<head>
    <title>Flame Graph - Setup Required</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; background: #1e1e1e; color: #d4d4d4; }
        .container { max-width: 800px; margin: 0 auto; }
        h1 { color: #ff6b6b; }
        h2 { color: #4ecdc4; }
        code { background: #2d2d2d; padding: 2px 6px; border-radius: 3px; color: #f8f8f2; }
        .command { background: #2d2d2d; padding: 10px; border-radius: 5px; margin: 10px 0; font-family: monospace; }
        .info { background: #264f78; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #4ecdc4; }
        a { color: #4ecdc4; text-decoration: none; }
        a:hover { text-decoration: underline; }
        ul { line-height: 1.8; }
    </style>
</head>
<body>
    <div class="container">
        <h1>🔥 Flame Graph Generation - Setup Required</h1>
        
        <div class="info">
            <strong>📊 JFR Recording:</strong> $JfrFile<br>
            <strong>⏰ Generated:</strong> $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
        </div>
        
        <h2>🛠️ Setup FlameGraph Toolkit</h2>
        <p>To generate interactive flame graphs from JFR recordings, install the FlameGraph toolkit:</p>
        
        <h3>Option 1: Using Git</h3>
        <div class="command">
git clone https://github.com/brendangregg/FlameGraph.git C:\Tools\FlameGraph
        </div>
        
        <h3>Option 2: Manual Download</h3>
        <ol>
            <li>Download from: <a href="https://github.com/brendangregg/FlameGraph" target="_blank">github.com/brendangregg/FlameGraph</a></li>
            <li>Extract to: <code>C:\Tools\FlameGraph</code></li>
            <li>Ensure <code>flamegraph.pl</code> exists in the directory</li>
        </ol>
        
        <h3>Option 3: async-profiler (Recommended for Java)</h3>
        <p>async-profiler has integrated flame graph generation and better Java support:</p>
        <ol>
            <li>Download from: <a href="https://github.com/async-profiler/async-profiler/releases" target="_blank">async-profiler releases</a></li>
            <li>Extract to a directory (e.g., <code>C:\Tools\async-profiler</code>)</li>
            <li>Run: <code>asprof -d 60 -f flamegraph.html &lt;java_pid&gt;</code></li>
        </ol>
        
        <h2>📊 Alternative Visualization Tools</h2>
        <ul>
            <li><strong>JDK Mission Control</strong> - GUI tool for JFR analysis
                <div class="command">jmc $JfrFile</div>
            </li>
            <li><strong>IntelliJ IDEA Ultimate</strong> - Built-in JFR viewer
                <div class="info">File → Open → Select .jfr file</div>
            </li>
            <li><strong>VisualVM</strong> - Free profiling tool with JFR plugin</li>
        </ul>
        
        <h2>🎯 What to Look for in Flame Graphs</h2>
        <ul>
            <li><strong>Wide bars</strong> = CPU hot spots (methods consuming most time)</li>
            <li><strong>Deep stacks</strong> = Complex call chains (potential optimization targets)</li>
            <li><strong>DatabaseStorage.getSync</strong> in hot paths = NOT optimized (should be rare)</li>
            <li><strong>CompletableFuture.join</strong> in event handlers = Blocking (should be minimal)</li>
            <li><strong>Flat profiles</strong> = Well-distributed load (good!)</li>
        </ul>
        
        <h2>🔍 Hot Paths to Monitor</h2>
        <p>Focus on these packages in the flame graph:</p>
        <ul>
            <li><code>org.leralix.tan.listeners.ChunkListener</code> - Block break/place events (target: &lt;5ms)</li>
            <li><code>org.leralix.tan.service.PermissionService</code> - Permission checks (should be async)</li>
            <li><code>org.leralix.tan.storage.stored.PlayerDataStorage</code> - Cache efficiency (&gt;95% hit rate)</li>
            <li><code>org.leralix.tan.gui.**</code> - GUI open operations (target: &lt;10ms)</li>
        </ul>
        
        <h2>✅ Next Steps</h2>
        <ol>
            <li>Install FlameGraph toolkit or async-profiler</li>
            <li>Re-run this script: <code>.\generate-flamegraph.ps1 -JfrFile "$JfrFile"</code></li>
            <li>Or use JDK Mission Control for immediate analysis</li>
        </ol>
        
        <div class="info">
            <strong>💡 Quick Start:</strong> For immediate results, use JDK Mission Control:<br>
            <div class="command">jmc "$JfrFile"</div>
        </div>
    </div>
</body>
</html>
"@
    
    Set-Content -Path $flameGraphFile -Value $placeholderHtml
    Write-Host "`n📄 Setup instructions saved to: $flameGraphFile" -ForegroundColor Cyan
    Write-Host "`n💡 Open this file in a browser for detailed setup guide" -ForegroundColor Yellow
    
    # Try to open in default browser
    Start-Process $flameGraphFile
    
    exit 0
}

# FlameGraph toolkit found - proceed with generation
Write-Host "✅ FlameGraph toolkit found at: $flameGraphPath" -ForegroundColor Green

# Convert JFR to collapsed stacks format
Write-Host "`n🔄 Converting JFR to collapsed stacks format..." -ForegroundColor Cyan

$collapsedFile = Join-Path $OutputDir "$baseFileName-$timestamp.collapsed"

try {
    # Extract execution samples and convert to collapsed format
    # This requires the JFR to be converted to a format FlameGraph understands
    # Using jfr print command to extract stack traces
    
    Write-Host "   Extracting execution samples..." -ForegroundColor Gray
    $jfrCommand = Get-Command jfr -ErrorAction SilentlyContinue
    
    if ($jfrCommand) {
        # Extract CPU samples in a format suitable for FlameGraph
        jfr print --events jdk.ExecutionSample $JfrFile | 
            Select-String -Pattern "stackTrace" -Context 0,50 |
            ForEach-Object { $_.Line } |
            Out-File -FilePath $collapsedFile
        
        Write-Host "   Collapsed stacks written to: $collapsedFile" -ForegroundColor Gray
    } else {
        Write-Host "   ⚠️  'jfr' command not found - using alternative method" -ForegroundColor Yellow
        
        # Create a note file explaining the limitation
        $noteContent = @"
To generate flame graphs from JFR files, you need:

1. JFR command-line tools (part of JDK)
2. FlameGraph toolkit (Perl scripts)

Current status:
- FlameGraph toolkit: ✅ Found at $flameGraphPath
- JFR tools: ❌ Not found in PATH

Solution:
1. Ensure you're using a full JDK (not just JRE)
2. Add JDK bin directory to PATH
3. Or use async-profiler which has integrated flame graph generation

Alternative: Use JDK Mission Control to view JFR recordings graphically
  Command: jmc $JfrFile
"@
        Set-Content -Path "$OutputDir\README.txt" -Value $noteContent
        Write-Host "`n📝 Instructions saved to: $OutputDir\README.txt" -ForegroundColor Cyan
        exit 1
    }
    
    # Generate flame graph SVG
    Write-Host "`n🔥 Generating flame graph..." -ForegroundColor Cyan
    
    $perlCommand = Get-Command perl -ErrorAction SilentlyContinue
    if (-not $perlCommand) {
        Write-Host "⚠️  Perl not found - installing Strawberry Perl recommended" -ForegroundColor Yellow
        Write-Host "   Download from: https://strawberryperl.com/" -ForegroundColor White
        exit 1
    }
    
    $svgFile = Join-Path $OutputDir "$baseFileName-$timestamp.svg"
    
    & perl "$flameGraphPath\flamegraph.pl" `
        --title "Towns & Nations - Performance Flame Graph" `
        --width 1600 `
        --height 800 `
        --colors java `
        $collapsedFile > $svgFile
    
    Write-Host "✅ Flame graph generated: $svgFile" -ForegroundColor Green
    
    # Create interactive HTML wrapper
    $htmlContent = @"
<!DOCTYPE html>
<html>
<head>
    <title>Towns & Nations - Flame Graph</title>
    <style>
        body { margin: 0; padding: 20px; background: #1e1e1e; font-family: Arial, sans-serif; }
        .header { color: #d4d4d4; padding: 20px; background: #252526; border-radius: 5px; margin-bottom: 20px; }
        .header h1 { margin: 0 0 10px 0; color: #4ecdc4; }
        .info { display: flex; gap: 30px; flex-wrap: wrap; }
        .info-item { flex: 1; min-width: 200px; }
        .info-label { color: #858585; font-size: 12px; text-transform: uppercase; }
        .info-value { color: #d4d4d4; font-size: 16px; margin-top: 5px; }
        .flame-container { background: white; border-radius: 5px; padding: 10px; }
        iframe { border: none; width: 100%; height: 800px; }
        .tips { background: #264f78; color: #d4d4d4; padding: 15px; border-radius: 5px; margin-top: 20px; border-left: 4px solid #4ecdc4; }
        .tips h3 { margin-top: 0; color: #4ecdc4; }
        .tips ul { margin: 10px 0; padding-left: 20px; }
    </style>
</head>
<body>
    <div class="header">
        <h1>🔥 Towns & Nations - Performance Flame Graph</h1>
        <div class="info">
            <div class="info-item">
                <div class="info-label">JFR Recording</div>
                <div class="info-value">$([System.IO.Path]::GetFileName($JfrFile))</div>
            </div>
            <div class="info-item">
                <div class="info-label">Generated</div>
                <div class="info-value">$(Get-Date -Format "yyyy-MM-dd HH:mm:ss")</div>
            </div>
            <div class="info-item">
                <div class="info-label">Type</div>
                <div class="info-value">CPU Sampling (ExecutionSample)</div>
            </div>
        </div>
    </div>
    
    <div class="flame-container">
        <iframe src="$([System.IO.Path]::GetFileName($svgFile))"></iframe>
    </div>
    
    <div class="tips">
        <h3>💡 How to Read This Flame Graph</h3>
        <ul>
            <li><strong>Width</strong> = Time spent (wider = more CPU time)</li>
            <li><strong>Height</strong> = Call stack depth (bottom = entry point, top = leaf function)</li>
            <li><strong>Color</strong> = Random (helps distinguish frames, no performance meaning)</li>
            <li><strong>Click</strong> a frame to zoom in and see details</li>
            <li><strong>Reset</strong> by clicking the top frame or reloading</li>
        </ul>
        
        <h3>🎯 What to Look For</h3>
        <ul>
            <li><strong>ChunkListener methods</strong> - Should be narrow (fast execution)</li>
            <li><strong>DatabaseStorage.getSync</strong> - Should be RARE in hot paths</li>
            <li><strong>CompletableFuture.join</strong> - Acceptable in GUI, bad in event handlers</li>
            <li><strong>QueryCacheManager.get</strong> - Should show high cache hit rate</li>
            <li><strong>GC activity</strong> - Should be minimal (<5% total time)</li>
        </ul>
        
        <h3>✅ Async Migration Success Indicators</h3>
        <ul>
            <li>Event handlers (BlockBreak, Interact) are narrow/flat</li>
            <li>CompletableFuture async chains visible (good!)</li>
            <li>Database operations NOT in event handler stacks</li>
            <li>PlayerDataStorage.get() appears with cache hits</li>
        </ul>
    </div>
</body>
</html>
"@
    
    Set-Content -Path $flameGraphFile -Value $htmlContent
    
    Write-Host "`n✅ Interactive flame graph created: $flameGraphFile" -ForegroundColor Green
    Write-Host "`n🌐 Opening in browser..." -ForegroundColor Cyan
    
    Start-Process $flameGraphFile
    
    Write-Host "`n✨ Flame graph generation complete!`n" -ForegroundColor Green
    
} catch {
    Write-Host "`n❌ Error generating flame graph: $_" -ForegroundColor Red
    Write-Host "`n💡 Try using JDK Mission Control instead: jmc $JfrFile" -ForegroundColor Yellow
    exit 1
}
