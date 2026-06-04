#!/usr/bin/env python3
import sys
import os
import pytest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))



@pytest.mark.integration
class TestDLMWorkerIntegration:
    @pytest.fixture(scope="function")
    def db_session(self):
        from sqlalchemy.orm import Session
        from sqlalchemy import create_engine, text
        from config import settings
        
        engine = create_engine(settings.DATABASE_URL, echo=False)
        with Session(engine) as session:
            yield session
        engine.dispose()

    def test_db_connection(self, db_session):
        from sqlalchemy import text
        result = db_session.execute(text("SELECT 1")).fetchone()
        assert result[0] == 1, "Database connection should work"
        print("Database connection established successfully")

    def test_data_retention_thresholds(self, db_session):
        from policies.rules_engine import RetentionRulesEngine
        
        engine = RetentionRulesEngine()
        
        telemetry_threshold = engine.get_telemetry_cleanup_threshold()
        assert telemetry_threshold is not None
        print(f"Telemetry cleanup threshold: {telemetry_threshold.strftime('%Y-%m-%d')}")
        
        ride_threshold = engine.get_ride_archive_threshold()
        assert ride_threshold is not None
        print(f"Ride archiving threshold: {ride_threshold.strftime('%Y-%m-%d')}")
        
        payment_threshold = engine.get_payment_preservation_threshold()
        assert payment_threshold is not None
        print(f"Payment preservation threshold: {payment_threshold.strftime('%Y-%m-%d')}")

    def test_schedule_job_configuration(self, db_session):
        from policies.rules_engine import RetentionRulesEngine
        
        engine = RetentionRulesEngine()
        schedule = engine.get_schedule_config()
        
        assert schedule["archiver"] == "0 0 * * *", "Archiver should run daily at midnight"
        assert schedule["anonymizer"] == "0 * * * *", "Anonymizer should run hourly"
        assert schedule["cleaner"] == "0 2 * * 0", "Cleaner should run weekly"
        
        print("Scheduled job configuration correct:")
        print(f"   Archiver: {schedule['archiver']} (daily at midnight)")
        print(f"   Anonymizer: {schedule['anonymizer']} (hourly)")
        print(f"   Cleaner: {schedule['cleaner']} (weekly)")

    def test_data_retention_policy_exists(self, db_session):
        import os
        from policies.config_validator import ConfigValidator
        
        config_path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 
                                   "policies", "retention.json")
        assert os.path.exists(config_path), "Retention policy file should exist"
        
        config = ConfigValidator.validate_config_file(config_path)
        assert config is not None, "Configuration should be valid"
        
        print(f"Data retention policy exists at: {config_path}")
        print(f"   Telemetry cleanup: {config['telemetry_policy']['cleanup_after_days']} days")
        print(f"   Ride archiving: {config['rides_policy']['archive_after_days']} days")
        print(f"   Payment preservation: {config['payments_policy']['preserve_for_years']} years")


def run_dlm_integration_tests():
    print("\n" + "=" * 100)
    print("RUNNING DLM WORKER INTEGRATION TESTS (Real PostgreSQL)")
    print("=" * 100)
    
    import subprocess
    import sys
    
    result = subprocess.run([
        sys.executable, "-m", "pytest",
        __file__,
        "-v",
        "-k", "integration",
        "--tb=short"
    ], cwd=os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    
    return result.returncode == 0


if __name__ == "__main__":
    success = run_dlm_integration_tests()
    sys.exit(0 if success else 1)