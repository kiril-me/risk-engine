package com.mercury.app.repository;

import static org.jooq.impl.DSL.using;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.jooq.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mercury.app.data.UserIdToken;
import com.mercury.app.db.public_.tables.daos.BalanceDao;
import com.mercury.app.db.public_.tables.pojos.Balance;
import com.mercury.app.db.public_.tables.records.BalanceRecord;

public class BalanceRepository extends BalanceDao {

	private static final Logger LOG = LoggerFactory.getLogger(BalanceRepository.class);

	public static final com.mercury.app.db.public_.tables.Balance TABLE = com.mercury.app.db.public_.tables.Balance.BALANCE;

	private final LoadingCache<UserIdToken, Balance> balanceCache;

	public BalanceRepository(Configuration configuration) {
		super(configuration);

		final CacheLoader<UserIdToken, Balance> loader = new CacheLoader<UserIdToken, Balance>() {
			public Balance load(UserIdToken key) throws Exception {
				return fetchBalance(key.getUserId(), key.getToken());
			}
		};
		balanceCache = CacheBuilder.newBuilder().maximumSize(1500) // 300 * 5 - Because we store balances, not users
				.expireAfterAccess(30, TimeUnit.MINUTES) // we might not need it anymore after 30 mins
				.build(loader);
	}

	private Balance fetchBalance(long userId, String token) {
		BalanceRecord record = using(configuration()).selectFrom(TABLE)
				.where(TABLE.USER_ID.eq(userId).and(TABLE.TOKEN.eq(token))).fetchOne();
		return record == null ? null : mapper().map(record);
	}

	public Balance getBalanceByUserIdToken(Long userId, String token) {
		try {
			return balanceCache.get(UserIdToken.of(userId, token));
		} catch (ExecutionException e) {
			LOG.error("Error get balance from cache {} {}", userId, token, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void update(Collection<Balance> objects) {
		super.update(objects);
		for (Balance balance : objects) {
			balanceCache.put(UserIdToken.of(balance.getUserId(), balance.getToken()), balance);
		}
	}

	@Override
	public void insert(Collection<Balance> objects) {
		super.insert(objects);
		for (Balance balance : objects) {
			balanceCache.put(UserIdToken.of(balance.getUserId(), balance.getToken()), balance);
		}
	}

	@Override
	public void delete(Collection<Balance> objects) {
		super.delete(objects);
		for (Balance balance : objects) {
			balanceCache.invalidate(UserIdToken.of(balance.getUserId(), balance.getToken()));
		}
	}

}
