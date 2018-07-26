package com.mercury.app.repository;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.jooq.Configuration;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mercury.app.db.public_.tables.daos.OrderDao;
import com.mercury.app.db.public_.tables.pojos.Order;

public class OrderRepository extends OrderDao {

	private final LoadingCache<Long, Order> orderCache;

	public static final com.mercury.app.db.public_.tables.Order TABLE = com.mercury.app.db.public_.tables.Order.ORDER;

	public OrderRepository(Configuration configuration) {
		super(configuration);

		final CacheLoader<Long, Order> loader = new CacheLoader<Long, Order>() {
			public Order load(Long id) throws Exception {
				return OrderRepository.super.fetchOneById(id);
			}
		};

		orderCache = CacheBuilder.newBuilder().maximumSize(1000) // orders are small critical part to match metch lets
																	// have 1000
				.expireAfterAccess(1, TimeUnit.HOURS) // let order live one hour in the cache
				.build(loader);
	}

	public Order fetchOneById(Long id) {
		try {
			return orderCache.get(id);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void insert(Collection<Order> objects) {
		super.insert(objects);
		for (Order order : objects) {
			orderCache.put(order.getId(), order);
		}
	}

	@Override
	public void update(Collection<Order> objects) {
		super.update(objects);
		for (Order order : objects) {
			orderCache.put(order.getId(), order);
		}
	}

	@Override
	public void delete(Collection<Order> objects) {
		super.delete(objects);
		for (Order order : objects) {
			orderCache.invalidate(order.getId());
		}
	}

}
