#!/usr/bin/env python3
"""
Test utilities for property-based testing
"""
import sys
import os
from datetime import datetime, timedelta, timezone
from decimal import Decimal

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))



def generate_random_user():
    """Generate a random valid user for testing"""
    import uuid
    return {
        "id": str(uuid.uuid4()),
        "email": f"user{uuid.uuid4().hex[:8]}@example.com",
        "phone": f"+7900{uuid.uuid4().hex[:9]}",
        "first_name": f"FirstName{uuid.uuid4().hex[:4]}",
        "last_name": f"LastName{uuid.uuid4().hex[:4]}",
        "is_deleted": False
    }


def generate_random_vehicle():
    """Generate a random vehicle for testing"""
    import uuid
    return {
        "id": str(uuid.uuid4()),
        "license_plate": f"A{uuid.uuid4().hex[:3]}BC{uuid.uuid4().hex[:2]}",
        "brand": f"Brand{uuid.uuid4().hex[:4]}",
        "model": f"Model{uuid.uuid4().hex[:4]}",
        "status": "available"
    }


def generate_random_ride(user_id, vehicle_id, timestamp=None):
    """Generate a random ride for testing"""
    import uuid
    if timestamp is None:
        timestamp = datetime.now(timezone.utc)
    
    return {
        "id": str(uuid.uuid4()),
        "user_id": user_id,
        "vehicle_id": vehicle_id,
        "start_time": (timestamp - timedelta(hours=1)).isoformat(),
        "end_time": timestamp.isoformat(),
        "start_location": {"lat": 55.752020, "lon": 37.617499},
        "end_location": {"lat": 55.752020, "lon": 37.617499},
        "total_cost": float(Decimal(str(uuid.uuid4().hex[:4])) / 100),
        "status": "completed",
        "created_at": timestamp.isoformat()
    }


def generate_random_payment(ride_id, user_id, timestamp=None):
    """Generate a random payment for testing"""
    import uuid
    if timestamp is None:
        timestamp = datetime.now(timezone.utc)
    
    return {
        "id": str(uuid.uuid4()),
        "ride_id": ride_id,
        "user_id": user_id,
        "amount": float(Decimal(str(uuid.uuid4().hex[:4])) / 10),
        "currency": "RUB",
        "status": "completed",
        "created_at": timestamp.isoformat()
    }


def generate_random_telemetry(ride_id, vehicle_id, timestamp=None):
    """Generate random telemetry data for testing"""
    import uuid
    if timestamp is None:
        timestamp = datetime.now(timezone.utc)
    
    return {
        "id": str(uuid.uuid4()),
        "ride_id": ride_id,
        "vehicle_id": vehicle_id,
        "timestamp": timestamp.isoformat(),
        "data": {
            "speed": float(Decimal(str(uuid.uuid4().hex[:3])) / 10),
            "fuel": float(Decimal(str(uuid.uuid4().hex[:3]))),
            "lat": 55.752020,
            "lon": 37.617499,
            "altitude": 150.0
        }
    }


def generate_deleted_user():
    """Generate a user marked as deleted for anonymization testing"""
    import uuid
    return {
        "id": str(uuid.uuid4()),
        "email": f"deleted{uuid.uuid4().hex[:8]}@example.com",
        "phone": f"+7900{uuid.uuid4().hex[:9]}",
        "first_name": "John",
        "last_name": "Doe",
        "is_deleted": True,
        "deleted_at": (datetime.now(timezone.utc) - timedelta(days=1)).isoformat()
    }


def generate_ride_for_archiving(user_id, vehicle_id):
    """Generate a ride that should be archived (older than 6 months)"""
    import uuid
    old_timestamp = datetime.now(timezone.utc) - timedelta(days=200)
    
    return {
        "id": str(uuid.uuid4()),
        "user_id": user_id,
        "vehicle_id": vehicle_id,
        "start_time": (old_timestamp - timedelta(days=10)).isoformat(),
        "end_time": old_timestamp.isoformat(),
        "start_location": {"lat": 55.752020, "lon": 37.617499},
        "end_location": {"lat": 55.752020, "lon": 37.617499},
        "total_cost": 250.00,
        "status": "completed",
        "created_at": old_timestamp.isoformat()
    }


def generate_telemetry_for_cleanup(ride_id, vehicle_id):
    """Generate telemetry that should be cleaned (older than 30 days)"""
    import uuid
    old_timestamp = datetime.now(timezone.utc) - timedelta(days=35)
    
    return {
        "id": str(uuid.uuid4()),
        "ride_id": ride_id,
        "vehicle_id": vehicle_id,
        "timestamp": old_timestamp.isoformat(),
        "data": {
            "speed": 50.0,
            "fuel": 80.0,
            "lat": 55.752020,
            "lon": 37.617499,
            "altitude": 150.0
        }
    }


def generate_payment_for_preservation():
    """Generate a payment that should be preserved for tax compliance"""
    import uuid
    timestamp = datetime.now(timezone.utc) - timedelta(days=365 * 2)  # 2 years old
    
    return {
        "id": str(uuid.uuid4()),
        "ride_id": str(uuid.uuid4()),
        "user_id": str(uuid.uuid4()),
        "amount": 250.00,
        "currency": "RUB",
        "status": "completed",
        "created_at": timestamp.isoformat()
    }


def generate_old_telemetry():
    """Generate telemetry data older than 30 days"""
    import uuid
    old_timestamp = datetime.now(timezone.utc) - timedelta(days=45)
    
    return {
        "id": str(uuid.uuid4()),
        "ride_id": str(uuid.uuid4()),
        "vehicle_id": str(uuid.uuid4()),
        "timestamp": old_timestamp.isoformat(),
        "data": {
            "speed": 60.0,
            "fuel": 70.0,
            "lat": 55.752020,
            "lon": 37.617499,
            "altitude": 150.0
        }
    }


def is_valid_user(user):
    """Check if a user object is valid"""
    required_fields = ["id", "email", "first_name", "last_name"]
    return all(field in user for field in required_fields)


def is_valid_ride(ride):
    """Check if a ride object is valid"""
    required_fields = ["id", "user_id", "vehicle_id", "start_time", "status"]
    return all(field in ride for field in required_fields)


def is_valid_payment(payment):
    """Check if a payment object is valid"""
    required_fields = ["id", "ride_id", "amount", "currency"]
    return all(field in payment for field in required_fields)


def is_valid_telemetry(telemetry):
    """Check if telemetry data is valid"""
    required_fields = ["id", "ride_id", "timestamp", "data"]
    return all(field in telemetry for field in required_fields)


def validate_archived_data_integrity(original, archived):
    """Validate that archived data maintains integrity"""
    required_fields = ["id", "user_id", "vehicle_id", "start_time", "total_cost"]
    
    for field in required_fields:
        if field in original:
            if field not in archived:
                return False
            if original[field] != archived[field]:
                return False
    
    return True


def validate_anonymized_user(original, anonymized):
    """Validate that user data has been properly anonymized"""
    # PII fields should be removed or masked
    pii_fields = ["email", "phone", "first_name", "last_name"]
    
    for field in pii_fields:
        if field in anonymized:
            # Field should be masked or removed
            value = anonymized[field]
            if isinstance(value, str):
                # Check if value is properly anonymized
                if value != "" and value != "anonymous":
                    return False
    
    # Required non-PII fields should still exist
    if "id" not in anonymized:
        return False
    
    return True
