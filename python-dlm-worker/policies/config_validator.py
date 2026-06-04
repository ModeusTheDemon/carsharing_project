import json
import os
from typing import Dict, Any
import jsonschema
from jsonschema import validate


class ConfigValidator:
    SCHEMA = {
        "type": "object",
        "properties": {
            "telemetry_policy": {
                "type": "object",
                "properties": {
                    "cleanup_after_days": {"type": "number", "minimum": 1, "maximum": 365},
                    "description": {"type": "string"}
                },
                "required": ["cleanup_after_days"],
                "additionalProperties": False
            },
            "rides_policy": {
                "type": "object",
                "properties": {
                    "archive_after_days": {"type": "number", "minimum": 1, "maximum": 730},
                    "description": {"type": "string"}
                },
                "required": ["archive_after_days"],
                "additionalProperties": False
            },
            "users_policy": {
                "type": "object",
                "properties": {
                    "anonymize_immediately": {"type": "boolean"},
                    "description": {"type": "string"}
                },
                "required": ["anonymize_immediately"],
                "additionalProperties": False
            },
            "payments_policy": {
                "type": "object",
                "properties": {
                    "preserve_for_years": {"type": "number", "minimum": 1, "maximum": 10},
                    "description": {"type": "string"}
                },
                "required": ["preserve_for_years"],
                "additionalProperties": False
            },
            "scheduling": {
                "type": "object",
                "properties": {
                    "archiver_cron": {"type": "string", "pattern": "^[0-9*/, -]+$"},
                    "anonymizer_cron": {"type": "string", "pattern": "^[0-9*/, -]+$"},
                    "cleaner_cron": {"type": "string", "pattern": "^[0-9*/, -]+$"},
                    "description": {"type": "string"}
                },
                "required": ["archiver_cron", "anonymizer_cron", "cleaner_cron"],
                "additionalProperties": False
            },
            "error_handling": {
                "type": "object",
                "properties": {
                    "max_retries": {"type": "number", "minimum": 0, "maximum": 10},
                    "retry_delay_seconds": {"type": "number", "minimum": 1, "maximum": 3600},
                    "exponential_backoff": {"type": "boolean"},
                    "description": {"type": "string"}
                },
                "required": ["max_retries", "retry_delay_seconds", "exponential_backoff"],
                "additionalProperties": False
            }
        },
        "required": [
            "telemetry_policy", 
            "rides_policy", 
            "users_policy", 
            "payments_policy",
            "scheduling",
            "error_handling"
        ],
        "additionalProperties": False
    }
    
    @classmethod
    def validate_config_file(cls, config_path: str) -> Dict[str, Any]:
        if not os.path.exists(config_path):
            raise FileNotFoundError(f"Configuration file not found: {config_path}")
        
        with open(config_path, "r", encoding="utf-8") as f:
            config = json.load(f)
        
        return cls.validate_config(config)
    
    @classmethod
    def validate_config(cls, config: Dict[str, Any]) -> Dict[str, Any]:
        try:
            validate(instance=config, schema=cls.SCHEMA)
            
            cls._validate_business_rules(config)
            
            return config
            
        except jsonschema.ValidationError as e:
            raise ValueError(f"Configuration validation failed: {e.message}") from e
    
    @classmethod
    def _validate_business_rules(cls, config: Dict[str, Any]) -> None:
        ride_archive_days = config["rides_policy"]["archive_after_days"]
        payment_preserve_years = config["payments_policy"]["preserve_for_years"]
        payment_preserve_days = payment_preserve_years * 365
        
        if ride_archive_days >= payment_preserve_days:
            raise ValueError(
                f"Ride archiving ({ride_archive_days} days) should happen before "
                f"payment preservation ({payment_preserve_days} days)"
            )
        
        telemetry_cleanup_days = config["telemetry_policy"]["cleanup_after_days"]
        if telemetry_cleanup_days >= ride_archive_days:
            raise ValueError(
                f"Telemetry cleanup ({telemetry_cleanup_days} days) should be more frequent "
                f"than ride archiving ({ride_archive_days} days)"
            )
        
        cron_fields = ["archiver_cron", "anonymizer_cron", "cleaner_cron"]
        for field in cron_fields:
            cron = config["scheduling"][field]
            parts = cron.split()
            if len(parts) != 5:
                raise ValueError(f"Invalid cron expression '{cron}' for {field}. Expected 5 parts.")
    
    @classmethod
    def generate_default_config(cls) -> Dict[str, Any]:
        return {
            "telemetry_policy": {
                "cleanup_after_days": 15,
                "description": "Remove telemetry data older than 15 days from main schema"
            },
            "rides_policy": {
                "archive_after_days": 90,
                "description": "Move ride history older than 3 months (90 days) from main to archive schema"
            },
            "users_policy": {
                "anonymize_immediately": True,
                "description": "Anonymize associated ride data when user account is marked as deleted"
            },
            "payments_policy": {
                "preserve_for_years": 5,
                "description": "Preserve payment data for 5 years in archive schema for tax compliance"
            },
            "scheduling": {
                "archiver_cron": "0 0 * * *",
                "anonymizer_cron": "0 * * * *",
                "cleaner_cron": "0 2 * * 0",
                "description": "Scheduled job execution times: midnight daily, top of any hour, 2 AM Sunday"
            },
            "error_handling": {
                "max_retries": 3,
                "retry_delay_seconds": 60,
                "exponential_backoff": True,
                "description": "Error handling and retry configuration"
            }
        }
    
    @classmethod
    def create_default_config_file(cls, config_path: str) -> None:
        default_config = cls.generate_default_config()
        
        with open(config_path, "w", encoding="utf-8") as f:
            json.dump(default_config, f, indent=2, ensure_ascii=False)
        
        print(f"Created default configuration file: {config_path}")


if __name__ == "__main__":
    import sys
    
    if len(sys.argv) > 1:
        config_file = sys.argv[1]
    else:
        config_file = os.path.join(os.path.dirname(__file__), "retention.json")
    
    try:
        config = ConfigValidator.validate_config_file(config_file)
        print(f"Configuration is valid: {config_file}")
        print(json.dumps(config, indent=2, ensure_ascii=False))
    except Exception as e:
        print(f"Configuration validation failed: {e}")
        sys.exit(1)