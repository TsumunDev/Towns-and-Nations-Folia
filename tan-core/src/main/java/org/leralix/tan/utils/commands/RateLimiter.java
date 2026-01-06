package org.leralix.tan.utils.commands;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
public final class RateLimiter {
  private final ConcurrentHashMap<UUID, Long> lastExecutionTimes;
  private final long cooldownMillis;
  private final String commandName;
  public RateLimiter(String commandName, long cooldown, TimeUnit unit) {
    this.commandName = commandName;
    this.cooldownMillis = unit.toMillis(cooldown);
    this.lastExecutionTimes = new ConcurrentHashMap<>();
  }
  public RateLimiter(String commandName, long cooldownSeconds) {
    this(commandName, cooldownSeconds, TimeUnit.SECONDS);
  }
  public RateLimitResult canExecute(UUID playerId) {
    long now = System.currentTimeMillis();
    Long lastExecution = lastExecutionTimes.get(playerId);
    if (lastExecution == null || (now - lastExecution) >= cooldownMillis) {
      lastExecutionTimes.put(playerId, now);
      return RateLimitResult.allowed();
    }
    long remainingMillis = cooldownMillis - (now - lastExecution);
    return RateLimitResult.denied(remainingMillis);
  }
  public RateLimitResult checkOnly(UUID playerId) {
    long now = System.currentTimeMillis();
    Long lastExecution = lastExecutionTimes.get(playerId);
    if (lastExecution == null || (now - lastExecution) >= cooldownMillis) {
      return RateLimitResult.allowed();
    }
    long remainingMillis = cooldownMillis - (now - lastExecution);
    return RateLimitResult.denied(remainingMillis);
  }
  public void resetCooldown(UUID playerId) {
    lastExecutionTimes.remove(playerId);
  }
  public long getRemainingCooldown(UUID playerId) {
    Long lastExecution = lastExecutionTimes.get(playerId);
    if (lastExecution == null) {
      return 0;
    }
    long elapsed = System.currentTimeMillis() - lastExecution;
    return Math.max(0, cooldownMillis - elapsed);
  }
  public void clearAll() {
    lastExecutionTimes.clear();
  }
  public String getCommandName() {
    return commandName;
  }
  public long getCooldownMillis() {
    return cooldownMillis;
  }
  public static final class RateLimitResult {
    private final boolean allowed;
    private final long remainingMillis;
    private RateLimitResult(boolean allowed, long remainingMillis) {
      this.allowed = allowed;
      this.remainingMillis = remainingMillis;
    }
    static RateLimitResult allowed() {
      return new RateLimitResult(true, 0);
    }
    static RateLimitResult denied(long remainingMillis) {
      return new RateLimitResult(false, Math.max(0, remainingMillis));
    }
    public boolean isAllowed() {
      return allowed;
    }
    public long getRemainingMillis() {
      return remainingMillis;
    }
    public long getRemainingSeconds() {
      return (remainingMillis + 999) / 1000;
    }
  }
}