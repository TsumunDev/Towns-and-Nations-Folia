# 🚀 Performance Profiling Guide - Towns & Nations

This directory contains automated scripts for profiling Towns & Nations plugin performance using **Java Flight Recorder (JFR)** and **flame graphs**.

---

## 📋 Quick Start

### 1. Start JFR Profiling (60-second recording)

```powershell
.\start-jfr-profiling.ps1 -ServerJar "path/to/folia-1.20.1.jar" -Duration 60
```

This generates the JFR command you need to start your server with profiling enabled.

### 2. During Recording - Perform These Actions

To capture meaningful performance data, execute these operations during the 60-second recording:

**Hot Path Operations (most important):**
- ✅ Break 50+ blocks in claimed chunks (tests chunk permission checks)
- ✅ Place 50+ blocks in claimed chunks
- ✅ Open chests, doors, buttons in claimed territory (interaction permissions)
- ✅ Open town/region GUI menus 10+ times (GUI async loading)
- ✅ Run commands: `/coconation info`, `/coconation claim`, `/coconation unclaim`

**Property Operations:**
- Buy/sell/rent properties
- Open property management GUI

**Territory Operations:**
- Create/delete territories
- Manage ranks and permissions
- Send proposals (vassals, diplomacy)

### 3. Analyze Results

After recording completes, analyze with:

```powershell
# Extract performance metrics
.\analyze-jfr.ps1 -JfrFile ".\jfr-recordings\tan-profile-YYYY-MM-DD_HH-mm-ss.jfr"

# Generate interactive flame graph
.\generate-flamegraph.ps1 -JfrFile ".\jfr-recordings\tan-profile-YYYY-MM-DD_HH-mm-ss.jfr"

# Or use JDK Mission Control GUI
jmc ".\jfr-recordings\tan-profile-YYYY-MM-DD_HH-mm-ss.jfr"
```

---

## 🎯 Performance Validation Targets

After async migration, we expect these metrics:

| Metric | Target | Priority |
|--------|--------|----------|
| Chunk permission checks | <5ms p95 | 🔴 CRITICAL |
| GUI open operations | <10ms p95 | 🔴 CRITICAL |
| Command execution | <10ms p95 | 🟡 HIGH |
| Cache hit rate | >95% | 🟡 HIGH |
| Database queries | <50ms p95 | 🟢 MEDIUM |
| No blocking in event handlers | Zero `getSync()` in hot paths | 🔴 CRITICAL |

---

## 📊 Scripts Overview

### `start-jfr-profiling.ps1`
**Purpose:** Generate JFR recording command with optimized settings for async analysis.

**Features:**
- CPU sampling (10ms intervals)
- Object allocation tracking (150/s throttle)
- Thread parking events (CompletableFuture waits)
- I/O operations (threshold: 10ms)
- Exception tracking with stack traces
- GC monitoring

**Usage:**
```powershell
.\start-jfr-profiling.ps1 -ServerJar "server.jar" -Duration 60 -OutputDir ".\recordings"
```

**Output:**
- JFR command line to start server with profiling
- Recording saved to `jfr-recordings\tan-profile-<timestamp>.jfr`

---

### `analyze-jfr.ps1`
**Purpose:** Extract key performance metrics from JFR recording.

**Extracts:**
- Top 20 hot methods (CPU time)
- Object allocation hotspots
- Thread park events (blocking operations)
- I/O statistics
- GC overhead
- Exception throw rate

**Usage:**
```powershell
.\analyze-jfr.ps1 -JfrFile ".\jfr-recordings\tan-profile-*.jfr" -OutputDir ".\analysis"
```

**Output:**
- `cpu-samples.txt` - Hot method analysis
- `allocations.txt` - Memory allocation hotspots
- `thread-parks.txt` - CompletableFuture waits
- `io-operations.txt` - File/network I/O
- `gc-events.txt` - Garbage collection events
- `exceptions.txt` - Exception tracking
- `jfr-report-<timestamp>.txt` - Summary report

---

### `generate-flamegraph.ps1`
**Purpose:** Generate interactive flame graph from JFR recording.

**Visualization:**
- Width = CPU time (wider = more time)
- Height = Call stack depth
- Interactive zooming and filtering

**Usage:**
```powershell
.\generate-flamegraph.ps1 -JfrFile ".\jfr-recordings\tan-profile-*.jfr"
```

**Requirements:**
- FlameGraph toolkit: https://github.com/brendangregg/FlameGraph
- OR async-profiler: https://github.com/async-profiler/async-profiler
- Perl (for FlameGraph.pl script)

**Output:**
- `flamegraphs\<name>-<timestamp>.svg` - Flame graph image
- `flamegraphs\<name>-<timestamp>.html` - Interactive viewer
- Opens automatically in default browser

---

## 🔍 What to Look For

### ✅ Good Patterns (Expected After Async Migration)

**Event Handlers:**
- `ChunkListener.onBlockBreak` - Narrow/flat profile (<5ms)
- `ChunkListener.onPlayerInteract` - Narrow/flat profile (<5ms)
- No `DatabaseStorage.getSync()` in call stacks

**Service Layer:**
- `PermissionService.canPlayerDoAction` - Returns CompletableFuture immediately
- `PlayerDataStorage.get()` - Cache hits visible (Guava/Redis)
- Async chains: `CompletableFuture.thenAccept/thenCompose`

**GUI Operations:**
- Static `open()` methods load data async
- No blocking `join()` on main thread
- Data pre-loaded before GUI construction

**Storage Layer:**
- High cache hit rate: `QueryCacheManager.get()` → cache
- Database queries batched with `CompletableFuture.allOf()`
- Minimal `getSync()` calls (only in legacy API wrappers)

---

### ❌ Red Flags (Should NOT Appear)

**Blocking Operations in Hot Paths:**
- `DatabaseStorage.getSync()` in `ChunkListener` methods
- `CompletableFuture.join()` in event handlers
- `Thread.sleep()` or `Object.wait()` in permission checks
- Sequential player loading instead of batch `allOf()`

**Performance Issues:**
- Event handler methods wider than 10ms
- GUI open operations taking >50ms
- Cache miss rate >5% for player data
- Excessive GC activity (>10% total time)

---

## 🛠️ Troubleshooting

### Issue: "jfr command not found"

**Solution:** Ensure you're using a full JDK (not JRE):
```powershell
# Check Java version
java -version

# Should show "Java(TM) SE Runtime Environment" or "OpenJDK"
# JFR tools are in JDK bin directory

# Add to PATH if needed
$env:PATH += ";C:\Program Files\Java\jdk-21\bin"
```

### Issue: "FlameGraph toolkit not found"

**Solution:** Install FlameGraph:
```powershell
# Option 1: Git clone
git clone https://github.com/brendangregg/FlameGraph.git C:\Tools\FlameGraph

# Option 2: Download ZIP from GitHub
# Extract to C:\Tools\FlameGraph
```

**Alternative:** Use **async-profiler** which has integrated flame graph generation:
```powershell
# Download from: https://github.com/async-profiler/async-profiler/releases
# Extract and run:
asprof -d 60 -f flamegraph.html <java_pid>
```

### Issue: "Perl not found"

**Solution:** Install Strawberry Perl:
```powershell
# Download from: https://strawberryperl.com/
# Or use Chocolatey:
choco install strawberryperl
```

### Issue: "Recording shows no data"

**Cause:** Server not under load during recording.

**Solution:** Perform test operations (break blocks, open GUIs, run commands) during the 60-second recording window.

---

## 📈 Baseline Performance (Pre-Async Migration)

For comparison, here are the performance metrics **before** async migration:

| Operation | Before | After (Expected) | Improvement |
|-----------|--------|------------------|-------------|
| Block Break Permission | 50-200ms | <5ms | **40x** |
| GUI Open (Town Menu) | 80-120ms | <10ms | **12x** |
| Property Purchase | 150ms | <10ms | **15x** |
| Rank Salary (50 players) | 2500ms | 200ms | **12x** |
| Cache Miss Penalty | N/A | 15-50ms | N/A |

---

## 🎯 Success Criteria

Async migration is considered successful if JFR analysis shows:

1. ✅ **Chunk permission checks <5ms p95** - Hot path optimization
2. ✅ **GUI operations <10ms p95** - Async data loading
3. ✅ **No blocking getSync() in event handlers** - Zero blocking calls
4. ✅ **Cache hit rate >95%** - Efficient caching (Guava + Redis)
5. ✅ **Database queries <50ms p95** - Connection pooling + batching
6. ✅ **CompletableFuture chains visible** - Async patterns in use

---

## 📚 Additional Resources

### JDK Mission Control (Recommended)
GUI tool for analyzing JFR recordings:
```powershell
jmc ".\jfr-recordings\tan-profile-*.jfr"
```

**Features:**
- Method profiling with call trees
- Memory allocation tracking
- Thread analysis (parking, waiting, sleeping)
- I/O operation statistics
- GC overhead visualization

### IntelliJ IDEA Ultimate
Built-in JFR viewer:
1. File → Open
2. Select `.jfr` file
3. View profiling data in dedicated tool window

### VisualVM
Free profiling tool with JFR plugin:
- Download: https://visualvm.github.io/
- Install JFR plugin from Tools → Plugins

### async-profiler
Java profiler with integrated flame graph generation:
- GitHub: https://github.com/async-profiler/async-profiler
- Supports CPU, allocations, locks
- Native stack traces (C/C++ libraries)
- Zero overhead when not profiling

---

## 🤝 Contributing

If you find performance bottlenecks or optimization opportunities:

1. **Document findings** with JFR screenshots/flame graphs
2. **Identify hot methods** with CPU time >10ms
3. **Suggest optimizations** (caching, batching, async patterns)
4. **Test changes** with before/after profiling
5. **Submit PR** with performance improvements

---

## 📝 License

These profiling scripts are part of the Towns & Nations plugin and follow the same license terms.

---

**Last Updated:** January 3, 2026  
**Async Migration Status:** ✅ Complete (46 getSync() eliminated)  
**Next Task:** Performance validation with JFR profiling
