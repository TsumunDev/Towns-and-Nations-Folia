# Nexo Integration - Version-Independent Approach

**Problem**: Nexo plugin API changes frequently, requiring constant maintenance.

**Solution**: Make integration completely version-agnostic using reflection and fallbacks.

---

## Current Implementation (Already Good)

The current `NexoIntegration.kt` already uses:

1. **Reflection** for all Nexo API calls
2. **Method fallbacks** (tries `get()`, `getItem()`, `asItem()`, `build()`, `toBukkitItem()`)
3. **Version detection** with graceful degradation
4. **Cache** for items and not-found IDs

## What's Still Breaking

The main issue is **class dependencies** when Nexo changes its internal structure:

```kotlin
// These change with every Nexo version
private const val NEXO_ITEMS_CLASS = "com.nexomc.nexo.api.NexoItems"
private const val NEXO_BLOCKS_CLASS = "com.nexomc.nexo.api.NexoBlocks"
```

## Proposed Solution: Dynamic Class Discovery

Instead of hardcoding class names, **search the plugin's JAR** for the classes:

```kotlin
private fun findNexoClass(nexoPlugin: Plugin): Class<*>? {
    try {
        // Try to access plugin's classloader
        val pluginClassLoader = nexoPlugin.javaClass.classLoader

        // Try known class paths
        val knownPaths = listOf(
            "com.nexomc.nexo.api.NexoItems",
            "com.nexomc.nexo.NexoItems",
            "com.nexomc.nexo.items.ItemManager"
        )

        for (path in knownPaths) {
            try {
                return Class.forName(path, false, pluginClassLoader)
            } catch (e: ClassNotFoundException) {
                // Try next path
            }
        }

        // Last resort: scan JAR for class names
        return scanJarForClass(nexoPlugin, "NexoItems")
    } catch (e: Exception) {
        return null
    }
}

private fun scanJarForClass(plugin: Plugin, className: String): Class<*>? {
    // Use reflection to access plugin's file path
    // Then scan JAR for classes matching pattern
    // This is advanced - see implementation below
}
```

## Recommended: Plugin.yml Soft-Depend

Instead of hard dependency, use **soft-depend**:

```yaml
name: Coconation
version: 2.0.0
softdepend: [Nexo]
```

Then in code, check if Nexo is available:

```kotlin
if (Bukkit.getPluginManager().getPlugin("Nexo") != null) {
    // Enable Nexo features
} else {
    // Use fallback materials (already implemented!)
}
```

## Configuration-Based Version Mapping

Add to `config.yml`:

```yaml
nexo:
  enabled: true
  version-auto-detect: true
  fallback-material: BARRIER
  class-paths:
    items: "com.nexomc.nexo.api.NexoItems"
    blocks: "com.nexomc.nexo.api.NexoBlocks"
    furniture: "com.nexomc.nexo.api.NexoFurniture"
  method-names:
    get-item: "itemFromId"
    item-exists: "exists"
    get-id: "idFromItem"
```

Then read from config instead of hardcoding:

```kotlin
object NexoIntegration {
    private val config = TownsAndNations.getPlugin().config

    private fun getClassName(type: String): String {
        return config.getString("nexo.class-paths.$type") ?: ""
    }

    private fun loadNexoClasses() {
        nexoItemsClass = Class.forName(getClassName("items"))
        // ...
    }
}
```

## Benefits of Config Approach

✅ **Zero code changes** when Nexo updates - just edit config.yml
✅ **Server admins** can fix compatibility themselves
✅ **No recompilation** needed
✅ **Easy testing** of different Nexo versions

## Implementation Steps

### Step 1: Add Config Defaults

```yaml
# config.yml
nexo:
  # Automatically detect Nexo version
  auto-detect: true

  # Enable Nexo integration (false = always use fallbacks)
  enabled: true

  # Fallback material when Nexo is unavailable
  fallback-material: BARRIER

  # Custom class paths (only change if Nexo API changes)
  class-paths:
    items: "com.nexomc.nexo.api.NexoItems"
    blocks: "com.nexomc.nexo.api.NexoBlocks"
    furniture: "com.nexomc.nexo.api.NexoFurniture"

  # Custom method names (only change if Nexo API changes)
  method-names:
    item-from-id: "itemFromId"
    item-exists: "exists"
    id-from-item: "idFromItem"
    place-block: "place"
    remove-block: "remove"
    is-custom-block: "isCustomBlock"
```

### Step 2: Update NexoIntegration.kt

Replace hardcoded constants with config values:

```kotlin
object NexoIntegration {
    private val config = TownsAndNations.getPlugin().config
    private val logger = LoggerFactory.getLogger(NexoIntegration::class.java)

    private fun getClassPath(type: String): String {
        return config.getString("nexo.class-paths.$type")
            ?: throw IllegalStateException("Nexo class path '$type' not configured")
    }

    private fun getMethodName(method: String): String {
        return config.getString("nexo.method-names.$method")
            ?: throw IllegalStateException("Nexo method '$method' not configured")
    }

    private fun loadNexoClasses() {
        nexoItemsClass = try {
            Class.forName(getClassPath("items"))
        } catch (e: ClassNotFoundException) {
            logger.warn("[NEXO] Items class not found: {}", getClassPath("items"))
            throw e
        }
        // ... same for blocks, furniture
    }

    private fun loadNexoMethods() {
        val itemsClass = nexoItemsClass ?: throw ClassNotFoundException("NexoItems class not loaded")

        itemFromIdMethod = itemsClass.getDeclaredMethod(
            getMethodName("item-from-id"),
            String::class.java
        )
        itemExistsMethod = itemsClass.getDeclaredMethod(
            getMethodName("item-exists"),
            String::class.java
        )
        // ... same for other methods
    }
}
```

### Step 3: Add Reload Command

Allow server admins to reload Nexo config without restarting:

```kotlin
command("tan-reload-nexo") {
    description = "Reload Nexo integration configuration"
    executes {
        NexoIntegration.reinitialize()
        sender.sendMessage("§aNexo integration reloaded!")
    }
}
```

### Step 4: Add Compatibility Check Command

```kotlin
command("tan-nexo-check") {
    description = "Check Nexo integration compatibility"
    executes {
        val info = NexoIntegration.getCompatibilityInfo()
        sender.sendMessage("""
            §6=== Nexo Compatibility Check ===
            §eNexo Version: §f${info.nexoVersion ?: "Not installed"}
            §eIntegration Enabled: §f${info.isEnabled}
            §ePaper Version: §f${info.paperVersion}
            §eModern Paper: §f${info.isModernPaper}
            §eCached Items: §f${info.cacheSize}
            §6==================================
        """.trimIndent())
    }
}
```

---

## Testing Strategy

### Test 1: Without Nexo

```bash
# Remove Nexo plugin
rm plugins/Nexo*.jar

# Start server
# Should see: "[NEXO] Plugin not installed - nexo: icons will use fallback materials"
```

### Test 2: With Old Nexo (1.16.x)

```bash
# Download Nexo 1.16.1
wget https://github.com/NexoMC/Nexo/releases/download/v1.16.1/Nexo-1.16.1.jar

# Start server
# Should work with Paper 1.20.x
```

### Test 3: With New Nexo (1.17.x)

```bash
# Download Nexo 1.17.0
wget https://github.com/NexoMC/Nexo/releases/download/v1.17.0/Nexo-1.17.0.jar

# Update config.yml if needed:
# nexo.class-paths.items: "com.nexomc.nexo.api.NexoItems"  # New path

# Start server with Paper 1.21.11+
# Should work with data components
```

---

## Troubleshooting

### Error: "ClassNotFoundException"

**Cause**: Nexo changed package structure

**Solution**:
1. Check Nexo's changelog for new package names
2. Update `config.yml` → `nexo.class-paths.*`
3. Run `/tan-reload-nexo`

### Error: "NoSuchMethodException"

**Cause**: Nexo renamed a method

**Solution**:
1. Check Nexo's JavaDoc/GitHub for new method name
2. Update `config.yml` → `nexo.method-names.*`
3. Run `/tan-reload-nexo`

### Error: "NoClassDefFoundError: io.papermc.paper.datacomponent.*"

**Cause**: Using Nexo 1.17+ on old Paper

**Solution**:
- Downgrade Nexo to 1.16.x OR
- Upgrade server to Paper 1.21.11+

---

## Migration Guide for Server Admins

When updating Nexo:

1. **Stop server**
2. **Update Nexo JAR** in `plugins/`
3. **Start server** and check console for warnings
4. **If errors appear**:
   - Check `[NEXO]` logs in console
   - Update `config.yml` with new class/method names
   - Run `/tan-reload-nexo`
5. **Test** with `/tandebug nexo checkversion`

---

## Conclusion

With **config-based class discovery**:

✅ Zero maintenance when Nexo updates
✅ Server admins can fix compatibility themselves
✅ No plugin recompilation needed
✅ Graceful fallback to vanilla materials
✅ Easy testing and debugging

**This approach works for ANY optional dependency!**
