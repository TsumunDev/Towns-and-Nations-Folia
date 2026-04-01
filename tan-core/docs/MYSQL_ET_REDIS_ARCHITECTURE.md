# Architecture MySQL & Redis - Towns-and-Nations

## 📋 Vue d'Ensemble

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             JOUEUR MINECRAFT                              │
│                     (connexion, déconnexion, actions)                       │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         TOWNS-AND-NATIONS PLUGIN                             │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │                         CACHE MULTI-NIVEAU                           │  │
│  │  ┌─────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │  │
│  │  │ Cache Local │  │ Redis Cache  │  │    Database Cache       │  │  │
│  │  │  (mémoire)  │  │  (partagé)   │  │   (HikariCP pool)       │  │  │
│  │  │   TTL: 5s   │  │  TTL: 10min  │  │   Query + Result Cache   │  │
│  │  │   (PlayerData)│ │ (Pub/Sub)    │  │   (get/put/invalidate)    │  │
│  │  └─────────────┘  └──────────────┘  └──────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                      │                                    │
│                                      ▼                                    ▼
│  ┌─────────────────────────┐  ┌──────────────────────────────────────────┐  │
│  │    REDIS CLUSTER        │  │         MYSQL / SQLITE                 │  │
│  │  (multi-serveur sync)   │  │     (données persistantes)              │  │
│  │  - Pub/Sub events      │  │     - towns, regions, players          │  │
│  │  - Query cache         │  │     - claims, wars, economy            │  │
│  │  - Health checks       │  │                                      │  │
│  └─────────────────────────┘  └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 1. MYSQL - Base de Données Principale

### 1.1 Architecture de Connexion

```java
// MySqlHandler.java - HikariCP Connection Pool
HikariConfig config = new HikariConfig();
config.setJdbcUrl("jdbc:mysql://host:3306/database");
config.setMaximumPoolSize(30);        // Max connexions simultanées
config.setMinimumIdle(5);             // Connexions maintenues prêtes
config.setConnectionTimeout(30000L); // Timeout attente
config.setMaxLifetime(1800000L);     // Durée max connexion (30 min)
config.setLeakDetectionThreshold(60000L); // Détection fuite
```

**Pourquoi HikariCP ?**
- Pool de connexions performant
- Réutilisation des connexions (évite le coût d'établissement)
- Détection automatique des connexions "fuitées"
- Gestion automatique des connexions mortes

### 1.2 Schéma Simplifié

```sql
-- Tables principales
tan_players      → Données joueur (UUID, argent, town, region)
tan_towns        → Towns (ID, nom, territoire, banque, membres)
tan_regions      → Nations/Régions (ID, nom, vassaux, capital)
tan_claimed_chunks→ Claims (world, x, z, town_id, permissions)
tan_metadata     → Méta-données (next_town_id, next_region_id)
tan_wars         → Guerres entre towns
tan_landmarks    → Points d'intérêt

-- Chaque table stocke le data en JSON dans une colonne `data`
CREATE TABLE tan_players (
    id VARCHAR(36) PRIMARY KEY,
    data TEXT NOT NULL  -- JSON sérialisé de ITanPlayer
);
```

### 1.3 Cycle de Vie d'une Requête MySQL

```
1. Demande de données (ex: getPlayerAsync(uuid))
           │
           ▼
2. Vérifier Cache Local (PlayerData cache)
   │  → HIT  → Retourner données immédiatement
   │  → MISS → Continuer
           │
           ▼
3. Obtenir connexion depuis HikariCP Pool
   │  (Pool maintient 5-30 connexions prêtes)
           │
           ▼
4. Exécuter PreparedStatement (paramétré)
   │  SELECT data FROM tan_players WHERE id = ?
           │
           ▼
5. Parser le JSON → objet Java (Gson)
           │
           ▼
6. Stocker dans Cache Local + Cache Redis
           │
           ▼
7. Retourner au joueur
           │
           ▼
8. Libérer la connexion (retour au pool)
```

---

## 2. REDIS - Cache Distribué & Multi-Serveur

### 2.1 Architecture Redis

```java
// RedisManager.java - Redisson Client
RedissonClient redisson = Redisson.create(config);
RMapCache<String, String> queryCache = redisson.getMapCache("tan_query_cache");
```

**Configuration Redis:**
```yaml
redis:
  host: "localhost"
  port: 6379
  password: ""
  database: 0
  retryAttempts: 3
  retryInterval: 1500
  connectTimeout: 10000
  timeout: 5000
```

### 2.1 Utilisations de Redis

| Fonction | Description | TTL |
|----------|-------------|-----|
| **Query Cache** | Cache des résultats SQL | 10 min |
| **Pub/Sub** | Synchronisation cross-serveur | Immédiat |
| **Health Checks** | Heartbeat serveurs | 5 sec |
| **Locks** | Distributed locks (éviter race conditions) | Auto-expire |

### 2.2 Pub/Sub - Synchronisation Multi-Serveur

```java
// Quand un événement survient sur Serveur A
redisson.getTopic("tan_updates").publish(new TownUpdateEvent(townId));

// Tous les serveurs écoutent
redisson.getTopic("tan_updates").addListener((channel, msg) -> {
    // Invalider le cache local
    cacheManager.invalidate(msg.getTownId());
});
```

**Événements synchronisés:**
- `TownCreated` → Nouvelle town créée
- `TownDeleted` → Town supprimée
- `PlayerJoinedTown` → Joueur rejoint une town
- `PlayerLeftTown` → Joueur quitte une town
- `ChunkClaimed` → Chunk claimé
- `ChunkUnclaimed` → Chunk unclaimé
- `WarDeclared` → Guerre déclarée

---

## 3. SCÉNARIO: JOUEUR SE CONNECTE

### 3.1 Flux Complet

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         PLAYER JOIN EVENT                                 │
└──────────────────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  1. PlayerJoinListener.onPlayerJoin()                                        │
│                                                                              │
│  - Créer ITanPlayer si n'existe pas                                        │
│  - Charger données depuis DatabaseStorage                                   │
│  - Initialiser les coroutines Folia                                         │
└──────────────────────────────────────────────────────────────────────────────┘
                                 │
                    ┌────────────┼────────────┐
                    │            │            │
                    ▼            ▼            ▼
            ┌──────────┐  ┌──────────┐  ┌──────────┐
            │  CACHE   │  │  REDIS   │  │  MYSQL   │
            │  CHECK   │  │  CHECK   │  │  QUERY   │
            └──────────┘  └──────────┘  └──────────┘
                    │            │            │
                    └────────────┼────────────┘
                                 │
                                 ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  2. Données chargées en mémoire                                          │
│                                                                              │
│  - PlayerData dans cache local (TTL: 5s)                                  │
│  - TownData dans cache si joueur a une town                                │
│  - RegionData dans cache si joueur a une region                            │
│  - ClaimedChunks autour du joueur préchargés                              │
└──────────────────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  3. Notifications                                                        │
│                                                                              │
│  - Message de bienvenue                                                   │
│  - Scoreboard mis à jour                                                  │
│  - Prefix du chat appliqué                                                │
│  - Event "PlayerJoinEvent" publié sur Redis (autres serveurs notifiés)   │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. SCÉNARIO: JOUEUR CRÉE UNE TOWN

### 4.1 Flux des Données

```java
// CreateTown.java - Processus complet

createTownAsync(player, "MaVille")
    │
    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  ÉTAPE 1: Validation synchrones                                           │
│  - InputValidator.validateTownName("MaVille")                             │
│  - Vérifier format du nom                                                 │
│  - Vérifier nom non réservé                                               │
└─────────────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  ÉTAPE 2: Vérifier solde async                                             │
│  AsyncEconomyService.getBalance(player)                                   │
│  → Cache local → Redis → MySQL                                           │
│  → Si solde < coût → annuler                                              │
└─────────────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  ÉTAPE 3: Vérifier nom disponible                                        │
│  TownDataStorage.isNameUsed("MaVille")                                    │
│  → Cache Redis (index nom→ID)                                            │
│  → Si utilisé → annuler                                                  │
└─────────────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  ÉTAPE 4: Charger données joueur                                           │
│  PlayerDataStorage.get(player)                                            │
│  → Cache local → Redis → MySQL                                           │
│  → Vérifier pas déjà de town                                              │
└─────────────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  ÉTAPE 5: Retirer l'argent                                                │
│  AsyncEconomyService.withdraw(player, cost)                               │
│  → Transaction économique                                               │
│  → Si échec → annuler + erreur                                            │
└─────────────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  ÉTAPE 6: Créer la town dans MySQL                                        │
│  TownDataStorage.newTown("MaVille", player)                               │
│  1. Générer nouvel ID (metadata +1)                                       │
│  2. Créer objet TownData                                                 │
│  3. Sérialiser en JSON                                                   │
│  4. INSERT INTO tan_towns (id, data)                                     │
│  5. UPDATE tan_metadata SET next_town_id = newId                         │
└─────────────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  ÉTAPE 7: Mettre à jour les caches                                        │
│  1. Cache local: ajouter TownData                                        │
│  2. Redis: publier "TownCreated" event                                   │
│  3. Autres serveurs: invalider + rafraîchir                             │
│  4. PlayerData: ajouter town_id au joueur                                │
└─────────────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  ÉTAPE 8: Notifications & Effets                                           │
│  - Message au joueur                                                      │
│  - Spawn chunk claimé automatiquement                                    │
│  - Scoreboard mis à jour                                                 │
│  - Prefix " [MaVille]" appliqué                                          │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. FLUX DE DONNÉES: CHAT SCOPE LOCAL

```
JOUEUR A écrit "Salut!" dans le chat local
              │
              ▼
    ChatScopeListener.onPlayerChat()
              │
        ┌─────┴─────┐
        │           │
        ▼           ▼
   Vérifier      Vérifier
  Scope du     Permissions
   joueur      (chat local)
        │           │
        └─────┬─────┘
              │
              ▼
    LocalChatStorage.broadcastInScope()
              │
        ┌─────┼─────┐
        │     │     │
        ▼     ▼     ▼
    MYSQL  REDIS CACHE
    (load) (check)
        │
        ▼
  Trouver tous
  les joueurs
  dans le même
    scope     │
        ▼
  Envoyer message
  à chaque joueur
  (async via
 FoliaScheduler)
```

---

## 6. PERFORMANCE & OPTIMISATIONS

### 6.1 Stratégie de Cache Multi-Niveau

```
╔════════════════════════════════════════════════════════════════════════════╗
║                         STRATÉGIE DE CACHE                               ║
╠════════════════════════════════════════════════════════════════════════════╣
║                                                                             ║
║  NIVEAU 1: Cache Local (mémoire du serveur)                               ║
║  ┌───────────────────────────────────────────────────────────────────────┐  ║
║  │ Map<String, ITanPlayer> playerCache                                 │  ║
║  │ Map<String, TownData> townCache                                     │  ║
║  │ TTL: 5 secondes                                                      │  ║
║  │ Hit rate: ~95%                                                       │  ║
║  └───────────────────────────────────────────────────────────────────────┘  ║
║                              ↓ (miss)                                     ║
║  NIVEAU 2: Redis (partagé entre serveurs)                                ║
║  ┌───────────────────────────────────────────────────────────────────────┐  ║
║  │ RMapCache<String, String> queryCache                                │  ║
║  │ TTL: 10 minutes                                                      │  ║
║  │ Hit rate: ~80% (des misses local)                                   │  ║
║  │ Pub/Sub pour invalidation cross-serveur                             │  ║
║  └───────────────────────────────────────────────────────────────────────┘  ║
║                              ↓ (miss)                                     ║
║  NIVEAU 3: MySQL/SQLite (persistant)                                     ║
║  ┌───────────────────────────────────────────────────────────────────────┐  ║
║  │ HikariCP DataSource (pool de connexions)                            │  ║
║  │ Requêtes SQL indexées                                               │  ║
║  │ Latence: ~5-20ms                                                    │  ║
║  └───────────────────────────────────────────────────────────────────────┘  ║
║                                                                             ║
╚════════════════════════════════════════════════════════════════════════════╝
```

### 6.2 SQL Index Optimisés

```sql
-- Index pour les lookups fréquents
CREATE INDEX idx_player_uuid ON tan_players(id);
CREATE INDEX idx_town_name ON tan_towns((JSON_EXTRACT(data, '$.name')));
CREATE INDEX idx_claim_chunk ON tan_claimed_chunks(world, x, z);
CREATE INDEX idx_town_players ON tan_players((JSON_EXTRACT(data, '$.townID')));
```

---

## 7. DIAGRAMME DE SÉQUENCE COMPLET

```
Joueur         Plugin          Cache Local      Redis         MySQL
  │              │                 │              │             │
  │  /towncreate │                 │              │             │
  │─────────────>│                 │              │             │
  │              │                 │              │             │
  │              │ checkBalance()  │              │             │
  │              │────────────────>│              │             │
  │              │    (MISS)        │              │             │
  │              │───────────────────────────────>│             │
  │              │                 │              │             │
  │<─────────────│ return 1000      │              │             │
  │              │                 │              │             │
  │              │ withdraw(500)   │              │             │
  │              │───────────────────────────────>│             │
  │              │                 │              │             │
  │<─────────────│ success          │              │             │
  │              │                 │              │             │
  │              │ createTown()    │              │             │
  │              │──────────────────────────────────────────>│
  │              │                 │              │             │
  │<─────────────│ townCreated      │              │             │
  │              │                 │              │             │
  │              │ cache.put(town) │              │             │
  │              │────────>         │              │             │
  │              │                 │              │             │
  │              │ publish(event)  │              │             │
  │              │─────────────────>│              │             │
  │              │                 │              │             │
  │<─────────────│ Done!            │              │             │
```

---

## 8. FOLIA COMPLIANCE

### 8.1 Threading Régional

```java
// FoliaScheduler.java - Abstraction Folia
public static void runTaskAtLocation(Location location, Runnable task) {
    Bukkit.getRegionScheduler()
        .execute(plugin, location, task);  // Région du chunk
}

// Utilisation dans DatabaseStorage
public CompletableFuture<T> get(String id) {
    CompletableFuture<T> future = new CompletableFuture<>();
    runTaskAsync(() -> {
        T result = loadFromDatabase(id);  // Thread async
        future.complete(result);
    });
    return future;
}
```

### 8.2 Pourquoi c'est important

- **Folia** divise le monde en **régions indépendantes**
- Chaque région a son **propre thread**
- Les opérations bloquantes doivent être **hors des régions**
- MySQL/Redis doivent être appelés depuis **threads async**

---

## 9. RÉSUMÉ

| Composant | Rôle | TTL | Hit Rate |
|-----------|-----|-----|----------|
| **Cache Local** | Mémoire serveur | 5s | 95% |
| **Redis** | Cache distribué | 10min | 80% (des misses local) |
| **MySQL** | Persistance | ∞ | N/A |

**Pipeline optimal:** 99%+ des requêtes sont servies depuis le cache local ou Redis, <1% atteignent MySQL.

---

## 10. DIAGNOSTICS

### Vérifier l'état MySQL

```bash
# Depuis la console du plugin
/ta admin debug mysql
# Affiche:
# - Connexions actives dans le pool
# - Requêtes par seconde
# - Temps moyen de requête
# - État du circuit breaker
```

### Vérifier l'état Redis

```bash
# Depuis la console
/ta admin debug redis
# Affiche:
# - Connexion Redis
# - Nombre d'entrées en cache
# - Hit/Miss ratio
# - Events Pub/Sub reçus
```

---

*Document créé pour CocoWorld R2 - 2026-03-27*
