# 🏰 Coconation - Towns & Nations

[![Version](https://img.shields.io/badge/version-2.0.0-blue.svg)](https://github.com/Leralix/Towns-And-Nations)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1%2B-green.svg)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Folia](https://img.shields.io/badge/Folia-1.21.1%2B-brightgreen.svg)](https://papermc.io/software/folia)
[![License](https://img.shields.io/badge/license-GPL--3.0-red.svg)](LICENSE)

> Un plugin de gestion territoriale, diplomatique et économique complet pour Minecraft Folia/Paper

## 📖 Table des Matières

- [Présentation](#-présentation)
- [Fonctionnalités](#-fonctionnalités)
- [Installation](#-installation)
- [Architecture](#-architecture)
- [Commandes](#-commandes)
- [API](#-api)
- [Performance](#-performance)
- [Développement](#-développement)
- [Support](#-support)

---

## 🎯 Présentation

**Coconation** (anciennement Towns & Nations) est un plugin Minecraft qui permet aux joueurs de créer et gérer des **territoires complexes** avec un système complet de diplomatie, d'économie et de guerre. Conçu nativement pour **Folia**, le plugin profite du multi-threading pour des performances optimales sur les serveurs haute capacité.

### Caractéristiques Principales

✅ **Architecture Multi-Threaded** - Compatible Folia avec patterns async/await  
✅ **Système Territorial Complet** - Villes, régions (nations), vassalité  
✅ **Économie Intégrée** - Taxes, salaires, propriétés, budget territorial  
✅ **Diplomatie Avancée** - Alliances, guerres, neutralité, vassalité  
✅ **Protection des Territoires** - Système de permissions granulaire par chunk  
✅ **Base de Données Performante** - MySQL/SQLite avec cache Redis optionnel  
✅ **API Publique** - Interface complète pour les développeurs tiers  

---

## 🚀 Fonctionnalités

### 🏘️ Système de Villes

#### Création et Gestion
- **Création de ville** avec nom, description et icône personnalisables
- **Niveaux de ville** (1-10) débloquant progressivement des capacités
- **Tag coloré** affiché devant le nom des joueurs
- **Capitale** avec point de spawn configurable
- **Recrutement** ouvert/fermé avec système de candidatures
- **Hiérarchie** avec rangs personnalisables et permissions granulaires

#### Territoire et Chunks
- **Claim de chunks** (16x16) avec limite basée sur le niveau de ville
- **Chunks spéciaux** : capitale (protégée), spawn, zone de guerre
- **Permissions par chunk** : construction, destruction, interactions, PvP
- **Visualisation** : carte ASCII in-game (`/ccn map`)
- **Autoclaim** : claim automatique en marchant

#### Économie de Ville
- **Trésorerie commune** financée par les taxes et dons
- **Taxes automatiques** : flat (montant fixe) ou pourcentage du solde
- **Salaires** versés aux membres selon leur rang
- **Propriétés** : système de parcelles privées 3D avec loyer
- **Landmarks** : bâtiments à construire débloquant des bonus
- **Transactions** : historique complet des revenus/dépenses

### 🌍 Système de Régions (Nations)

- **Création de région** regroupant plusieurs villes
- **Capitale régionale** avec point de spawn
- **Diplomatie régionale** (alliances, guerres entre nations)
- **Budget régional** avec système de taxes inter-villes
- **Rangs régionaux** distincts des rangs de ville
- **Vassalité** : ville vassale d'une région avec tribut automatique

### ⚔️ Système de Guerre

#### Déclaration et Déroulement
- **Planification** : déclaration avec préparation de 24h minimum
- **Time slots** configurables (ex : guerres uniquement 20h-23h)
- **Objectifs de guerre** : conquête de chunks, vassalité, argent, libération
- **Attaques planifiées** avec phases de préparation et d'assaut
- **Score de guerre** : calcul automatique du vainqueur
- **Reddition** : capitulation anticipée avec application des objectifs

#### Mécaniques de Combat
- **Zone d'attaque** : chunks spécifiques visés par l'assaut
- **Défense territoriale** : bonus pour les défenseurs sur leur terrain
- **PvP automatique** dans les zones de conflit
- **Capture de chunks** si défenseurs éliminés
- **Cooldown** entre attaques sur le même territoire

### 🤝 Système Diplomatique

- **Relations** :
  - 🟢 **Alliance** : accès mutuel aux territoires, défense commune
  - 🟡 **Non-Aggression** : paix garantie sans accès territorial
  - 🔴 **Guerre** : PvP activé, attaques de chunks possibles
  - ⚪ **Neutre** : relation par défaut

- **Traités** : propositions avec acceptation requise
- **Vassalité** : ville soumise à une région avec tribut
- **Indépendance** : libération d'une vassalité

### 💰 Système Économique

#### Économie de Joueur
- **Balance personnelle** stockée en base de données
- **Transactions** : `/ccn pay <joueur> <montant>`
- **Salaire automatique** versé par la ville
- **Taxes municipales** prélevées quotidiennement
- **Propriétés** : achat et location de parcelles

#### Économie Territoriale
- **Budget de ville/région** géré par les dirigeants
- **Revenus** :
  - Taxes des citoyens (flat ou %)
  - Vente de propriétés
  - Tributs des vassaux
  - Dons volontaires
- **Dépenses** :
  - Salaires des membres
  - Construction de landmarks
  - Coût de maintenance
  - Tribut au suzerain
- **Transactions historique** : journal complet avec date/montant/type

#### Intégration Vault (Optionnelle)
- Support natif de **Vault API** pour économie externe
- Mode standalone disponible si Vault absent
- Synchronisation automatique des balances

### 🏗️ Système de Landmarks

- **Bâtiments spéciaux** à construire dans les villes :
  - 🏛️ **Town Hall** : augmente limite de membres
  - 🏰 **Fortress** : bonus défensif en guerre
  - 📚 **Library** : XP supplémentaire pour citoyens
  - 💎 **Bank** : réduit taxes municipales
  - ⚒️ **Workshop** : accès crafts spéciaux
  - 🏪 **Market** : place de marché inter-villes

- **Exigences** : 
  - Coût de construction (argent + ressources)
  - Volume minimal (schematic 3D)
  - Niveau de ville requis

- **Gestion** :
  - Placement avec schematic
  - Validation automatique de construction
  - Destruction possible avec remboursement partiel

### 🛡️ Système de Permissions

#### Permissions de Chunk
Par chunk, configuration fine de :
- 🔨 **BUILD** : placement/destruction de blocs
- 🚪 **INTERACT** : portes, coffres, boutons, levier
- 🗡️ **PVP** : combat entre joueurs
- 🐑 **MOB_HURT** : attaque des animaux/monstres
- 💥 **EXPLOSIONS** : protection contre TNT/creepers

#### Permissions par Groupe
- **Citoyens** : accès complet à la ville
- **Alliés** : permissions limitées configurables
- **Étrangers** : accès restreint (configurable)
- **Ennemis** : accès interdit en temps de guerre

#### Permissions de Rôle
Rangs avec permissions granulaires :
- Gestion des membres (invite/kick)
- Claim/unclaim de chunks
- Modification des permissions
- Gestion des alliances
- Déclaration de guerre
- Gestion du budget
- Modification des paramètres

### 📊 Interface Graphique

#### Menus Principaux
- 🏠 **Menu principal** : accès à toutes les fonctionnalités
- 🏘️ **Menu ville** : informations et gestion territoriale
- 🌍 **Menu région** : diplomatie et budget national
- 👥 **Menu membres** : liste des citoyens avec rangs
- 💰 **Menu trésorerie** : budget et transactions
- 🗺️ **Menu territoire** : chunks revendiqués et permissions
- ⚔️ **Menu diplomatie** : relations et guerres
- 🏗️ **Menu landmarks** : bâtiments et constructions

#### Fonctionnalités GUI
- **Pagination** automatique pour listes longues
- **Icônes personnalisées** via Nexo/Oraxen (optionnel)
- **Layouts configurables** (`layouts.yml`)
- **Hover info** : tooltips détaillés sur items
- **Confirmation** : popups pour actions critiques
- **Animation** : effets visuels pour feedback

### 🔔 Système de Newsletter

- **Événements automatiques** notifiés :
  - Nouveau membre rejoint
  - Membre quitte/est expulsé
  - Guerre déclarée/terminée
  - Alliance créée/rompue
  - Niveau de ville augmenté
  - Budget insuffisant
  - Landmark construit

- **Configuration** : activation/désactivation par type
- **Stockage** : historique persistant en base de données
- **Affichage** : liste avec pagination in-game

### 🗨️ Système de Chat

- **Chat de ville** : `/ccn chat town` - discussion privée citoyens
- **Chat de région** : `/ccn chat region` - discussion nationale
- **Chat global** : retour au chat public
- **Préfixe** : tag coloré de la ville devant messages
- **Portée** : limitation géographique optionnelle (chunks voisins)

### 🗺️ Intégrations Externes

#### PlaceholderAPI
Placeholders disponibles :
- `%tan_town_name%` : nom de la ville du joueur
- `%tan_town_tag%` : tag coloré de la ville
- `%tan_region_name%` : nom de la région
- `%tan_balance%` : balance personnelle
- `%tan_town_balance%` : trésorerie de ville
- `%tan_rank%` : rang dans la ville
- ... (30+ placeholders)

#### WorldGuard
- **Détection automatique** des régions WG
- **Blocage du claim** dans zones protégées
- **Respect des flags** WG pour permissions

#### Dynmap/BlueMap (Planifié)
- Affichage territoires sur carte web
- Coloration par ville/région
- Popup avec informations territoire

#### Nexo/Oraxen
- **Icônes customs** pour items GUI
- **Glyphs** pour décoration menus
- Fallback automatique vers icons vanilla

---

## 📥 Installation

### Prérequis

- **Serveur** : Paper 1.20.1+ ou Folia 1.20.1+
- **Java** : OpenJDK 21 (recommandé : Eclipse Temurin)
- **Dépendances** : CocoNationLib (fournie)
- **Optionnel** : PlaceholderAPI, WorldGuard, Vault, Nexo, Redis

### Étapes d'Installation

1. **Télécharger** le JAR depuis [Releases](https://github.com/Leralix/Towns-And-Nations/releases)

2. **Placer** les fichiers dans `/plugins/` :
   ```
   plugins/
   ├── CocoNationLib-X.X.X.jar
   └── Coconation-X.X.X.jar
   ```

3. **Démarrer** le serveur pour générer la configuration

4. **Configurer** `plugins/Coconation/config.yml` :
   ```yaml
   database:
     type: "mysql"  # ou "sqlite" pour démarrage rapide
     host: "localhost"
     port: 3306
     database: "coconation"
     username: "root"
     password: "votre_mot_de_passe"
   
   economy:
     starting_balance: 1000.0
     town_creation_cost: 5000.0
     chunk_claim_cost: 100.0
   
   town:
     max_name_length: 20
     max_description_length: 200
     claim_distance_limit: 10  # chunks entre claims
   ```

5. **Redémarrer** le serveur

### Configuration Avancée

#### Base de Données MySQL (Production)
```yaml
database:
  type: "mysql"
  host: "localhost"
  port: 3306
  database: "coconation"
  username: "coconation_user"
  password: "strong_password_here"
  pool_size: 10
  connection_timeout: 30000
```

#### Cache Redis (Multi-Serveur)
```yaml
redis:
  enabled: true
  host: "localhost"
  port: 6379
  password: ""
  database: 0
  cache_ttl: 300  # secondes
```

#### Batch Write Optimizer
```yaml
database:
  batch_write_interval: 20  # ticks entre flush
  batch_write_size: 100     # queue max avant flush forcé
```

---

## 🏗️ Architecture

### Structure Multi-Modules

```
Towns-and-Nations/
├── tan-api/          # API publique pour développeurs
│   ├── interfaces/   # Contrats TownAPI, EconomyAPI...
│   └── events/       # Événements custom Bukkit
│
├── tan-core/         # Implémentation du plugin
│   ├── commands/     # Gestionnaires de commandes
│   ├── dataclass/    # Modèles de données (Town, Region, Player)
│   ├── economy/      # Système économique
│   ├── events/       # Listeners Bukkit
│   ├── gui/          # Interfaces utilisateur
│   ├── storage/      # Couche persistance (DB + cache)
│   ├── tasks/        # Tâches planifiées (taxes, backups)
│   ├── utils/        # FoliaScheduler, ChatUtils...
│   └── wars/         # Système de guerre
│
└── docs/             # Documentation technique
```

### Flux de Données

```
Joueur
  ↓ Commande/GUI
PlayerCommandManager
  ↓ Validation
TownData / RegionData
  ↓ Modification
DatabaseStorage (async)
  ↓ Cache L1
Guava Cache (local)
  ↓ Cache L2
Redis Cache (cross-server)
  ↓ Persistance
MySQL / SQLite
```

### Patterns Architecturaux

- **Composition over Inheritance** : `TownData` délègue à `TerritoryEconomy`, `TerritoryChunks`, `TerritoryRelations`
- **Singleton** : Storages, Managers, Schedulers
- **Factory Static** : Menus GUI avec chargement async des données
- **Strategy** : Permissions calculées par `PermissionService`
- **Observer** : Événements Bukkit pour découplage

---

## ⚙️ Commandes

### Commandes Joueur (`/coconation` ou `/ccn`)

| Commande | Description | Permission |
|----------|-------------|------------|
| `/ccn help` | Affiche l'aide | `tan.base.commands.help` |
| `/ccn gui` | Ouvre le menu principal | `tan.base.commands.gui` |
| `/ccn create <nom>` | Crée une ville | `tan.base.town.create` |
| `/ccn join <ville>` | Rejoint une ville | `tan.base.town.join` |
| `/ccn quit` | Quitte sa ville | `tan.base.town.quit` |
| `/ccn claim` | Revendique le chunk actuel | `tan.base.commands.claim` |
| `/ccn unclaim` | Abandonne le chunk actuel | `tan.base.commands.unclaim` |
| `/ccn autoclaim` | Toggle claim automatique | `tan.base.commands.autoclaim` |
| `/ccn map` | Affiche la carte des chunks | `tan.base.commands.map` |
| `/ccn spawn [ville]` | Téléporte au spawn | `tan.base.commands.spawn` |
| `/ccn setspawn` | Définit le spawn de ville | `tan.base.commands.setspawn` |
| `/ccn invite <joueur>` | Invite un joueur | Permission de rang |
| `/ccn kick <joueur>` | Expulse un joueur | Permission de rang |
| `/ccn pay <joueur> <montant>` | Transfère de l'argent | `tan.base.commands.pay` |
| `/ccn balance` | Affiche sa balance | `tan.base.commands.balance` |
| `/ccn chat <scope>` | Change de canal de chat | `tan.base.commands.chat` |
| `/ccn newsletter` | Consulte les actualités | `tan.base.commands.newsletter` |

### Commandes Admin (`/coconationadmin` ou `/ccnadmin`)

| Commande | Description |
|----------|-------------|
| `/ccnadmin reload` | Recharge la config |
| `/ccnadmin town <delete/teleport/info>` | Gestion des villes |
| `/ccnadmin player <reset/teleport/info>` | Gestion des joueurs |
| `/ccnadmin economy <set/add/remove>` | Gestion de l'économie |
| `/ccnadmin claim <force/remove>` | Gestion forcée des claims |
| `/ccnadmin debug <cache/database/threads>` | Outils de debug |

### Commandes Debug (`/coconationdebug` ou `/ccndebug`)

| Commande | Description |
|----------|-------------|
| `/ccndebug performance` | Métriques de performance |
| `/ccndebug cache stats` | Statistiques de cache |
| `/ccndebug db query <sql>` | Requête SQL directe |
| `/ccndebug thread dump` | Dump des threads Folia |
| `/ccndebug memory` | État de la mémoire JVM |

---

## 🔌 API

### Accès à l'API

```java
import org.tan.api.TANAPIProvider;
import org.tan.api.TownAPI;
import org.tan.api.EconomyAPI;

public class MonPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // Récupération des APIs
        TownAPI townAPI = TANAPIProvider.getTownAPI();
        EconomyAPI economyAPI = TANAPIProvider.getEconomyAPI();
        
        // Utilisation
        TanTown town = townAPI.getTown(player);
        if (town != null) {
            getLogger().info("Joueur dans la ville : " + town.getName());
        }
        
        double balance = economyAPI.getBalance(player);
        getLogger().info("Balance : " + balance);
    }
}
```

### Interfaces Principales

#### TownAPI
```java
public interface TownAPI {
    TanTown getTown(Player player);
    TanTown getTown(String townId);
    Collection<TanTown> getAllTowns();
    boolean createTown(Player owner, String name);
    boolean deleteTown(String townId);
    boolean playerHasTown(Player player);
}
```

#### EconomyAPI
```java
public interface EconomyAPI {
    double getBalance(Player player);
    boolean hasBalance(Player player, double amount);
    boolean deposit(Player player, double amount);
    boolean withdraw(Player player, double amount);
    double getTownBalance(TanTown town);
}
```

#### ClaimAPI
```java
public interface ClaimAPI {
    TanClaimedChunk getClaimedChunk(Chunk chunk);
    boolean isChunkClaimed(Chunk chunk);
    boolean canPlayerBuild(Player player, Location location);
    Collection<TanClaimedChunk> getTownChunks(TanTown town);
}
```

#### NationAPI
```java
public interface NationAPI {
    TanRegion getRegion(Player player);
    TanRegion getRegion(String regionId);
    Collection<TanRegion> getAllRegions();
    boolean createRegion(TanTown capital, String name);
    boolean isPlayerInRegion(Player player);
}
```

### Événements Custom

```java
import org.tan.api.events.*;

// Town events
@EventHandler
public void onTownCreate(TownCreateEvent event) {
    Player owner = event.getOwner();
    String townName = event.getTownName();
    // event.setCancelled(true); pour annuler
}

@EventHandler
public void onPlayerJoinTown(PlayerJoinTownEvent event) {
    Player player = event.getPlayer();
    TanTown town = event.getTown();
}

// War events
@EventHandler
public void onWarDeclare(WarDeclareEvent event) {
    TanTerritory attacker = event.getAttacker();
    TanTerritory defender = event.getDefender();
}

// Economy events
@EventHandler
public void onPlayerBalanceChange(PlayerBalanceChangeEvent event) {
    Player player = event.getPlayer();
    double oldBalance = event.getOldBalance();
    double newBalance = event.getNewBalance();
}
```

### Wrappers de Données

Les objets retournés par l'API sont des **wrappers read-only** pour protéger l'état interne :

```java
public interface TanTown extends TanTerritory {
    String getID();
    String getName();
    String getDescription();
    TanPlayer getOwner();
    int getLevel();
    double getBalance();
    Collection<TanPlayer> getMembers();
    Collection<TanClaimedChunk> getClaimedChunks();
    Collection<TanProperty> getProperties();
    Collection<TanLandmark> getLandmarksOwned();
    // Méthodes read-only uniquement
}
```

---

## ⚡ Performance

### Optimisations Folia

**Coconation** est natif Folia et utilise les patterns async/await pour éviter les blocages :

```java
// ❌ Ancien pattern (bloquant)
TownData town = storage.getSync(townId); // Bloque 50-200ms

// ✅ Pattern Folia (non-bloquant)
storage.get(townId)
    .thenAccept(town -> {
        // Traitement async
    })
    .exceptionally(err -> {
        // Gestion erreurs
        return null;
    });
```

### Système de Cache Bi-Niveaux

1. **Cache Local (Guava)** :
   - TTL : 3 minutes
   - Limite : 10 000 entrées par storage
   - Latence : <5ms
   - Éviction LRU automatique

2. **Cache Redis** (optionnel) :
   - TTL : 5 minutes
   - Cross-server synchronization
   - Latence : ~15ms
   - Pub/Sub pour invalidation

### Batch Write Optimizer

Les écritures sont regroupées pour réduire les connexions DB :

```yaml
database:
  batch_write_interval: 20  # Flush toutes les 20 ticks
  batch_write_size: 100     # Flush si 100+ éléments en queue
```

**Impact** : 80-90% de réduction des requêtes DB pendant forte activité.

### Indexes Base de Données

Toutes les tables ont des index optimisés :

```sql
-- Exemple : table towns
CREATE INDEX idx_town_owner ON towns(owner_uuid);
CREATE INDEX idx_town_region ON towns(region_id);
CREATE INDEX idx_town_name ON towns(name);

-- Exemple : table claimed_chunks
CREATE INDEX idx_chunk_location ON claimed_chunks(world, x, z);
CREATE INDEX idx_chunk_owner ON claimed_chunks(owner_id);
```

### Métriques de Performance

Activer Prometheus pour monitoring :

```yaml
monitoring:
  prometheus:
    enabled: true
    port: 9090
```

**Métriques disponibles** :
- `tan_cache_hit_rate` : taux de hit cache
- `tan_db_query_duration_seconds` : latence requêtes SQL
- `tan_async_task_duration_seconds` : durée tâches async
- `tan_towns_total` : nombre de villes actives
- `tan_players_online` : joueurs connectés avec ville

---

## 🛠️ Développement

### Build du Plugin

```bash
# Clone du repository
git clone https://github.com/Leralix/Towns-And-Nations.git
cd Towns-And-Nations

# Build complet (Gradle Wrapper)
./gradlew build

# JAR de production (avec dépendances)
./gradlew shadowJar
# Sortie : tan-core/build/libs/Coconation-X.X.X.jar

# Tests unitaires
./gradlew test

# Rapport de couverture
./gradlew test jacocoTestReport
# Rapport : tan-core/build/reports/jacoco/test/html/index.html

# Formatage du code (Google Java Format)
./gradlew spotlessApply
```

### Environnement de Développement

**IDE Recommandé** : IntelliJ IDEA avec plugins :
- Lombok
- Google Java Format
- JUnit 5

**Configuration** :
1. Importer projet Gradle
2. SDK : Java 21 (Temurin ou Zulu)
3. Activer annotation processing (Lombok)
4. Code style : Google Java Format

### Structure des Tests

```
tan-core/src/test/java/
├── org.leralix.tan/
│   ├── economy/      # Tests économie
│   ├── storage/      # Tests persistance
│   ├── dataclass/    # Tests modèles
│   └── utils/        # Tests utilitaires
```

**Framework** : JUnit 5 + MockBukkit

**Exemple** :
```java
@ExtendWith(MockBukkitExtension.class)
class TownDataTest {
    @Test
    void testTownCreation() {
        TownData town = new TownData("TestTown", playerUUID);
        assertEquals("TestTown", town.getName());
    }
}
```

### Contribuer

1. **Fork** le projet
2. **Créer** une branche feature : `git checkout -b feature/ma-fonctionnalite`
3. **Commit** : `git commit -m "feat: ajout de ma fonctionnalité"`
4. **Push** : `git push origin feature/ma-fonctionnalite`
5. **Pull Request** vers `main`

**Standards** :
- Code formaté avec Google Java Format
- Tests unitaires pour nouvelle logique
- Documentation JavaDoc pour API publique
- Messages de commit conventionnels

---

## 🐛 Support

### Discord

Rejoignez notre serveur Discord pour :
- Signaler des bugs
- Proposer des fonctionnalités
- Obtenir de l'aide
- Partager vos créations

🔗 **[discord.gg/Q8gZSFUuzb](https://discord.gg/Q8gZSFUuzb)**

### Issues GitHub

Pour les bugs techniques :
1. Vérifier [Issues existantes](https://github.com/Leralix/Towns-And-Nations/issues)
2. Créer une nouvelle issue avec :
   - Version du plugin
   - Version de Folia/Paper
   - Logs d'erreur complets
   - Steps to reproduce

### Wiki

Documentation utilisateur complète :
🔗 **[Wiki GitHub](https://github.com/Leralix/Towns-And-Nations/wiki)**

---

## 📄 License

Ce projet est sous licence **GNU General Public License v3.0**.

Voir [LICENSE](LICENSE) pour plus de détails.

---

## 👥 Crédits

**Développeur principal** : Leralix  
**Contributeurs** : [Liste des contributeurs](https://github.com/Leralix/Towns-And-Nations/graphs/contributors)

**Remerciements** :
- PaperMC pour Folia API
- Triumph-GUI pour framework GUI
- HikariCP pour connection pooling
- Redisson pour client Redis
- MockBukkit pour framework de tests

---

## 🌟 Soutenez le Projet

Si vous appréciez Coconation, n'hésitez pas à :
- ⭐ **Star** le repository
- 🐛 **Signaler** les bugs
- 💡 **Suggérer** de nouvelles fonctionnalités
- 🛠️ **Contribuer** au code
- 📢 **Partager** avec votre communauté

---

<div align="center">

**Coconation** - Façonnez votre empire virtuel 🏰

[![Discord](https://img.shields.io/discord/YOUR_DISCORD_ID?label=Discord&logo=discord&style=flat-square)](https://discord.gg/Q8gZSFUuzb)
[![GitHub Stars](https://img.shields.io/github/stars/Leralix/Towns-And-Nations?style=flat-square)](https://github.com/Leralix/Towns-And-Nations/stargazers)
[![GitHub Issues](https://img.shields.io/github/issues/Leralix/Towns-And-Nations?style=flat-square)](https://github.com/Leralix/Towns-And-Nations/issues)

</div>
