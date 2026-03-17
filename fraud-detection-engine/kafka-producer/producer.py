"""
Transaction Event Producer
Simulates 10,000+ events/sec into Kafka topic 'transactions'
Injects realistic fraud patterns at configurable rate
"""

import json
import random
import time
import uuid
import logging
import os
from datetime import datetime, timezone
from kafka import KafkaProducer
from dotenv import load_dotenv

load_dotenv()
logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

KAFKA_BOOTSTRAP = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
TOPIC           = os.getenv("KAFKA_TOPIC", "transactions")
EVENTS_PER_SEC  = int(os.getenv("EVENTS_PER_SEC", "500"))
FRAUD_RATE      = float(os.getenv("FRAUD_RATE", "0.05"))   # 5% fraud

MERCHANTS       = ["Amazon", "Walmart", "Starbucks", "Shell", "Apple Store", "Netflix", "Uber", "AliExpress"]
CATEGORIES      = ["retail", "food", "fuel", "entertainment", "online", "travel", "atm"]
COUNTRIES       = ["US", "GB", "CA", "AU", "DE", "FR", "JP", "CN", "BR", "RU"]
CURRENCIES      = ["USD", "GBP", "CAD", "AUD", "EUR", "JPY", "CNY", "BRL"]


def make_transaction(fraudulent: bool = False) -> dict:
    now = datetime.now(timezone.utc)
    return {
        "transaction_id":   str(uuid.uuid4()),
        "account_id":       f"ACC-{random.randint(1000, 9999)}",
        "amount":           round(random.uniform(5000, 20000), 2) if fraudulent else round(random.uniform(1, 500), 2),
        "merchant":         random.choice(MERCHANTS),
        "merchant_category": random.choice(CATEGORIES[-2:]) if fraudulent else random.choice(CATEGORIES[:5]),
        "country":          random.choice(COUNTRIES[6:]) if fraudulent else random.choice(COUNTRIES[:4]),
        "currency":         random.choice(CURRENCIES[4:]) if fraudulent else random.choice(CURRENCIES[:3]),
        "timestamp":        now.isoformat(),
        "card_present":     False if fraudulent else bool(random.getrandbits(1)),
        "ip_address":       f"{random.randint(1,255)}.{random.randint(0,255)}.{random.randint(0,255)}.{random.randint(0,255)}",
        "device_id":        str(uuid.uuid4()),
        "hour_of_day":      random.randint(0, 5) if fraudulent else random.randint(8, 22),
        "day_of_week":      random.randint(5, 6) if fraudulent else random.randint(0, 4),
    }


def main():
    producer = KafkaProducer(
        bootstrap_servers=KAFKA_BOOTSTRAP,
        value_serializer=lambda v: json.dumps(v).encode("utf-8"),
        key_serializer=lambda k: k.encode("utf-8"),
        acks="all",
        retries=3,
        linger_ms=5,
        batch_size=32_768,
        compression_type="lz4",
    )

    logger.info(f"Producer started → topic={TOPIC} rate={EVENTS_PER_SEC}/s fraud={FRAUD_RATE*100:.0f}%")

    interval   = 1.0 / EVENTS_PER_SEC
    total_sent = 0
    fraud_sent = 0
    start_time = time.time()

    try:
        while True:
            is_fraud = random.random() < FRAUD_RATE
            event    = make_transaction(fraudulent=is_fraud)

            producer.send(
                TOPIC,
                key=event["account_id"],
                value=event,
            )

            total_sent += 1
            if is_fraud:
                fraud_sent += 1

            if total_sent % 1000 == 0:
                elapsed = time.time() - start_time
                logger.info(f"Sent {total_sent} events ({fraud_sent} fraud) | {total_sent/elapsed:.0f} events/sec")

            time.sleep(interval)

    except KeyboardInterrupt:
        logger.info(f"Stopping. Total sent: {total_sent} | Fraud: {fraud_sent}")
    finally:
        producer.flush()
        producer.close()


if __name__ == "__main__":
    main()
