# Plan de Refactoring - TownData

## 📊 Analyse Actuelle

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     TOWNDATA - ÉTAT ACTUEL                                │
│                                                                              │
│  Taille: 986 lignes                                                           │
│  Responsabilités: 9+                                                       │
│  Héritage: TownData → TerritoryData                                         │
│                                                                              │
│  ╔════════════════════════════════════════════════════════════════════╗   │
│  │ RESPONSIBILITÉS MÉLANGÉES                                              │   │
│  ╠════════════════════════════════════════════════════════════════════╣   │
│  │ 1. DOMAINE       │ addPlayer(), removePlayer(), getRank()           │   │
│  │ 2. PERSISTENCE   │ save(), delete(), update()                        │   │
│  │ 3. GUI           │ getIconWithName(), getOrderedMemberList()        │   │
│  │ 4. COMMUNICATION │ broadCastMessage(), broadcastMessageWithSound() │   │
│  │ 5. PROPERTIES    │ getProperty(), registerNewProperty()             │   │
│  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │
│  │ 6. ECONOMIE      │ getProgression(), getPrestigePoints(), getTownTier()│   │
│  │ 7. DIPLOMATIE    │ getPotentialVassals(), removeOverlord()        │   │
│  │ 8. CLAIMS/TERRITORY │ abstractClaimChunk(), setCapitalLocation()   │   │
│  │ 9. ASYNC OPS     │ addPlayerJoinRequestAsync(), kickPlayerAsync()  │   │
│  ╚════════════════════════════════════════════════════════════════════╝   │
│                                                                              │
│  Problème: Violation SRP (Single Responsibility Principle)                   │
│  Impact: Difficile à tester, maintenir, étendre                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🎯 OBJECTIF DU REFACTORING

```
AVANT:                          APRÈS:
┌──────────────┐                ┌──────────────────────────────────────┐
│  TownData    │                │  TownData (Data Class Pure)         │
│  986 lignes  │                │  - Seulement données et getters        │
│  9 responsa  │                │  - Aucune logique métier           │
│  Difficile à  │                │  - Aucune présentation             │
│  tester       │                │  - Aucune persistance              │
└──────────────┘                └──────────────────────────────────────┘
                                         │
                                         ▼
                    ┌──────────────────────────────────────────────┐
                    │  Services (Logique Métier)               │
                    │                                           │
                    │  ┌───────────────┐  ┌──────────────────┐  │
                    │  │ TownService   │  │  MemberService  │  │
                    │  │  - addPlayer  │  │  - getRank()     │  │
                    │  │  - removePlayer│  │  - getMembers()  │  │
                    │  └───────────────┘  └──────────────────┘  │
                    │                                           │
                    │  ┌───────────────┐  ┌──────────────────┐  │
                    │  │ GuiService    │  │  DiplomacyService│  │
                    │  │  - getIcon()   │  │  - getVassals()  │  │
                    │  └───────────────┘  └──────────────────┘  │
                    │                                           │
                    │  ┌───────────────┐  ┌──────────────────┐  │
                    │  │ EconomyService│  │ PropertyService │  │
                    │  │  - getTier()  │  │  - getProperty()│  │
                    │  └───────────────┘  └──────────────────┘  │
                    └──────────────────────────────────────────────┘
```

---

## 📋 PLAN DE REFACTORING

### PHASE 1: Préparation (Semaine 1)

#### 1.1 Créer les nouvelles classes de structure

```java
package org.leralix.tan.domain.town;

/**
 * PURE DATA CLASS - Ne contient que des données
 * Aucune logique métier, aucune présentation, aucune persistance
 */
public final class TownData {
    private final String id;
    private final String name;
    private final String leaderId;
    private String townTag;
    private boolean isRecruiting;
    private Vector2D capitalLocation;
    private TeleportationPosition spawn;
    private Set<String> playerIds;           // Copie défensive
    private Set<String> purchasedUpgrades;
    private String regionId;                  // null = independant

    // Getters uniquement
    public String getId() { return id; }
    public String getName() { return name; }
    public String getLeaderId() { return leaderId; }
    public Set<String> getPlayerIds() { return Set.copyOf(playerIds); }
    // ...
}
```

#### 1.2 Créer les interfaces de service

```java
package org.leralix.tan.domain.town;

public interface TownService {
    CompletableFuture<TownData> getTown(String id);
    CompletableFuture<Void> addPlayer(String townId, String playerId);
    CompletableFuture<Void> removePlayer(String townId, String playerId);
    CompletableFuture<Boolean> isFull(String townId);
}

public interface MemberService {
    CompletableFuture<RankData> getRank(String townId, String playerId);
    CompletableFuture<List<ITanPlayer>> getMembers(String townId);
}

public interface TownGuiService {
    CompletableFuture<ItemStack> getIcon(String townId, LangType lang);
    CompletableFuture<List<GuiItem>> getMemberList(String townId, ITanPlayer viewer);
}
```

---

### PHASE 2: Extraction (Semaines 2-3)

#### 2.1 Extraire MemberService

```java
package org.leralix.tan.domain.town;

public class MemberServiceImpl implements MemberService {
    private final PlayerDataStorage playerStorage;
    private final RankDataStorage rankStorage;

    @Override
    public CompletableFuture<RankData> getRank(String townId, String playerId) {
        return TownDataStorage.getInstance().get(townId)
            .thenCompose(town -> {
                String rankId = town.getRankId(playerId);
                return rankStorage.get(rankId);
            });
    }

    @Override
    public CompletableFuture<List<ITanPlayer>> getMembers(String townId) {
        return TownDataStorage.getInstance().get(townId)
            .thenCompose(town -> {
                CompletableFuture<List<ITanPlayer>> futures =
                    CompletableFuture.completedFuture(new ArrayList<>());
                for (String playerId : town.getPlayerIds()) {
                    futures = futures.thenCombine(
                        playerStorage.get(playerId),
                        (list, player) -> { list.add(player); return list; }
                    );
                }
                return futures;
            });
    }
}
```

#### 2.2 Extraire GuiService

```java
package org.leralix.tan.domain.gui;

public class TownGuiServiceImpl implements TownGuiService {
    private final HeadUtils headUtils;

    @Override
    public CompletableFuture<ItemStack> getIcon(String townId, LangType langType) {
        return TownDataStorage.getInstance().get(townId)
            .thenApply(town -> {
                ItemStack head = headUtils.getHead(town.getLeaderId());
                ItemMeta meta = head.getItemMeta();
                // ... formatting
                return head;
            });
    }

    @Override
    public CompletableFuture<List<GuiItem>> getMemberList(String townId, ITanPlayer viewer) {
        return memberService.getMembers(townId)
            .thenApply(members -> {
                List<GuiItem> items = new ArrayList<>();
                for (ITanPlayer member : members) {
                    items.add(createMemberItem(member, viewer));
                }
                return items;
            });
    }
}
```

#### 2.3 Extraire EconomyService

```java
package org.leralix.tan.domain.economy;

public class TownEconomyServiceImpl implements TownEconomyService {
    @Override
    public CompletableFuture<TownTier> getTownTier(String townId) {
        return TownDataStorage.getInstance().get(townId)
            .thenApply(TownData::getTownTier);
    }

    @Override
    public CompletableFuture<Long> getPrestigeBalance(String townId) {
        return TownDataStorage.getInstance().get(townId)
            .thenApply(TownData::getPrestigeBalance);
    }
}
```

---

### PHASE 3: Migration Graduelle (Semaines 4-5)

#### 3.1 Stratégie: Strangler Fig Pattern

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    TOWNDATA ACTUEL                                    │
│                                                                              │
│  addPlayer() {                                                              │
│    // 1. Logique métier                                                     │
│    playerJoinRequestSet.add(id);                                        │
│    townPlayerListId.add(id);                                             │
│    save();                                                                 │
│    // 2. Notification                                                      │
│    broadcastMessage(...);                                                 │
│    // 3. GUI                                                              │
│    updateScoreboard();                                                    │
│  }                                                                          │
└─────────────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    REFACTORING ÉTAPE 1                              │
│                                                                              │
│  addPlayer() {                                                              │
│    return TownService.addPlayer(this)  // Délégation                      │
│      .thenRun(() -> {                                                       │
│        // Ne conserver que la notification dans TownData                  │
│        broadcastMessage(...);                                             │
│      });                                                                    │
│  }                                                                          │
└─────────────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    REFACTORING ÉTAPE 2                              │
│                                                                              │
│  addPlayer() {                                                              │
│    // Délégation complète                                                 │
│    return TownService.addPlayer(this);                                    │
│  }                                                                          │
│  // Notification gérée par le service ou par events                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 3.2 Ordre de Migration des Méthodes

| Méthode | Complexité | Priorité | Nouvelle Classe |
|---------|-----------|----------|----------------|
| `getIconWithName()` | Faible | 1 | `TownGuiService` |
| `getTownTag()` | Faible | 1 | Garder (getter) |
| `getLeaderID()` | Faible | 1 | Garder (getter) |
| `addPlayer()` | Moyenne | 2 | `TownService` |
| `removePlayer()` | Moyenne | 2 | `TownService` |
| `getRank()` | Moyenne | 2 | `MemberService` |
| `broadCastMessage()` | Faible | 2 | `NotificationService` |
| `getProperty()` | Faible | 3 | `PropertyService` |
| `getProgression()` | Faible | 3 | `TownEconomyService` |
| `getTownTier()` | Faible | 3 | `TownEconomyService` |
| `getPotentialVassals()` | Élevée | 4 | `DiplomacyService` |
| `abstractClaimChunk()` | Élevée | 4 | `ClaimService` |

---

### PHASE 4: Tests (Semaine 6)

#### 4.1 Tests Unitaires

```java
@Test
@DisplayName("TownService should add player correctly")
void testAddPlayer() {
    // Given
    String townId = "town_test";
    String playerId = "player_test";

    // When
    townService.addPlayer(townId, playerId).join();

    // Then
    TownData town = townService.getTown(townId).join();
    assertTrue(town.getPlayerIds().contains(playerId));
}

@Test
@DisplayName("MemberService should return correct rank")
void testGetRank() {
    // Given
    String townId = "town_test";
    String playerId = "player_test";

    // When
    RankData rank = memberService.getRank(townId, playerId).join();

    // Then
    assertNotNull(rank);
    assertEquals("Member", rank.getName());
}
```

#### 4.2 Tests d'Intégration

```java
@SpringBootTest
@DisplayName("TownService integration should work end-to-end")
void testAddPlayerIntegration() {
    // Given
    Player player = mockPlayer();
    TownData town = new TownData(...);

    // When
    townService.addPlayer(town.getId(), player.getUniqueId()).join();

    // Then
    TownData updated = townService.getTown(town.getId()).join();
    assertTrue(updated.getPlayerIds().contains(player.getUniqueId()));
}
```

---

### PHASE 5: Nettoyage (Semaine 7)

#### 5.1 Code Supprimé de TownData

- ❌ Méthodes déplacées vers services
- ❌ Imports inutiles (Triumph-GUI, etc.)
- ❌ Logique métier dupliquée

#### 5.2 Ajout de Deprecation Warnings

```java
@Deprecated(forRemoval = true)
public ItemStack getIconWithName() {
    throw new UnsupportedOperationException("Use TownGuiService.getIcon() instead");
}
```

---

## 📊 DIAGRAMME DE SÉQUENCE POST-REFACTORING

```
JOUEUR          TOWNDATA          SERVICES          STORAGE
  │                 │                   │                │
  │ /town add       │                   │                │
  └───────────────>│                   │                │
                    │                   │                │
                    ▼                   ▼                ▼
            ┌──────────────────────────────────────────────────────┐
            │  townService.addPlayer(townId, playerId)          │
            └──────────────────────────────────────────────────────┘
                              │
                    ┌──────────────┬──────────────┐
                    ▼              ▼              ▼
            ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
            │ PlayerService│ │ MemberService│ │ CacheService  │
            │ (addPlayer)   │ │ (getRank)    │ │ (invalidate)  │
            └──────────────┘ └──────────────┘ └──────────────┘
                    │              │              │
                    └──────────────┼──────────────┘
                                   ▼
                    ┌──────────────────────────────────┐
                    │  townDataStorage.save(town)    │
            └──────────────────────────────────────┘
                                   │
                                   ▼
                        ┌───────────────────────────────┐
                        │  MySQL UPDATE town_data SET ... │
                        └───────────────────────────────┘
                                   │
                    ┌──────────────┬──────────────┐
                    ▼              ▼              ▼
            ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
            │ EventService │ │ Notification│ │ GuiService   │
            │ (publish)     │ │  (notify)   │ │  (icon)      │
            └──────────────┘ └──────────────┘ └──────────────┘
```

---

## 📁 NOUVELLE STRUCTURE DE PACKAGES

```
org.leralix.tan.domain/
├── town/
│   ├── TownData.java              (Pure data class - 200 lignes)
│   ├── TownService.java           (Interface)
│   ├── TownServiceImpl.java        (Implementation)
│   ├── MemberService.java         (Interface)
│   ├── MemberServiceImpl.java    (Implementation)
│   └── TownFactory.java          (Factory pattern)
│
├── gui/
│   ├── TownGuiService.java       (Interface)
│   └── TownGuiServiceImpl.java    (Implementation)
│
├── economy/
│   ├── TownEconomyService.java   (Interface)
│   └── TownEconomyServiceImpl.java(Implementation)
│
├── diplomacy/
│   ├── DiplomacyService.java     (Interface)
│   └── DiplomacyServiceImpl.java(Implementation)
│
├── property/
│   ├── PropertyService.java      (Interface)
│   └── PropertyServiceImpl.java   (Implementation)
│
└── notification/
    ├── NotificationService.java  (Interface)
    └── NotificationServiceImpl.java(Implementation)
```

---

## 🔧 CODE EXAMPLES

### AVANT (Actuel - 986 lignes)

```java
public class TownData extends TerritoryData {
    // ... 986 lignes de mélanges

    public ItemStack getIconWithName() {
        // GUI logic
        return headUtils.getHead(uuidLeader);
    }

    public void addPlayer(ITanPlayer player) {
        // Domain logic
        playerJoinRequestSet.add(id);
        townPlayerListId.add(id);

        // Persistence
        TownDataStorage.getInstance().putSync(this);

        // Notification
        broadcastMessage(...);

        // GUI
        updateScoreboard();
    }

    public PropertyData getProperty(String id) {
        // Property logic
        return propertyDataMap.get(id);
    }

    public TownTier getTownTier() {
        // Economy logic
        return progression.getCurrentTier();
    }

    // ... 40+ autres méthodes
}
```

### APRÈS (Refactorisé)

```java
// PURE DATA CLASS (200 lignes)
public final class TownData {
    private final String id;
    private final String name;
    private final String leaderId;
    private String townTag;
    private boolean isRecruiting;
    private Vector2D capitalLocation;
    private Set<String> playerIds;
    // ... getters only
}

// DOMAIN SERVICE
public class TownServiceImpl {
    public CompletableFuture<Void> addPlayer(String townId, String playerId) {
        return validateCanJoin(townId, playerId)
            .thenCompose(valid -> {
                if (!valid) throw new JoinException();
                return updateTownData(townId, town -> {
                    town.addPlayerId(playerId);
                    return town;
                });
            })
            .thenCompose(town -> {
                return townStorage.save(town);
            })
            .thenCompose(town -> {
                return notificationService.notifyPlayerJoined(playerId, townId);
            });
    }
}

// GUI SERVICE
public class TownGuiServiceImpl {
    public CompletableFuture<ItemStack> getIcon(String townId, LangType lang) {
        return townStorage.get(townId)
            .thenApply(town -> headUtils.getHead(town.getLeaderId()));
    }
}
```

---

## 📈 BÉNÉFICES ATTENDUS

| Aspect | Avant | Après |
|--------|-------|-------|
| **Taille classe** | 986 lignes | 200 lignes (data) |
| **Testabilité** | Difficile | Facile (services mockables) |
| **Maintenabilité** | Complexe | Simple (1 classe = 1 responsabilité) |
| **Extensibilité** | Risque de régression | Facile (ajouter service) |
| **Performance** | Non optimisé | Cache par service |
| **Folia** | Sync/async mélangé | Parfaitement async |
| **Couverture tests** | ~15% | 70-80%+ |

---

## ⚠️ RISQUES & ATTÉNUATIONS

### Risques

1. **Breaking Changes** - API publique modifiée
2. **Temps estimé** - 6-7 semaines de travail
3. **Tests** - 413 tests à mettre à jour
4. **Documentation** - À mettre à jour

### Atténuations

1. **Migration Progressive** - Utiliser le pattern Strangler Fig
2. **Backward Compatibility** - Garder les anciennes méthodes comme deprecated
3. **Tests Continus** - Tests à mettre à jour en même temps
4. **Parallel Development** - Possibilité de développer en parallèle

---

## 🎯 CHECKLIST DE VALIDATION

- [ ] Services créés avec interfaces claires
- [ ] TownData réduite à data pure
- [ ] Tests unitaires pour chaque service
- [ ] Tests d'intégration mis à jour
- [ ] Documentation mise à jour
- [ ] Code review effectuée
- [ ] Performance testée
- [ **Tous les 413 tests passent**]

---

*Document de refactoring créé pour CocoWorld R2*
*Date: 2026-03-27*
*Estimation: 6-7 semaines de développement*
