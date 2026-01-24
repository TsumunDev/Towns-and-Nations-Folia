# Guide de Debug - Intégration VaultUnlockedAPI / zessentials

## Problème Actuel

Le plugin Towns and Nations ne parvient pas à utiliser l'économie de VaultUnlockedAPI/zessentials lors de la création de villes ou de nations.

## Diagnostic

### 1. Vérifier si VaultUnlockedAPI est chargé

Dans la console du serveur, cherchez :
```
[TaN] -Vault is detected, using ...
```

Si vous voyez :
```
[TaN] -No active vault economy. Running standalone...
```
Alors VaultUnlockedAPI n'est **pas détecté**.

### 2. Vérifier l'enregistrement du service Economy

VaultUnlockedAPI doit enregistrer un service `net.milkbowl.vault.economy.Economy`.

Test avec cette commande en jeu :
```
/vault view
```

Ou vérifiez avec un plugin comme PlugManX :
```
/plugins
```
Vérifiez que VaultUnlockedAPI est bien activé.

### 3. Vérifier la priorité du service

Parfois, plusieurs plugins économiques entrent en conflit. Vérifiez quel plugin fournit le service Economy.

## Solution Temporaire

Si VaultUnlockedAPI ne fonctionne pas, vous pouvez :

**Option 1: Utiliser l'économie interne de TAN**
```yaml
# config.yml de TAN
Economy:
  UseStandalone: true
```

**Option 2: Utiliser un autre plugin Vault compatible**
- EssentialsX
- CMI
- CoinEngine

## Solution Permanente (à implémenter)

Créer un intégrateur direct pour zessentials/VaultUnlockedAPI sans passer par Vault.

## Logs à Fournir

Pour un debug plus approfondi, fournissez :
1. Le log de démarrage complet du serveur
2. Le résultat de la commande `/plugins`
3. Le résultat de `/vault view` si disponible
4. Le log au moment où vous essayez de créer une ville
