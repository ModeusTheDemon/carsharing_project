#!/usr/bin/env python3
import sys
import os
import pytest
from datetime import datetime, timedelta, timezone
from decimal import Decimal

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))



def pytest_addoption(parser):
    parser.addoption(
        "--run-slow",
        action="store_true",
        default=False,
        help="Run slow tests"
    )
    parser.addoption(
        "--database-url",
        default=os.environ.get("DATABASE_URL", "postgresql://test_user:test_pass@localhost:5432/carsharing_test"),
        help="Database URL for integration tests"
    )


def pytest_collection_modifyitems(config, items):
    if not config.getoption("--run-slow"):
        skip_slow = pytest.mark.skip(reason="need --run-slow to run")
        for item in items:
            if "slow" in item.keywords:
                item.add_marker(skip_slow)


@pytest.fixture(scope="function")
def test_user_id():
    import uuid
    return str(uuid.uuid4())


@pytest.fixture(scope="function")
def test_vehicle_id():
    import uuid
    return str(uuid.uuid4())


@pytest.fixture(scope="function")
def test_ride_id():
    import uuid
    return str(uuid.uuid4())


@pytest.fixture(scope="function")
def current_timestamp():
    return datetime.now(timezone.utc)


@pytest.fixture(scope="function")
def past_timestamp():
    return datetime.now(timezone.utc) - timedelta(days=45)


@pytest.fixture(scope="function")
def old_timestamp():
    return datetime.now(timezone.utc) - timedelta(days=200)


@pytest.fixture(scope="function")
def recent_ride_data(test_user_id, test_vehicle_id, current_timestamp):
    return {
        "id": str(__import__('uuid').uuid4()),
        "user_id": test_user_id,
        "vehicle_id": test_vehicle_id,
        "start_time": (current_timestamp - timedelta(hours=1)).isoformat(),
        "end_time": current_timestamp.isoformat(),
        "start_location": {"lat": 55.752020, "lon": 37.617499},
        "end_location": {"lat": 55.752020, "lon": 37.617499},
        "total_cost": 150.00,
        "status": "completed",
        "created_at": current_timestamp.isoformat()
    }


@pytest.fixture(scope="function")
def archived_ride_data(test_user_id, test_vehicle_id, old_timestamp):
    return {
        "id": str(__import__('uuid').uuid4()),
        "user_id": test_user_id,
        "vehicle_id": test_vehicle_id,
        "start_time": (old_timestamp - timedelta(days=10)).isoformat(),
        "end_time": old_timestamp.isoformat(),
        "start_location": {"lat": 55.752020, "lon": 37.617499},
        "end_location": {"lat": 55.752020, "lon": 37.617499},
        "total_cost": 250.00,
        "status": "completed",
        "created_at": old_timestamp.isoformat()
    }


@pytest.fixture(scope="function")
def telemetry_data(test_ride_id, test_vehicle_id, current_timestamp):
    return {
        "id": str(__import__('uuid').uuid4()),
        "ride_id": test_ride_id,
        "vehicle_id": test_vehicle_id,
        "timestamp": current_timestamp.isoformat(),
        "data": {
            "speed": 60.5,
            "fuel": 80.0,
            "lat": 55.752020,
            "lon": 37.617499,
            "altitude": 150.0
        }
    }


@pytest.fixture(scope="function")
def deleted_user_data(test_user_id):
    return {
        "id": test_user_id,
        "email": "deleted@example.com",
        "phone": "+79001234567",
        "first_name": "John",
        "last_name": "Doe",
        "is_deleted": True,
        "deleted_at": (datetime.now(timezone.utc) - timedelta(days=1)).isoformat()
    }