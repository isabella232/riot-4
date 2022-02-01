package com.redis.riot.gen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import com.redis.lettucemod.search.CreateOptions;
import com.redis.lettucemod.search.Document;
import com.redis.lettucemod.search.Field;
import com.redis.lettucemod.search.Field.TextField.PhoneticMatcher;
import com.redis.lettucemod.search.SearchResults;
import com.redis.riot.AbstractRiotIntegrationTests;
import com.redis.testcontainers.RedisModulesContainer;
import com.redis.testcontainers.RedisServer;
import com.redis.testcontainers.junit.RedisTestContext;
import com.redis.testcontainers.junit.RedisTestContextsSource;

import io.lettuce.core.Range;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.api.sync.RedisHashCommands;
import io.lettuce.core.api.sync.RedisKeyCommands;
import io.lettuce.core.api.sync.RedisSetCommands;
import io.lettuce.core.api.sync.RedisSortedSetCommands;
import io.lettuce.core.api.sync.RedisStreamCommands;

@SuppressWarnings("unchecked")
class TestGen extends AbstractRiotIntegrationTests {

	private final RedisModulesContainer redisMod = new RedisModulesContainer(
			RedisModulesContainer.DEFAULT_IMAGE_NAME.withTag(RedisModulesContainer.DEFAULT_TAG));

	@Override
	protected Collection<RedisServer> redisServers() {
		Collection<RedisServer> servers = new ArrayList<>(super.redisServers());
		servers.add(redisMod);
		return servers;
	}

	@Override
	protected RiotGen app() {
		return new RiotGen();
	}

	@ParameterizedTest
	@RedisTestContextsSource
	void genFakerHash(RedisTestContext redis) throws Exception {
		execute("import-hset", redis);
		RedisKeyCommands<String, String> sync = redis.sync();
		List<String> keys = sync.keys("person:*");
		Assertions.assertEquals(1000, keys.size());
		Map<String, String> person = ((RedisHashCommands<String, String>) sync).hgetall(keys.get(0));
		Assertions.assertTrue(person.containsKey("firstName"));
		Assertions.assertTrue(person.containsKey("lastName"));
		Assertions.assertTrue(person.containsKey("address"));
	}

	@ParameterizedTest
	@RedisTestContextsSource
	void genFakerSet(RedisTestContext redis) throws Exception {
		execute("import-sadd", redis);
		RedisSetCommands<String, String> sync = redis.sync();
		Set<String> names = sync.smembers("got:characters");
		Assertions.assertTrue(names.size() > 10);
		for (String name : names) {
			Assertions.assertFalse(name.isEmpty());
		}
	}

	@ParameterizedTest
	@RedisTestContextsSource
	void genFakerZset(RedisTestContext redis) throws Exception {
		execute("import-zadd", redis);
		RedisKeyCommands<String, String> sync = redis.sync();
		List<String> keys = sync.keys("leases:*");
		Assertions.assertTrue(keys.size() > 100);
		String key = keys.get(0);
		Assertions.assertTrue(((RedisSortedSetCommands<String, String>) sync).zcard(key) > 0);
	}

	@ParameterizedTest
	@RedisTestContextsSource
	void genFakerStream(RedisTestContext redis) throws Exception {
		execute("import-xadd", redis);
		RedisStreamCommands<String, String> sync = redis.sync();
		List<StreamMessage<String, String>> messages = sync.xrange("teststream:1", Range.unbounded());
		Assertions.assertTrue(messages.size() > 0);
	}

	@Test
	void genFakerIndexIntrospection() throws Exception {
		Assumptions.assumeTrue(redisMod.isEnabled());
		RedisTestContext redismod = new RedisTestContext(redisMod);
		String INDEX = "beerIdx";
		String FIELD_ID = "id";
		String FIELD_ABV = "abv";
		String FIELD_NAME = "name";
		String FIELD_STYLE = "style";
		String FIELD_OUNCES = "ounces";
		redismod.sync().create(INDEX, CreateOptions.<String, String>builder().prefix("beer:").build(),
				Field.tag(FIELD_ID).sortable().build(), Field.text(FIELD_NAME).sortable().build(),
				Field.text(FIELD_STYLE).matcher(PhoneticMatcher.ENGLISH).sortable().build(),
				Field.numeric(FIELD_ABV).sortable().build(), Field.numeric(FIELD_OUNCES).sortable().build());
		execute("import-infer", redismod);
		SearchResults<String, String> results = redismod.sync().search(INDEX, "*");
		Assertions.assertEquals(1000, results.getCount());
		Document<String, String> doc1 = results.get(0);
		Assertions.assertNotNull(doc1.get(FIELD_ABV));
		redismod.close();
	}

}