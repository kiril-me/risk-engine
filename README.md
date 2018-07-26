# Build
gradle install

# Run:

Start zookeeper:
 docker run -d --name zookeeper -p 2181:2181 confluent/zookeeper
 
Start kafka:
 docker run -d --name kafka -p 9092:9092 --link zookeeper:zookeeper confluent/kafka
 
Use KAFKA_ADVERTISED_HOST_NAME if you have issues. 
    --env KAFKA_ADVERTISED_HOST_NAME=localhost confluent/kafka

Navigate to build/install/risk-engine/bin
./risk-engine

# Example:
 
Open http://localhost:8080/docs

Navigate to TestRiskEngineService.UserBalance, send payload:
{ "user_id": 100 }

Verify current BTC balance.

 
Navigate to RiskEngineService.WithdrawBalance and send paylod:
{
 "user_id": 100,
 "token": "BTC",
 "requested_amount": 1.0
}

You will get back:
{"status":"SUFFICIENT_BALANCE","orderId":"1"}

To test send TestRiskEngineService.SendSettlement.	
{
 "user_id": 100,
 "order_id": 1,
 "bought_token": "ETH",
 "bought_quantity": 4,
  "sold_token": "BTC",
  "sold_quantity": 0.8
}
