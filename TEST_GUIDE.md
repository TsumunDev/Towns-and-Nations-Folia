# 🧪 TEST GUIDE - Quest & Prestige System POC

## 📋 Prérequis

1. **Compiler le projet**:
   ```bash
   cd Towns-and-Nations-folia
   ./gradlew build
   ```

2. **Démarrer le serveur** avec le plugin chargé

## 🎮 Procédure de Test

### Étape 1: Créer une ville

```
/town create MaVille
```

**Résultat attendu**: Ville créée, vous êtes le maire

### Étape 2: Ouvrir le menu des quêtes

```
/coconationdebug quest
```

**Résultat attendu**:
- GUI s'ouvre avec la quête "Wheat Harvest I"
- La quête affiche:
  - Nom: "Wheat Harvest I"
  - Description: "Harvest 50 wheat for your town."
  - Type: Farming
  - Required Tier: Camping
  - Rewards: 100 XP, 5 Prestige, $50

### Étape 3: Accepter la quête

**Cliquez sur l'icône blé** dans la GUI

**Résultat attendu**:
- Message: "§aQuest accepted: §eWheat Harvest I"
- La quête est maintenant en statut ACTIVE

### Étape 4: Casser du blé

**Cassez 50 blocs de blé** (mature wheat)

**Résultat attendu**:
- Chaque bloc cassé met à jour la progression de la quête
- À 50 blé, message: "§aQuest completed: §eWheat Harvest I"
- Message: "§7Click to claim rewards!"

### Étape 5: Réclamer les récompenses

Il n'y a pas encore de GUI pour réclamer, mais le système marque la quête comme complétée.

### Étape 6: Vérifier les gains

**Vérifiez que vous avez reçu**:
- ✅ 100 XP pour votre ville (progression)
- ✅ 5 Points de Prestige
- ✅ $50 (si Vault est installé)

## 🔍 Vérification Console

Regardez dans la console du serveur, vous devriez voir:

```
[TownsAndNations]: Initializing Progression Services
[TownsAndNations]: QuestService initialized with 1 quests.
[TownsAndNations]: PrestigeService initialized.
[TownsAndNations]: UpgradeService initialized with 3 upgrades.
[TownsAndNations]: Progression services initialized
[TownsAndNations]: Registering Quest Listeners
```

## 🐛 Débogage

Si ça ne fonctionne pas:

1. **Vérifiez les logs**:
   - Recherchez les erreurs dans la console
   - Les exceptions sont loggées avec `[TownsAndNations]`

2. **Vérifiez que vous êtes dans une ville**:
   ```
   /town info
   ```

3. **Test simple** - Créez une ville si vous n'en avez pas

## 📊 Ce qui est testé

✅ **Domain Layer**: Quest, PrestigePoints, TownUpgrade (records immuables)
✅ **Service Layer**: QuestService, PrestigeService, UpgradeService (singletons)
✅ **Event Listeners**: QuestBlockBreakListener détecte les casses de blocs
✅ **GUI**: QuestListMenu affiche les quêtes disponibles
✅ **Async Operations**: CompletableFuture pour DB operations
✅ **Thread Safety**: ConcurrentHashMap, records immuables

## 🎯 Prochaines étapes après test

Si le POC fonctionne:

1. **Ajouter plus de quêtes** dans `QuestServiceImpl.loadDefaultQuests()`
2. **Créer la GUI Prestige Shop** pour dépenser les points
3. **Ajouter les autres listeners** (BUILD, COMBAT déjà fait)
4. **Configuration YAML** pour définir quêtes/upgrades en fichier

## 📝 Notes Techniques

- **Quêtes répétables**: Après complétion, cooldown de 24h
- **Progression**: 100 XP = progression de la ville
- **Prestige**: 5 points par quête (système de monnaie)
- **Async**: Toutes les opérations DB sont non-blocking
- **Folia**: Compatible avec le threading régionalisé

---

**Bon test! 🚀**
