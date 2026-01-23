# Nexo Integration - Configuration Guide

## Quick Start

The plugin now supports **config-based Nexo integration**. This means you can fix Nexo compatibility issues without waiting for a plugin update!

---

## What Changed?

### Before (Hardcoded)
```kotlin
// Had to recompile plugin to change this
private const val NEXO_ITEMS_CLASS = "com.nexomc.nexo.api.NexoItems"
```

### After (Config-Based)
```yaml
# Just edit config.yml and reload!
nexo:
  class-paths:
    items: "com.nexomc.nexo.api.NexoItems"
```

---

## Configuration

Edit `plugins/Coconation/config.yml`:

```yaml
nexo:
  # Enable/disable Nexo integration
  enabled: true

  # Try to auto-detect Nexo API (recommended: true)
  auto-detect: true

  # Fallback material when Nexo unavailable
  fallback-material: "BARRIER"

  # ONLY change these if Nexo API changes!
  class-paths:
    items: "com.nexomc.nexo.api.NexoItems"
    blocks: "com.nexomc.nexo.api.NexoBlocks"
    furniture: "com.nexomc.nexo.api.NexoFurniture"

  # ONLY change these if Nexo methods change!
  method-names:
    item-from-id: "itemFromId"
    item-exists: "exists"
    id-from-item: "idFromItem"
```

---

## Troubleshooting

### Problem: "ClassNotFoundException: com.nexomc.nexo.api.NexoItems"

**Cause**: Nexo changed package structure

**Solution**:
1. Check Nexo's changelog for new package names
2. Update `config.yml`:
   ```yaml
   nexo:
     class-paths:
       items: "com.nexomc.nexo.NexoItems"  # New path
   ```
3. Run `/tan-reload` (or restart server)

### Problem: "NoSuchMethodException: itemFromId"

**Cause**: Nexo renamed a method

**Solution**:
1. Check Nexo's JavaDoc/GitHub
2. Update `config.yml`:
   ```yaml
   nexo:
     method-names:
       item-from-id: "getItem"  # New method name
   ```
3. Run `/tan-reload`

### Problem: Nexo 1.17+ on old Paper (1.20.x)

**Error**: `NoClassDefFoundError: io.papermc.paper.datacomponent.*`

**Solution**:
- **Option A**: Upgrade server to Paper 1.21.11+
- **Option B**: Downgrade Nexo to 1.16.x

---

## Testing

### Check Current Configuration

```bash
/tandebug nexo checkversion
```

Output:
```
[NEXO Config] Configuration:
[NEXO Config]   Enabled: true
[NEXO Config]   Auto-detect: true
[NEXO Config]   Fallback: BARRIER
[NEXO Config]   Custom config: false
```

### Test Item Loading

```bash
/tandebug nexo getitem <item_id>
```

Example:
```bash
/tandebug nexo getitem nexo:example_item
```

---

## Example Scenarios

### Scenario 1: Nexo 1.16.x on Paper 1.20.4

**Status**: ✅ Works out of the box

No config changes needed. The plugin will auto-detect and use correct API.

### Scenario 2: Nexo 1.17.0 on Paper 1.21.11+

**Status**: ✅ Works out of the box

No config changes needed. Data components are auto-detected.

### Scenario 3: Nexo Changed API (Hypothetical Future)

Let's say Nexo 2.0 changes everything:

**Old (Nexo 1.x)**:
```yaml
class-paths:
  items: "com.nexomc.nexo.api.NexoItems"
```

**New (Nexo 2.0)**:
```yaml
class-paths:
  items: "io.nexo.api.ItemManager"
```

**Steps**:
1. Download Nexo 2.0
2. Update `config.yml` with new paths
3. Restart server
4. No plugin update needed!

---

## Advanced: Disable Nexo Integration

If you don't use Nexo items, you can disable it completely:

```yaml
nexo:
  enabled: false
  fallback-material: "BARRIER"
```

All `nexo:` icons will use the fallback material (BARRIER by default).

---

## Get Help

If you're stuck:

1. **Check console logs** for `[NEXO]` messages
2. **Run** `/tandebug nexo checkversion`
3. **Read** the full guide: `docs/NEXO_INTEGRATION_GUIDE.md`
4. **Ask** on GitHub Issues with:
   - Nexo version
   - Paper/Folia version
   - Error logs
   - Your config.yml (nexo section)

---

## Summary

✅ **No more plugin updates** when Nexo changes API
✅ **Fix it yourself** by editing config.yml
✅ **Zero downtime** - just reload
✅ **Backwards compatible** - old configs still work

**The power is in your hands!** 🚀
