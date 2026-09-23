#!/usr/bin/env bash
set -e
KAFKA_CONTAINER=pharmacy-kafka
BOOTSTRAP=localhost:9092

echo "=== Kafka Broker status ==="
docker ps --filter name=$KAFKA_CONTAINER --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
echo "=== Danh sách Topic (Kafka Tool / CLI) ==="
docker exec $KAFKA_CONTAINER /opt/kafka/bin/kafka-topics.sh --bootstrap-server $BOOTSTRAP --list

echo ""
echo "=== Chi tiết Partition từng Topic ==="
docker exec $KAFKA_CONTAINER /opt/kafka/bin/kafka-topics.sh --bootstrap-server $BOOTSTRAP --describe

echo ""
echo "=== Kiểm tra từng topic theo yêu cầu ==="
echo "--- medicine-stock-events (yêu cầu 3 partitions) ---"
docker exec $KAFKA_CONTAINER /opt/kafka/bin/kafka-topics.sh --bootstrap-server $BOOTSTRAP --describe --topic medicine-stock-events
echo "--- medicine-price-updates (yêu cầu 1 partition - đảm bảo thứ tự) ---"
docker exec $KAFKA_CONTAINER /opt/kafka/bin/kafka-topics.sh --bootstrap-server $BOOTSTRAP --describe --topic medicine-price-updates
echo "--- pharmacy-notifications (yêu cầu 2 partitions) ---"
docker exec $KAFKA_CONTAINER /opt/kafka/bin/kafka-topics.sh --bootstrap-server $BOOTSTRAP --describe --topic pharmacy-notifications

echo ""
echo "=== Kiểm tra KRaft mode (không dùng Zookeeper) ==="
docker exec $KAFKA_CONTAINER env | grep -E "KAFKA_PROCESS_ROLES|KAFKA_NODE_ID|CLUSTER_ID" || true
docker inspect $KAFKA_CONTAINER --format '{{json .Config.Env}}' | python3 -m json.tool | grep -E "KAFKA|CLUSTER" || true
