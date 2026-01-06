package org.leralix.tan.utils.commands;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
public final class RateLimitRegistry {
  private static volatile RateLimitRegistry instance;
  private final Map<String, RateLimiter> limiters;
  private static final long DEFAULT_COMMAND_COOLDOWN = 0;
  private static final long FAST_COMMAND_COOLDOWN = 1;
  private static final long NORMAL_COMMAND_COOLDOWN = 2;
  private static final long SLOW_COMMAND_COOLDOWN = 5;
  private RateLimitRegistry() {
    this.limiters = new ConcurrentHashMap<>();
    initializeDefaults();
  }
  public static RateLimitRegistry getInstance() {
    if (instance == null) {
      synchronized (RateLimitRegistry.class) {
        if (instance == null) {
          instance = new RateLimitRegistry();
        }
      }
    }
    return instance;
  }
  private void initializeDefaults() {
    registerLimiter("balance", FAST_COMMAND_COOLDOWN);
    registerLimiter("seebalance", FAST_COMMAND_COOLDOWN);
    registerLimiter("map", NORMAL_COMMAND_COOLDOWN);
    registerLimiter("claim", NORMAL_COMMAND_COOLDOWN);
    registerLimiter("unclaim", NORMAL_COMMAND_COOLDOWN);
    registerLimiter("autoclaim", NORMAL_COMMAND_COOLDOWN);
    registerLimiter("open", FAST_COMMAND_COOLDOWN);
    registerLimiter("newsletter", NORMAL_COMMAND_COOLDOWN);
    registerLimiter("pay", NORMAL_COMMAND_COOLDOWN);
    registerLimiter("deposit", NORMAL_COMMAND_COOLDOWN);
    registerLimiter("withdraw", NORMAL_COMMAND_COOLDOWN);
    registerLimiter("create", SLOW_COMMAND_COOLDOWN);
    registerLimiter("join", SLOW_COMMAND_COOLDOWN);
    registerLimiter("leave", NORMAL_COMMAND_COOLDOWN);
    registerLimiter("disband", SLOW_COMMAND_COOLDOWN);
    registerLimiter("spawn", FAST_COMMAND_COOLDOWN);
  }
  public RateLimiter registerLimiter(String commandName, long cooldownSeconds) {
    RateLimiter limiter = new RateLimiter(commandName, cooldownSeconds, TimeUnit.SECONDS);
    limiters.put(commandName, limiter);
    return limiter;
  }
  public RateLimiter registerLimiter(String commandName, long cooldown, TimeUnit unit) {
    RateLimiter limiter = new RateLimiter(commandName, cooldown, unit);
    limiters.put(commandName, limiter);
    return limiter;
  }
  public RateLimiter getLimiter(String commandName) {
    return limiters.get(commandName);
  }
  public RateLimiter getOrCreateLimiter(String commandName) {
    return limiters.computeIfAbsent(commandName,
        k -> new RateLimiter(commandName, DEFAULT_COMMAND_COOLDOWN, TimeUnit.SECONDS));
  }
  public RateLimiter.RateLimitResult canExecute(String commandName, UUID playerId) {
    RateLimiter limiter = getLimiter(commandName);
    if (limiter == null) {
      return RateLimiter.RateLimitResult.allowed();
    }
    return limiter.canExecute(playerId);
  }
  public RateLimiter.RateLimitResult checkOnly(String commandName, UUID playerId) {
    RateLimiter limiter = getLimiter(commandName);
    if (limiter == null) {
      return RateLimiter.RateLimitResult.allowed();
    }
    return limiter.checkOnly(playerId);
  }
  public void clearPlayerCooldowns(UUID playerId) {
    for (RateLimiter limiter : limiters.values()) {
      limiter.resetCooldown(playerId);
    }
  }
  public void clearAll() {
    for (RateLimiter limiter : limiters.values()) {
      limiter.clearAll();
    }
  }
}