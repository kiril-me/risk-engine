package com.mercury.app.service;

import java.util.List;
import java.util.Objects;

import com.mercury.app.db.public_.tables.pojos.Balance;
import com.mercury.app.repository.BalanceRepository;
import com.mercury.proto.SettlementRequest;
import com.mercury.proto.SettlementResponce;
import com.mercury.proto.TestRiskEngineServiceGrpc;
import com.mercury.proto.UserBalanceResponce;
import com.mercury.proto.UserRequest;

import io.grpc.stub.StreamObserver;

public class TestRiskEngineService extends TestRiskEngineServiceGrpc.TestRiskEngineServiceImplBase {

	private final SettlementService settlementService;

	private final BalanceRepository balanceRepository;

	public TestRiskEngineService(SettlementService settlementService, BalanceRepository balanceRepository) {
		this.settlementService = Objects.requireNonNull(settlementService);
		this.balanceRepository = Objects.requireNonNull(balanceRepository);
	}

	@Override
	public void sendSettlement(SettlementRequest request, StreamObserver<SettlementResponce> responseObserver) {
		SettlementResponce.Builder responce = SettlementResponce.newBuilder();

		settlementService.sendSettlement(request);

		responseObserver.onNext(responce.build());
		responseObserver.onCompleted();
	}

	@Override
	public void userBalance(UserRequest request, StreamObserver<UserBalanceResponce> responseObserver) {
		UserBalanceResponce.Builder responce = UserBalanceResponce.newBuilder();

		List<Balance> balances = balanceRepository.fetchByUserId(request.getUserId());
		responce.setUserId(request.getUserId());
		if (balances != null) {
			for (Balance balance : balances) {
				com.mercury.proto.Balance.Builder balanceProto = com.mercury.proto.Balance.newBuilder();
				balanceProto.setToken(balance.getToken());
				balanceProto.setAvailable(balance.getAvailable().doubleValue());
				balanceProto.setWithdraw(balance.getWithdraw().doubleValue());
				responce.addBalances(balanceProto);
			}
		}

		responseObserver.onNext(responce.build());
		responseObserver.onCompleted();
	}

}
