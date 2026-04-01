# Packaging & Déploiment - Towns-and-Nations

## 📦 Vue d'Ensemble du Build

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CODE SOURCE                                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                      │
│  │   tan-api/   │  │  tan-core/   │  │  tan-vault/ │                      │
│  │  (Interfaces) │  │ (Implémentation)│ │  (Eco bridge)│                      │
│  └──────────────┘  └──────────────┘  └──────────────┘                      │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      GRADLE BUILD SYSTEM                                  │
│                                                                              │
│  plugins { id 'java' id 'kotlin' id 'com.github.johnrengelman.shadow' }   │
│                                                                              │
│  dependencies {                                                              │
│    compileOnly 'io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT'            │
│    implementation 'com.zaxxer:HikariCP:5.1.0'                               │
│    implementation 'org.redisson:redisson:3.24.0'                            │
│    ...                                                                        │
│  }                                                                            │
│                                                                              │
│  shadowJar {                                                                  │
│    relocate 'org.bstats', 'org.leralix.tan.lib.bstats'                     │
│    relocate 'com.zaxxer', 'org.leralix.tan.lib.zaxxer'                     │
│    relocate 'com.google', 'org.leralix.tan.lib.google'                     │
│    relocate 'org.slf4j', 'org.leralix.tan.lib.slf4j'                       │
│  }                                                                            │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       SHADOW JAR (OUTPUT)                                   │
│                                                                              │
│  File: TownsAndNations-{version}-shadow.jar                                │
│  Size: ~15-25 MB                                                             │
│  Contains:                                                                    │
│    ✓ Plugin code + classes                                                   │
│    ✓ All dependencies (embedded)                                              │
│    ✓ Resources (config.yml, lang/)                                           │
│    ✓ Relocated packages (no conflicts)                                      │
│                                                                              │
│  Destination: plugins/TownsAndNations.jar                                   │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 1. CONFIGURATION GRADLE

### 1.1 Modules

```gradle
settings.gradle.kts:
rootProject.name = "Towns-and-Nations-folia"
include(":tan-api")
include(":tan-core")
include(":tan-vault")
```

### 1.2 Shadow Plugin (Relocation)

**Pourquoi "relocate" les dépendances ?**

Le plugin embarque ses propres versions de bibliothèques pour éviter les conflits avec d'autres plugins :

```gradle
// build.gradle
shadowJar {
    archiveFileName = "TownsAndNations-${version}-shadow.jar"

    // Relocaliser les dépendances pour éviter les conflits
    relocate 'org.bstats', 'org.leralix.tan.lib.bstats'
    relocate 'com.zaxxer.hikari', 'org.leralix.tan.lib.zaxxer'
    relocate 'com.google.gson', 'org.leralix.tan.lib.google'
    relocate 'com.google.common', 'org.leralix.tan.lib.google.common'
    relocate 'org.slf4j', 'org.leralix.tan.lib.slf4j'
    relocate 'redis.clients', 'org.leralix.tan.lib.jedis'
    relocate 'org.redisson', 'org.leralix.tan.lib.redisson'

    // Minifier pour réduire la taille
    minimize()
}
```

**Avantages de la relocation:**
- Évite `NoSuchMethodError` si un autre plugin utilise une version différente
- Évite `ClassNotFoundException` pour les classes conflitantes
- Permet d'embarquer les dépendances sans "polluer" le classpath du serveur

---

## 2. PROCESSUS DE BUILD

### 2.1 Commandes Gradle

```bash
# Dans le dossier du projet
cd Towns-and-Nations-folia

# Compilation complète
./gradlew build

# Compilation + Shadow JAR
./gradlew shadowJar

# Compilation + Tests
./gradlew build test

# Clean + Build complet
./gradlew clean build

# Build avec rapport de couverture
./gradlew build jacocoTestReport
```

### 2.2 Output Généré

```
build/
├── libs/
│   ├── tan-api-{version}.jar           (Module API seul)
│   ├── tan-core-{version}.jar          (Module core seul)
│   ├── tan-vault-{version}.jar         (Module vault seul)
│   └── TownsAndNations-{version}-shadow.jar  ← JAR FINAL À DÉPLOYER
├── reports/
│   ├── jacoco/                         (Couverture de code)
│   └── tests/                          (Rapports de tests)
└── tmp/                                 (Fichiers temporaires)
```

---

## 3. CONTENU DU JAR FINAL

### 3.1 Structure Interne

```
TownsAndNations-2.0.0-shadow.jar
│
├── META-INF/
│   ├── MANIFEST.MF
│   │   └── Main-Class: org.leralix.tan.bukkit.TownsAndNations
│   └── plugin.yml                     (Metadata plugin)
│
├── org/leralix/tan/                   (Code principal)
│   ├── TownsAndNations.class           (Main class)
│   ├── commands/                       (Commandes)
│   ├── listeners/                      (Event listeners)
│   ├── gui/                            (Menus Triumph-GUI)
│   ├── storage/                        (MySQL/Redis)
│   ├── service/                        (Services métier)
│   └── ...
│
├── org/leralix/tan/lib/                (Dépendances relocalisées)
│   ├── bstats/                         (bstats metrics)
│   ├── zaxxer/                         (HikariCP connection pool)
│   ├── google/                         (Gson JSON)
│   ├── slf4j/                          (Logging)
│   └── ...
│
├── resources/                          (Ressources)
│   └── ...
│
└── *.kt                                (Classes Kotlin compilées)
```

### 3.2 plugin.yml

```yaml
name: TownsAndNations
version: 2.0.0
main: org.leralix.tan.bukkit.TownsAndNations
api-version: "1.21"
author: Tsumu
description: Plugin de gestion de towns et nations pour Folia
website: https://github.com/Tsumu
softdepend:
  - PlaceholderAPI
  - Vault
  - EssentialsX
loadbefore: []
commands:
  town:
    description: Gestion des towns
  claim:
    description: Gérer les claims
  # ... autres commandes
permissions:
  tan.base.*:
    description: Permissions de base
    default: true
  tan.admin.*:
    description: Permissions admin
    default: false
```

---

## 4. DÉPLOIEMENT

### 4.1 Installation Standard

```bash
# 1. Arrêter le serveur
stop.sh

# 2. Copier le JAR dans le dossier plugins
cp TownsAndNations-2.0.0-shadow.jar \
   /path/to/serveur/plugins/TownsAndNations.jar

# 3. Démarrer le serveur
start.sh

# Le plugin va:
# - Charger les dépendances
# - Initialiser MySQL/Redis
# - Créer les tables SQL si inexistantes
# - Charger les données existantes
```

### 4.2 Première Installation

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     PREMIER DÉMARRAGE                                     │
│                                                                              │
│  1. Chargement du plugin                                                    │
│     ├─ Scan du classpath                                                   │
│     ├─ Vérification des dépendances                                        │
│     └─ Enregistrement des commands/listeners                              │
│                                                                              │
│  2. Initialisation des connexions                                           │
│     ├─ MySQL: HikariCP pool créé                                          │
│     ├─ Redis: Redisson client connecté                                   │
│     └─ Tables SQL créées automatiquement                                  │
│                                                                              │
│  3. Chargement des données                                                   │
│     ├─ Players depuis MySQL                                               │
│     ├─ Towns depuis MySQL                                                │
│     ├─ Regions depuis MySQL                                              │
│     ├─ Claims depuis MySQL                                               │
│     └─ Cache local peuplé                                                │
│                                                                              │
│  4. Démarrage des tâches de fond                                           │
│     ├─ DailyTasks (taxes, prestiges)                                      │
│     ├─ Health checks                                                      │
│     └─ Metrics Prometheus                                                 │
│                                                                              │
│  5. Plugin prêt !                                                           │
│     └─ [TownsAndNations] Enabled - v2.0.0                                │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. CONFIGURATION POST-INSTALL

### 5.1 config.yml

```yaml
# Database configuration
database:
  type: "mysql"           # mysql ou sqlite
  host: "localhost"
  port: 3306
  database: "townsandnations"
  username: "root"
  password: "changeme"    # À MODIFIER !
  pool-size: 30
  min-idle: 5
  connection-timeout: 30000
  idle-timeout: 600000
  max-lifetime: 1800000
  leak-detection-threshold: 60000
  ssl:
    enabled: false
    require: false
    verify-server-certificate: false

# Redis configuration
redis:
  host: "localhost"
  port: 6379
  password: ""
  database: 0
  retryAttempts: 3
  retryInterval: 1500
  connectTimeout: 10000
  timeout: 5000
  enabled: true

# Cache configuration
cache:
  players: 1000          # Max joueurs en cache local
  towns: 500             # Max towns en cache local
  regions: 200           # Max regions en cache local
  chunks: 5000           # Max chunks en cache local
```

### 5.2 Permissions

```
/adminp setdefault <joueur> tan.base.* true
/adminp setdefault <joueur> tan.admin.* false
```

---

## 6. HISTORIQUE DES DONNÉES

### 6.1 Migration depuis JSON

```java
// JsonToDatabaseMigration.java
// Si l'ancien système JSON est détecté, migrer automatiquement

if (jsonFilesExist()) {
    plugin.getLogger().info("Legacy JSON data detected. Starting migration...");
    migratePlayersFromJSON();
    migrateTownsFromJSON();
    migrateRegionsFromJSON();
    backupJSONFiles();
    plugin.getLogger().info("Migration complete! JSON files backed up.");
}
```

### 6.2 Sauvegarde automatique

```java
// Automatic backup enabled by default
// Les données JSON sont sauvegardées avant suppression
backup/
├── tan_players_backup_20260327.json
├── tan_towns_backup_20260327.json
└── tan_regions_backup_20260327.json
```

---

## 7. MISES À JOUR

### 7.1 Processus de Mise à Jour

```bash
# 1. Arrêter le serveur
stop.sh

# 2. Sauvegarder la base de données
mysqldump townsandnations > backup_$(date +%Y%m%d).sql

# 3. Remplacer l'ancien JAR par le nouveau
rm plugins/TownsAndNations.jar
cp TownsAndNations-2.0.1-shadow.jar plugins/TownsAndNations.jar

# 4. Démarrer le serveur
start.sh

# 5. Vérifier les logs
tail -f logs/latest.log
```

### 7.2 Schema Migrations

```java
// DatabaseSchemaUpdater.java
// Les migrations automatiques sont gérées au démarrage

public void migrateToLatest() {
    int currentVersion = getSchemaVersion();

    // V1.0 → V1.1: Ajouter colonne town_name
    if (currentVersion < 2) {
        executeMigration("ALTER TABLE tan_towns ADD COLUMN town_name VARCHAR(255)");
        setSchemaVersion(2);
    }

    // V1.1 → V1.2: Ajouter index sur town_name
    if (currentVersion < 3) {
        executeMigration("CREATE INDEX idx_town_name ON tan_towns(...)");
        setSchemaVersion(3);
    }
}
```

---

## 8. TAILLE ET PERFORMANCE

### 8.1 Taille du JAR

| Composant | Taille |
|-----------|--------|
| Code compilé | ~3 MB |
| Kotlin Stdlib | ~2 MB |
| HikariCP | ~150 KB |
| Redisson | ~1 MB |
| Gson | ~250 KB |
| Paper API | ~1 MB |
| **Total** | **~15-25 MB** |

### 8.2 Memory Usage

```
Plugin chargé (sans données joueur):

- Heap Memory: ~50-100 MB
- Off-Heap: ~20-50 MB (Direct buffers MySQL/Redis)
- Thread Stacks: ~10-20 MB

Avec 100 joueurs:

- Heap Memory: ~200-300 MB
- Cache local: ~50-100 MB
- Connections MySQL: ~30 connexions (HikariCP)
- Connections Redis: ~10 connexions
```

---

## 9. DIAGNOSTICS

### 9.1 Vérifier l'état du plugin

```bash
# Depuis la console in-game ou console serveur
/ta version
/ta admin debug mysql
/ta admin debug redis
/ta admin debug cache
```

### 9.2 Logs Importants

```
logs/latest.log:
[TownsAndNations] Enabling TownsAndNations v2.0.0
[TownsAndNations] Connecting to MySQL database...
[TownsAndNations] MySQL connection pool established (30 connections)
[TownsAndNations] Connecting to Redis...
[TownsAndNations] Redis connection established
[TownsAndNations] Loading 124 towns from database...
[TownsAndNations] Loading 5 regions from database...
[TownsAndNations] Loading 1,234 players from database...
[TownsAndNations] Plugin fully enabled! Ready to serve.
```

---

## 10. RÉSUMÉ

| Étape | Commande | Output |
|-------|----------|--------|
| **Build** | `./gradlew shadowJar` | `build/libs/TownsAndNations-{version}-shadow.jar` |
| **Deploy** | Copier dans `plugins/` | Plugin chargé au démarrage |
| **Config** | Éditer `config.yml` | MySQL + Redis settings |
| **Update** | Remplacer le JAR | Migration automatique |

**Points clés à retenir :**
1. Le plugin utilise **Shadow JAR** pour embarquer ses dépendances
2. Les dépendances sont **relocalisées** pour éviter les conflits
3. La **première installation** crée automatiquement les tables SQL
4. Les **migrations** sont automatiques (schema versioning)
5. Le plugin est **Folia-compatible** (region threading)

---

*Document créé pour CocoWorld R2 - 2026-03-27*
