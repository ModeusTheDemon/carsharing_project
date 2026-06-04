#!/usr/bin/env python3
import sys
import os
from datetime import datetime, timedelta, timezone

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))



def test_rules_engine_basic():
    print("\n" + "=" * 80)
    print("Testing Rules Engine - Basic Configuration")
    print("=" * 80)

    from policies.rules_engine import RetentionRulesEngine

    engine = RetentionRulesEngine()

    telemetry_threshold = engine.get_telemetry_cleanup_threshold()
    ride_threshold = engine.get_ride_archive_threshold()
    payment_threshold = engine.get_payment_preservation_threshold()
    
    assert telemetry_threshold is not None, "Telemetry threshold should be set"
    assert ride_threshold is not None, "Ride threshold should be set"
    assert payment_threshold is not None, "Payment threshold should be set"

    print(f"Thresholds: telemetry={telemetry_threshold.strftime('%Y-%m-%d')}, ride={ride_threshold.strftime('%Y-%m-%d')}, payment={payment_threshold.strftime('%Y-%m-%d')}")

    schedule = engine.get_schedule_config()

    assert "archiver" in schedule, "Archiver cron should be defined"
    assert "anonymizer" in schedule, "Anonymizer cron should be defined"
    assert "cleaner" in schedule, "Cleaner cron should be defined"

    print(f"Schedule: archiver={schedule['archiver']}, anonymizer={schedule['anonymizer']}, cleaner={schedule['cleaner']}")

    print("\n" + "=" * 80)
    print("Rules engine tests passed!")
    print("=" * 80)


def test_config_validator():
    print("\n" + "=" * 80)
    print("Testing Config Validator")
    print("=" * 80)

    from policies.config_validator import ConfigValidator

    default_config = ConfigValidator.generate_default_config()
    
    try:
        ConfigValidator.validate_config(default_config)
        print("Default configuration is valid")
    except ValueError as e:
        print(f"Default configuration validation failed: {e}")
        return False

    invalid_config = ConfigValidator.generate_default_config()
    invalid_config["rides_policy"]["archive_after_days"] = 2000

    try:
        ConfigValidator.validate_config(invalid_config)
        print("Should have failed validation for rides > 5 years")
        return False
    except ValueError:
        print("Business rule validation correctly rejects rides > 5 years")

    invalid_config2 = ConfigValidator.generate_default_config()
    invalid_config2["telemetry_policy"]["cleanup_after_days"] = 200

    try:
        ConfigValidator.validate_config(invalid_config2)
        print("Should have failed validation for telemetry > ride archiving")
        return False
    except ValueError:
        print("Business rule validation correctly rejects telemetry > ride archiving")

    print("\n" + "=" * 80)
    print("Config validator tests passed!")
    print("=" * 80)

    return True


def test_ride_threshold_logic():
    print("\n" + "=" * 80)
    print("Testing Ride Archiving Threshold Logic")
    print("=" * 80)

    from policies.rules_engine import RetentionRulesEngine

    engine = RetentionRulesEngine()
    threshold = engine.get_ride_archive_threshold()

    print("\nTest 1: Ride older than threshold should be archived")
    old_ride = {
        "created_at": (datetime.now(timezone.utc) - timedelta(days=200)).isoformat(),
        "status": "completed"
    }

    ride_time = datetime.fromisoformat(old_ride["created_at"].replace("Z", "+00:00"))
    is_old = ride_time < threshold
    assert is_old, "Old ride should be archived"
    print(f"Ride from {old_ride['created_at'][:10]} is older than threshold ({threshold.strftime('%Y-%m-%d')})")
    
    print("\nTest 2: Recent ride should not be archived")
    recent_ride = {
        "created_at": (datetime.now(timezone.utc) - timedelta(days=30)).isoformat(),
        "status": "completed"
    }

    recent_time = datetime.fromisoformat(recent_ride["created_at"].replace("Z", "+00:00"))
    is_recent = recent_time >= threshold
    assert is_recent, "Recent ride should not be archived"
    print(f"Ride from {recent_ride['created_at'][:10]} is recent (within threshold)")

    print("\n" + "=" * 80)
    print("Ride archiving threshold tests passed!")
    print("=" * 80)


def test_telemetry_cleanup_logic():
    print("\n" + "=" * 80)
    print("Testing Telemetry Cleanup Threshold Logic")
    print("=" * 80)

    from policies.rules_engine import RetentionRulesEngine

    engine = RetentionRulesEngine()
    threshold = engine.get_telemetry_cleanup_threshold()

    print("\nTest 1: Telemetry older than threshold should be cleaned")
    old_telemetry = {
        "timestamp": (datetime.now(timezone.utc) - timedelta(days=45)).isoformat()
    }

    telemetry_time = datetime.fromisoformat(old_telemetry["timestamp"].replace("Z", "+00:00"))
    is_old = telemetry_time < threshold
    assert is_old, "Old telemetry should be cleaned"
    print(f"Telemetry from {old_telemetry['timestamp'][:10]} is older than threshold ({threshold.strftime('%Y-%m-%d')})")

    print("\nTest 2: Recent telemetry should not be cleaned")
    recent_telemetry = {
        "timestamp": (datetime.now(timezone.utc) - timedelta(days=10)).isoformat()
    }

    recent_time = datetime.fromisoformat(recent_telemetry["timestamp"].replace("Z", "+00:00"))
    is_recent = recent_time >= threshold
    assert is_recent, "Recent telemetry should not be cleaned"
    print(f"Telemetry from {recent_telemetry['timestamp'][:10]} is recent (within threshold)")

    print("\n" + "=" * 80)
    print("Telemetry cleanup threshold tests passed!")
    print("=" * 80)


def test_archiver_function_basic():
    print("\n" + "=" * 80)
    print("Testing Archiver Function - Basic Execution")
    print("=" * 80)
    
    print("\nTest 1: Import archiver module")
    from jobs.archiver import run_archiver, uuid_to_bigint
    print("Archiver module imported successfully")
    
    print("\nTest 2: Test UUID to bigint conversion")
    test_uuid = "12345678-1234-1234-1234-123456789012"
    bigint_val = uuid_to_bigint(test_uuid)
    assert isinstance(bigint_val, int), "Should return an integer"
    assert bigint_val > 0, "Should return positive integer"
    print(f"UUID {test_uuid} -> bigint {bigint_val}")
    
    print("\n" + "=" * 80)
    print("Archiver function tests passed!")
    print("=" * 80)


def test_anonymizer_function_basic():
    print("\n" + "=" * 80)
    print("Testing Anonymizer Function - Basic Execution")
    print("=" * 80)
    
    print("\nTest 1: Import anonymizer module")
    from jobs.anonymizer import run_anonymizer
    print("Anonymizer module imported successfully")
    
    print("\n" + "=" * 80)
    print("Anonymizer function tests passed!")
    print("=" * 80)


def test_cleaner_function_basic():
    print("\n" + "=" * 80)
    print("Testing Cleaner Function - Basic Execution")
    print("=" * 80)
    
    print("\nTest 1: Import cleaner module")
    from jobs.cleaner import run_cleaner
    print("Cleaner module imported successfully")
    
    print("\n" + "=" * 80)
    print("Cleaner function tests passed!")
    print("=" * 80)


def run_all_basic_tests():
    print("\n" + "=" * 100)
    print("RUNNING BASIC JOB UNIT TESTS")
    print("=" * 100)

    results = []

    try:
        test_rules_engine_basic()
        results.append(("Rules engine basic tests", True))
    except Exception as e:
        print(f"Rules engine tests failed: {e}")
        import traceback
        traceback.print_exc()
        results.append(("Rules engine basic tests", False))

    try:
        test_config_validator()
        results.append(("Config validator tests", True))
    except Exception as e:
        print(f"Config validator tests failed: {e}")
        import traceback
        traceback.print_exc()
        results.append(("Config validator tests", False))

    try:
        test_ride_threshold_logic()
        results.append(("Ride archiving threshold tests", True))
    except Exception as e:
        print(f"Ride archiving threshold tests failed: {e}")
        import traceback
        traceback.print_exc()
        results.append(("Ride archiving threshold tests", False))

    try:
        test_telemetry_cleanup_logic()
        results.append(("Telemetry cleanup threshold tests", True))
    except Exception as e:
        print(f"Telemetry cleanup threshold tests failed: {e}")
        import traceback
        traceback.print_exc()
        results.append(("Telemetry cleanup threshold tests", False))

    try:
        test_archiver_function_basic()
        results.append(("Archiver function tests", True))
    except Exception as e:
        print(f"Archiver function tests failed: {e}")
        import traceback
        traceback.print_exc()
        results.append(("Archiver function tests", False))

    try:
        test_anonymizer_function_basic()
        results.append(("Anonymizer function tests", True))
    except Exception as e:
        print(f"Anonymizer function tests failed: {e}")
        import traceback
        traceback.print_exc()
        results.append(("Anonymizer function tests", False))

    try:
        test_cleaner_function_basic()
        results.append(("Cleaner function tests", True))
    except Exception as e:
        print(f"Cleaner function tests failed: {e}")
        import traceback
        traceback.print_exc()
        results.append(("Cleaner function tests", False))

    print("\n" + "=" * 100)
    print("TEST SUMMARY")
    print("=" * 100)

    passed = sum(1 for _, result in results if result)
    failed = sum(1 for _, result in results if not result)

    for test_name, result in results:
        status = "PASSED" if result else "FAILED"
        print(f"{status:15} {test_name}")

    print(f"\nTotal: {passed} passed, {failed} failed")

    if failed > 0:
        print("\nSome tests failed. Review the errors above.")
        return False

    print("\nAll basic tests passed!")
    return True


if __name__ == "__main__":
    success = run_all_basic_tests()
    sys.exit(0 if success else 1)