package org.leralix.tan.redis;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SentinelServersConfig;
import org.redisson.config.SingleServerConfig;
public class RedisClusterConfig {
  private static final Logger logger = Logger.getLogger(RedisClusterConfig.class.getName());
  public static RedissonClient createRedisClient(FileConfiguration config) {
    String mode = config.getString("redis.mode", "single").toLowerCase();
    logger.info("[TaN-Redis] Initializing Redis in mode: " + mode);
    switch (mode) {
      case "cluster":
        return createClusterClient(config);
      case "sentinel":
        return createSentinelClient(config);
      case "single":
      default:
        return createSingleClient(config);
    }
  }
  private static RedissonClient createSingleClient(FileConfiguration config) {
    Config redisConfig = new Config();
    String host = config.getString("redis.single.host", "127.0.0.1");
    int port = config.getInt("redis.single.port", 6379);
    String password = config.getString("redis.password", "");
    int database = config.getInt("redis.database", 0);
    String address = "redis://" + host + ":" + port;
    SingleServerConfig serverConfig =
        redisConfig
            .useSingleServer()
            .setAddress(address)
            .setDatabase(database)
            .setConnectionPoolSize(32)
            .setConnectionMinimumIdleSize(8)
            .setConnectTimeout(10000)
            .setRetryAttempts(3)
            .setRetryInterval(1000);
    if (password != null && !password.isEmpty()) {
      serverConfig.setPassword(password);
    }
    logger.info("[TaN-Redis] Single server mode: " + address);
    return Redisson.create(redisConfig);
  }
  private static RedissonClient createClusterClient(FileConfiguration config) {
    Config redisConfig = new Config();
    List<String> nodes = config.getStringList("redis.cluster.nodes");
    if (nodes == null || nodes.isEmpty()) {
      throw new IllegalArgumentException("Redis cluster nodes not configured in config.yml");
    }
    String password = config.getString("redis.password", "");
    ClusterServersConfig clusterConfig =
        redisConfig
            .useClusterServers()
            .setScanInterval(2000)
            .setMasterConnectionPoolSize(32)
            .setSlaveConnectionPoolSize(32)
            .setMasterConnectionMinimumIdleSize(8)
            .setSlaveConnectionMinimumIdleSize(8)
            .setConnectTimeout(10000)
            .setRetryAttempts(3)
            .setRetryInterval(1000);
    for (String node : nodes) {
      if (!node.startsWith("redis://")) {
        node = "redis://" + node;
      }
      clusterConfig.addNodeAddress(node);
    }
    if (password != null && !password.isEmpty()) {
      clusterConfig.setPassword(password);
    }
    logger.info("[TaN-Redis] Cluster mode with " + nodes.size() + " nodes");
    return Redisson.create(redisConfig);
  }
  private static RedissonClient createSentinelClient(FileConfiguration config) {
    Config redisConfig = new Config();
    String masterName = config.getString("redis.sentinel.master-name");
    List<String> sentinels = config.getStringList("redis.sentinel.nodes");
    if (masterName == null || masterName.isEmpty()) {
      throw new IllegalArgumentException("Redis sentinel master-name not configured");
    }
    if (sentinels == null || sentinels.isEmpty()) {
      throw new IllegalArgumentException("Redis sentinel nodes not configured");
    }
    String password = config.getString("redis.password", "");
    int database = config.getInt("redis.database", 0);
    SentinelServersConfig sentinelConfig =
        redisConfig
            .useSentinelServers()
            .setMasterName(masterName)
            .setDatabase(database)
            .setMasterConnectionPoolSize(32)
            .setSlaveConnectionPoolSize(32)
            .setMasterConnectionMinimumIdleSize(8)
            .setSlaveConnectionMinimumIdleSize(8)
            .setConnectTimeout(10000)
            .setRetryAttempts(3)
            .setRetryInterval(1000)
            .setScanInterval(2000);
    for (String sentinel : sentinels) {
      if (!sentinel.startsWith("redis://")) {
        sentinel = "redis://" + sentinel;
      }
      sentinelConfig.addSentinelAddress(sentinel);
    }
    if (password != null && !password.isEmpty()) {
      sentinelConfig.setPassword(password);
    }
    logger.info(
        "[TaN-Redis] Sentinel mode with master: "
            + masterName
            + ", "
            + sentinels.size()
            + " sentinels");
    return Redisson.create(redisConfig);
  }
}