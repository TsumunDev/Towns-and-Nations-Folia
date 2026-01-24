package org.leralix.tan.integration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.leralix.tan.testutils.RedisTestBase;
import redis.clients.jedis.Jedis;

/**
 * Example integration test demonstrating RedisTestBase usage.
 *
 * <p>This test shows how to use TestContainers for real Redis integration testing without mocking.
 * Tests cover basic key-value operations, TTL, pub/sub, and atomic operations.
 *
 * <p><b>Running this test:</b>
 *
 * <pre>
 * ./gradlew test --tests "org.leralix.tan.integration.RedisIntegrationExampleTest"
 * </pre>
 *
 * <p><b>Requirements:</b>
 * <ul>
 *   <li>Docker must be running (TestContainers needs it)
 *   <li>First run downloads Redis 7.0-alpine image (~30MB)
 *   <li>Subsequent runs reuse cached container (~1-3s startup)
 * </ul>
 */
class RedisIntegrationExampleTest extends RedisTestBase {

  /**
   * Cleans up Redis data after each test.
   *
   * <p>Ensures test isolation by flushing all keys.
   */
  @AfterEach
  void cleanup() {
    flushRedis();
  }

  /** Test: Basic set and get operations. */
  @Test
  void testSetAndGet() {
    // Given: A player data key-value pair
    String key = "player:uuid-123";
    String value = "{\"name\":\"TestPlayer\",\"balance\":1000.0}";

    // When: Setting the key
    setRedisKey(key, value, 0); // No TTL

    // Then: Value can be retrieved
    String retrieved = getRedisKey(key);
    assertEquals(value, retrieved, "Retrieved value should match stored value");
    assertTrue(redisKeyExists(key), "Key should exist in Redis");
  }

  /** Test: Key expiration with TTL. */
  @Test
  void testKeyExpiration() throws InterruptedException {
    // Given: A cache entry with 2-second TTL
    String key = "cache:town:uuid-456";
    String value = "{\"name\":\"ExpireTest\"}";

    // When: Setting key with TTL
    setRedisKey(key, value, 2); // 2 second TTL

    // Then: Key exists immediately
    assertTrue(redisKeyExists(key), "Key should exist immediately after set");

    // Wait for expiration
    Thread.sleep(3000); // Wait 3 seconds (longer than TTL)

    // Then: Key no longer exists
    assertFalse(redisKeyExists(key), "Key should be expired after TTL");
    assertNull(getRedisKey(key), "Expired key should return null");
  }

  /** Test: Atomic increment operations. */
  @Test
  void testAtomicIncrement() {
    String key = "counter:logins";

    try (Jedis jedis = getJedisPool().getResource()) {
      // Given: Initial counter value
      jedis.set(key, "10");

      // When: Incrementing atomically
      long newValue = jedis.incr(key);
      assertEquals(11, newValue, "Counter should increment by 1");

      // Then: Value persists correctly
      String storedValue = jedis.get(key);
      assertEquals("11", storedValue);
    }
  }

  /** Test: Hash operations (field-value pairs). */
  @Test
  void testHashOperations() {
    String key = "player:uuid-789";

    try (Jedis jedis = getJedisPool().getResource()) {
      // Given: A player with multiple fields
      jedis.hset(key, "name", "HashTestPlayer");
      jedis.hset(key, "balance", "5000.0");
      jedis.hset(key, "rank", "Mayor");

      // When: Retrieving specific fields
      String name = jedis.hget(key, "name");
      String balance = jedis.hget(key, "balance");
      String rank = jedis.hget(key, "rank");

      // Then: All fields are correct
      assertEquals("HashTestPlayer", name);
      assertEquals("5000.0", balance);
      assertEquals("Mayor", rank);

      // Then: Can retrieve all fields at once
      var allFields = jedis.hgetAll(key);
      assertEquals(3, allFields.size(), "Should have 3 fields");
    }
  }

  /** Test: List operations (queue/stack). */
  @Test
  void testListOperations() {
    String key = "queue:newsletter";

    try (Jedis jedis = getJedisPool().getResource()) {
      // Given: A queue of newsletter events
      jedis.rpush(key, "event1", "event2", "event3"); // Push to end

      // When: Popping from front (FIFO)
      String first = jedis.lpop(key);
      String second = jedis.lpop(key);

      // Then: Items are retrieved in order
      assertEquals("event1", first);
      assertEquals("event2", second);

      // Then: One item remains
      long remaining = jedis.llen(key);
      assertEquals(1, remaining);
    }
  }

  /** Test: Set operations (unique values). */
  @Test
  void testSetOperations() {
    String key = "town:members:uuid-town-1";

    try (Jedis jedis = getJedisPool().getResource()) {
      // Given: Adding members to a town
      jedis.sadd(key, "player-1", "player-2", "player-3");
      jedis.sadd(key, "player-2"); // Duplicate - should be ignored

      // When: Checking set size
      long memberCount = jedis.scard(key);

      // Then: Only unique members are stored
      assertEquals(3, memberCount, "Set should have 3 unique members");

      // Then: Can check membership
      assertTrue(jedis.sismember(key, "player-1"));
      assertFalse(jedis.sismember(key, "player-999"));
    }
  }

  /** Test: Sorted set (leaderboard use case). */
  @Test
  void testSortedSetLeaderboard() {
    String key = "leaderboard:balance";

    try (Jedis jedis = getJedisPool().getResource()) {
      // Given: Towns with different balances
      jedis.zadd(key, 5000.0, "TownAlpha");
      jedis.zadd(key, 10000.0, "TownBeta");
      jedis.zadd(key, 7500.0, "TownGamma");

      // When: Retrieving top 2 towns
      var topTowns = jedis.zrevrangeWithScores(key, 0, 1); // Top 2 by score

      // Then: Towns are ordered correctly
      var townsArray = topTowns.toArray();
      assertEquals(2, townsArray.length, "Should retrieve top 2 towns");

      // First place: TownBeta (10000.0)
      var firstPlace = (redis.clients.jedis.resps.Tuple) townsArray[0];
      assertEquals("TownBeta", firstPlace.getElement());
      assertEquals(10000.0, firstPlace.getScore(), 0.01);

      // Second place: TownGamma (7500.0)
      var secondPlace = (redis.clients.jedis.resps.Tuple) townsArray[1];
      assertEquals("TownGamma", secondPlace.getElement());
      assertEquals(7500.0, secondPlace.getScore(), 0.01);
    }
  }

  /** Test: Pipeline operations (batching). */
  @Test
  void testPipeline() {
    try (Jedis jedis = getJedisPool().getResource()) {
      var pipeline = jedis.pipelined();

      // Given: Batch setting multiple keys
      pipeline.set("key1", "value1");
      pipeline.set("key2", "value2");
      pipeline.set("key3", "value3");

      // When: Executing pipeline
      pipeline.sync(); // Send all commands at once

      // Then: All keys are set correctly
      assertEquals("value1", jedis.get("key1"));
      assertEquals("value2", jedis.get("key2"));
      assertEquals("value3", jedis.get("key3"));
    }
  }

  /** Test: Transaction with WATCH (optimistic locking). */
  @Test
  void testTransactionWithWatch() {
    String key = "balance:town-123";

    try (Jedis jedis = getJedisPool().getResource()) {
      // Given: Initial balance
      jedis.set(key, "1000.0");

      // When: Starting transaction with WATCH
      jedis.watch(key); // Watch for modifications

      var multi = jedis.multi();
      multi.set(key, "1500.0"); // Set new balance
      var response = multi.exec(); // Commit transaction

      // Then: Transaction succeeds (no concurrent modification)
      assertNotNull(response, "Transaction should succeed");
      assertEquals("1500.0", jedis.get(key));
    }
  }

  /** Test: Pub/Sub messaging (cache invalidation use case). */
  @Test
  void testPubSubCacheInvalidation() throws InterruptedException {
    String channel = "cache:invalidate";
    final String[] receivedMessage = {null};

    // Given: Subscriber listening for cache invalidation
    Thread subscriberThread =
        new Thread(
            () -> {
              try (Jedis subscriber = getJedisPool().getResource()) {
                subscriber.subscribe(
                    new redis.clients.jedis.JedisPubSub() {
                      @Override
                      public void onMessage(String ch, String msg) {
                        receivedMessage[0] = msg;
                        unsubscribe(); // Stop listening after first message
                      }
                    },
                    channel);
              }
            });

    subscriberThread.start();
    Thread.sleep(500); // Wait for subscriber to be ready

    // When: Publishing cache invalidation message
    try (Jedis publisher = getJedisPool().getResource()) {
      publisher.publish(channel, "town:uuid-123");
    }

    subscriberThread.join(2000); // Wait for message processing

    // Then: Subscriber received the message
    assertEquals("town:uuid-123", receivedMessage[0], "Subscriber should receive published message");
  }
}
