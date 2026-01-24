package org.leralix.tan.testutils;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Base class for Redis integration tests using TestContainers.
 *
 * <p>Provides a real Redis container for testing cache operations, pub/sub, and distributed
 * locking without mocking. Automatically starts a Redis 7.0 container before tests and stops it
 * after all tests complete.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * class MyCacheTest extends RedisTestBase {
 *     @Test
 *     void testCacheOperation() {
 *         try (Jedis jedis = getJedisPool().getResource()) {
 *             jedis.set("key", "value");
 *             assertEquals("value", jedis.get("key"));
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p><b>Benefits:</b>
 * <ul>
 *   <li>Real Redis behavior - no mocking inconsistencies
 *   <li>Test pub/sub, TTL, and advanced features
 *   <li>Fast startup - container caching, reusable across tests
 *   <li>Production-like testing - same commands, same performance characteristics
 * </ul>
 *
 * @see org.testcontainers.containers.GenericContainer
 * @see redis.clients.jedis.JedisPool
 */
@Testcontainers
public abstract class RedisTestBase {

  /** Redis 7.0 container with default configuration. Reused across all test methods. */
  @Container
  protected static final GenericContainer<?> redisContainer =
      new GenericContainer<>(DockerImageName.parse("redis:7.0-alpine"))
          .withExposedPorts(6379)
          .withReuse(true); // Reuse container for faster tests

  /** Jedis connection pool configured for test Redis instance. */
  protected static JedisPool jedisPool;

  /**
   * Starts Redis container and initializes connection pool before any tests run.
   *
   * <p>Container startup takes ~1-3 seconds on first run, then cached for subsequent runs.
   */
  @BeforeAll
  static void setupRedis() {
    // Container auto-starts via @Testcontainers annotation
    JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(10);
    poolConfig.setMaxIdle(5);
    poolConfig.setMinIdle(2);
    poolConfig.setTestOnBorrow(true);

    String host = redisContainer.getHost();
    Integer port = redisContainer.getMappedPort(6379);

    jedisPool = new JedisPool(poolConfig, host, port);
  }

  /**
   * Stops Redis container and closes connection pool after all tests complete.
   *
   * <p>Resources are automatically cleaned up by TestContainers framework.
   */
  @AfterAll
  static void teardownRedis() {
    if (jedisPool != null && !jedisPool.isClosed()) {
      jedisPool.close();
    }
    // Container auto-stops via @Testcontainers annotation
  }

  /**
   * Gets the Jedis connection pool for test Redis instance.
   *
   * <p>Use this to obtain connections for your test operations:
   *
   * <pre>{@code
   * try (Jedis jedis = getJedisPool().getResource()) {
   *     jedis.set("player:uuid-123", "{\"name\":\"TestPlayer\"}");
   *     String data = jedis.get("player:uuid-123");
   *     // ... assertions
   * }
   * }</pre>
   *
   * @return Active Jedis connection pool connected to Redis container
   */
  protected static JedisPool getJedisPool() {
    return jedisPool;
  }

  /**
   * Gets Redis container instance for advanced configuration.
   *
   * <p>Useful for retrieving connection details:
   *
   * <pre>{@code
   * String host = getRedisContainer().getHost();
   * int port = getRedisContainer().getMappedPort(6379);
   * }</pre>
   *
   * @return Running Redis container instance
   */
  protected static GenericContainer<?> getRedisContainer() {
    return redisContainer;
  }

  /**
   * Flushes all data from Redis test instance.
   *
   * <p>Useful for test cleanup to ensure isolation:
   *
   * <pre>{@code
   * @AfterEach
   * void cleanup() {
   *     flushRedis();
   * }
   * }</pre>
   *
   * <p><b>Warning:</b> This clears ALL keys in the Redis instance. Safe for tests since each test
   * run gets an isolated container.
   */
  protected void flushRedis() {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.flushAll();
    }
  }

  /**
   * Sets a key with value and optional TTL in Redis.
   *
   * <p>Convenience method for test data setup:
   *
   * <pre>{@code
   * setRedisKey("player:uuid-123", "{\"name\":\"TestPlayer\"}", 60); // 60 second TTL
   * }</pre>
   *
   * @param key Redis key
   * @param value Value to store
   * @param ttlSeconds Time-to-live in seconds (0 for no expiration)
   */
  protected void setRedisKey(String key, String value, int ttlSeconds) {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.set(key, value);
      if (ttlSeconds > 0) {
        jedis.expire(key, ttlSeconds);
      }
    }
  }

  /**
   * Gets a value from Redis by key.
   *
   * <p>Convenience method for assertions:
   *
   * <pre>{@code
   * String value = getRedisKey("player:uuid-123");
   * assertNotNull(value);
   * }</pre>
   *
   * @param key Redis key
   * @return Value stored at key, or null if key doesn't exist
   */
  protected String getRedisKey(String key) {
    try (Jedis jedis = jedisPool.getResource()) {
      return jedis.get(key);
    }
  }

  /**
   * Checks if a key exists in Redis.
   *
   * <p>Useful for cache hit/miss testing:
   *
   * <pre>{@code
   * assertTrue(redisKeyExists("player:uuid-123"));
   * assertFalse(redisKeyExists("player:nonexistent"));
   * }</pre>
   *
   * @param key Redis key to check
   * @return true if key exists, false otherwise
   */
  protected boolean redisKeyExists(String key) {
    try (Jedis jedis = jedisPool.getResource()) {
      return jedis.exists(key);
    }
  }
}
