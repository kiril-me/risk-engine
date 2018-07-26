package com.mercury.app.service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mercury.app.data.OrderStatus;
import com.mercury.app.db.public_.Sequences;
import com.mercury.app.db.public_.tables.pojos.Balance;
import com.mercury.app.db.public_.tables.pojos.Order;
import com.mercury.app.repository.BalanceRepository;
import com.mercury.app.repository.OrderRepository;
import com.mercury.proto.RiskEngineServiceGrpc;
import com.mercury.proto.SettlementRequest;
import com.mercury.proto.WithdrawBalanceRequest;
import com.mercury.proto.WithdrawBalanceResponce;
import com.mercury.proto.WithdrawBalanceResponce.Status;

import io.grpc.stub.StreamObserver;

public class RiskEngineService extends RiskEngineServiceGrpc.RiskEngineServiceImplBase {

	private static final Logger LOG = LoggerFactory.getLogger(RiskEngineService.class);

	private final BalanceRepository balanceRepository;

	private final OrderRepository orderRepository;

	private final ConcurrentHashMap<Long, Lock> userIdLockMap = new ConcurrentHashMap<>();

	public RiskEngineService(BalanceRepository balanceRepository, OrderRepository orderRepository) {
		this.balanceRepository = Objects.requireNonNull(balanceRepository);
		this.orderRepository = Objects.requireNonNull(orderRepository);
	}

	@Override
	public void withdrawBalance(WithdrawBalanceRequest request,
			StreamObserver<WithdrawBalanceResponce> responseObserver) {
		WithdrawBalanceResponce.Builder response = WithdrawBalanceResponce.newBuilder();

		final BigDecimal amount = BigDecimal.valueOf(request.getRequestedAmount());

		final Long orderId = DSL.using(balanceRepository.configuration()).transactionResult(() -> {
			final Long userId = request.getUserId();
			final Lock lock = getUserLock(userId);
			try {
				return withdraw(userId, request.getToken(), amount);
			} finally {
				unlockUser(userId, lock);
			}
		});

		if (orderId == null) {
			LOG.info("Insufficent balance {}", request);
			response.setStatus(Status.INSUFFICIENT_BALANCE);
			response.setOrderId(-1);
		} else {
			response.setStatus(Status.SUFFICIENT_BALANCE);
			response.setOrderId(orderId);

			LOG.info("Order created {} {}", orderId, request);
		}

		responseObserver.onNext(response.build());
		responseObserver.onCompleted();
	}
	
	private void unlockUser(Long userId, Lock lock) {
		userIdLockMap.remove(userId);
		lock.unlock();
	}

	private Lock getUserLock(Long userId) {
		final Lock lock = userIdLockMap.computeIfAbsent(userId, (userId_) -> {
			return new ReentrantLock();
		});
		lock.lock();
		return lock;
	}

	public synchronized Long withdraw(final Long userId, final String token, final BigDecimal amount) {
		final Balance balance = balanceRepository.getBalanceByUserIdToken(userId, token);
		if (balance == null || balance.getAvailable().compareTo(amount) <= 0) {
			return null;
		}
		final BigDecimal updatedAvailable = balance.getAvailable().subtract(amount);
		final BigDecimal updatedWithdraw = balance.getWithdraw().add(amount);
		Balance updateBalance = new Balance(balance.getId(), balance.getToken(), balance.getUserId(), updatedAvailable,
				updatedWithdraw);
		balanceRepository.update(updateBalance);

		final Long orderId = DSL.using(orderRepository.configuration()).nextval(Sequences.S_ORDER_ID);
		final Order order = new Order(orderId, userId, token, OrderStatus.OPEN, amount);
		orderRepository.insert(order);
		return orderId;
	}

	public void processSettlement(final Long orderId, final SettlementRequest settlement) {
		DSL.using(balanceRepository.configuration()).transaction(() -> {
			final Long userId = settlement.getUserId();
			final Lock lock = getUserLock(userId);
			try {
				processSettlementInner(orderId, settlement);
			} finally {
				unlockUser(userId, lock);
			}
		});
	}

	private void processSettlementInner(final Long orderId, final SettlementRequest settlement) {
		final Order order = orderRepository.fetchOneById(orderId);
		if (order == null) {
			LOG.info("Could not find order {}", orderId);
		} else if (order.getUserId().equals(settlement.getUserId())
				|| order.getToken().equals(settlement.getSoldToken())) {
			LOG.info("Wrong user or token");
		} else if(OrderStatus.OPEN.equals(order.getStatus())) {
			LOG.warn("Could not match order {} with settlement {}", orderId, settlement);
			BigDecimal soldQuantity = BigDecimal.valueOf(settlement.getSoldQuantity());
			BigDecimal remain = order.getAmount().subtract(soldQuantity);

			if (remain.compareTo(BigDecimal.ZERO) == -1) {
				LOG.warn("Settlement is bigger whan order");
				// should we process this settlement?
				return;
			}

			final Balance soldBalance = balanceRepository.getBalanceByUserIdToken(settlement.getUserId(),
					settlement.getSoldToken());
			final BigDecimal soldAvailable = soldBalance.getAvailable().add(remain);
			final BigDecimal soldWithdraw = soldBalance.getWithdraw().subtract(order.getAmount());
			final Balance updateSoldBalance = new Balance(soldBalance.getId(), soldBalance.getToken(),
					soldBalance.getUserId(), soldAvailable, soldWithdraw);
			balanceRepository.update(updateSoldBalance);

			final Balance boughtBalance = balanceRepository.getBalanceByUserIdToken(settlement.getUserId(),
					settlement.getBoughtToken());
			BigDecimal boughtQuantity = BigDecimal.valueOf(settlement.getBoughtQuantity());
			final BigDecimal boughtAvailable = boughtBalance.getAvailable().add(boughtQuantity);
			final Balance updateBoughtBalance = new Balance(boughtBalance.getId(), boughtBalance.getToken(),
					boughtBalance.getUserId(), boughtAvailable, boughtBalance.getWithdraw());
			balanceRepository.update(updateBoughtBalance);

			final Order updateOrder = new Order(order.getId(), order.getUserId(), order.getToken(), OrderStatus.CLOSED,
					order.getAmount());
			orderRepository.update(updateOrder);
		}
	}
}
