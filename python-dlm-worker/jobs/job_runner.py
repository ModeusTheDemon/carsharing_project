"""
Job runner with monitoring and error handling integration
"""
import logging
from typing import Dict, Any, Optional
from datetime import datetime, timezone

from policies.rules_engine import RetentionRulesEngine
from utils.job_monitor import JobMonitor, create_job_monitor
from utils.job_status_tracker import JobStatusTracker, JobType, track_job_execution


class JobRunner:
    """Run DLM jobs with monitoring and error handling"""
    
    def __init__(self, config_path: Optional[str] = None):
        self.rules_engine = RetentionRulesEngine(config_path)
        self.error_config = self.rules_engine.get_error_handling_config()
        self.tracker = JobStatusTracker()
        self.logger = logging.getLogger("job_runner")
    
    def run_archiver(self) -> Dict[str, Any]:
        """Run archiver job with monitoring"""
        from .archiver import run_archiver as archiver_func
        
        job_monitor = create_job_monitor("archiver", self.error_config)
        
        try:
            result = track_job_execution(
                self.tracker,
                JobType.ARCHIVER,
                self.rules_engine.get_config_summary(),
                lambda: job_monitor.execute_with_retry(archiver_func)
            )
            
            return {
                "status": "success",
                "result": result,
                "monitor_report": job_monitor.get_job_report(),
                "config_used": self.rules_engine.get_config_summary()
            }
            
        except Exception as e:
            self.logger.error(f"Archiver job failed: {e}")
            return {
                "status": "failed",
                "error": str(e),
                "monitor_report": job_monitor.get_job_report() if 'job_monitor' in locals() else None
            }
    
    def run_anonymizer(self) -> Dict[str, Any]:
        """Run anonymizer job with monitoring"""
        from .anonymizer import run_anonymizer as anonymizer_func
        
        job_monitor = create_job_monitor("anonymizer", self.error_config)
        
        try:
            result = track_job_execution(
                self.tracker,
                JobType.ANONYMIZER,
                self.rules_engine.get_config_summary(),
                lambda: job_monitor.execute_with_retry(anonymizer_func)
            )
            
            return {
                "status": "success",
                "result": result,
                "monitor_report": job_monitor.get_job_report(),
                "config_used": self.rules_engine.get_config_summary()
            }
            
        except Exception as e:
            self.logger.error(f"Anonymizer job failed: {e}")
            return {
                "status": "failed",
                "error": str(e),
                "monitor_report": job_monitor.get_job_report() if 'job_monitor' in locals() else None
            }
    
    def run_cleaner(self) -> Dict[str, Any]:
        """Run cleaner job with monitoring"""
        from .cleaner import run_cleaner as cleaner_func
        
        job_monitor = create_job_monitor("cleaner", self.error_config)
        
        try:
            result = track_job_execution(
                self.tracker,
                JobType.CLEANER,
                self.rules_engine.get_config_summary(),
                lambda: job_monitor.execute_with_retry(cleaner_func)
            )
            
            return {
                "status": "success",
                "result": result,
                "monitor_report": job_monitor.get_job_report(),
                "config_used": self.rules_engine.get_config_summary()
            }
            
        except Exception as e:
            self.logger.error(f"Cleaner job failed: {e}")
            return {
                "status": "failed",
                "error": str(e),
                "monitor_report": job_monitor.get_job_report() if 'job_monitor' in locals() else None
            }
    
    def run_all_jobs(self) -> Dict[str, Any]:
        """Run all DLM jobs in sequence"""
        start_time = datetime.now(timezone.utc)
        
        results = {
            "archiver": self.run_archiver(),
            "anonymizer": self.run_anonymizer(),
            "cleaner": self.run_cleaner(),
            "start_time": start_time.isoformat(),
            "end_time": datetime.now(timezone.utc).isoformat()
        }
        
        # Calculate overall status
        success_count = sum(1 for r in results.values() 
                          if isinstance(r, dict) and r.get("status") == "success")
        total_jobs = 3
        
        results["overall_status"] = {
            "success_count": success_count,
            "total_jobs": total_jobs,
            "success_rate": success_count / total_jobs if total_jobs > 0 else 0
        }
        
        return results
    
    def get_dashboard_data(self) -> Dict[str, Any]:
        """Get monitoring dashboard data"""
        dashboard = self.tracker.generate_dashboard_data()
        
        # Add current configuration
        dashboard["current_configuration"] = {
            "rules": self.rules_engine.get_config_summary(),
            "error_handling": self.error_config,
            "scheduling": self.rules_engine.get_schedule_config()
        }
        
        # Add system status
        dashboard["system_status"] = {
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "config_valid": True,  # Assuming config was validated on init
            "database_connected": self._check_database_connection()
        }
        
        return dashboard
    
    def _check_database_connection(self) -> bool:
        """Check database connection status"""
        try:
            from database.session import get_db
            with get_db() as db:
                db.execute("SELECT 1")
            return True
        except Exception:
            return False
    
    def cleanup_old_tracking_data(self, days_to_keep: int = 30) -> Dict[str, Any]:
        """Cleanup old tracking data"""
        try:
            records_deleted = self.tracker.cleanup_old_records(days_to_keep)
            return {
                "status": "success",
                "records_deleted": records_deleted,
                "days_to_keep": days_to_keep
            }
        except Exception as e:
            return {
                "status": "failed",
                "error": str(e),
                "days_to_keep": days_to_keep
            }


# Command line interface
if __name__ == "__main__":
    import argparse
    import json
    import sys
    
    parser = argparse.ArgumentParser(description="Run DLM jobs with monitoring")
    parser.add_argument("--job", choices=["archiver", "anonymizer", "cleaner", "all"],
                       default="all", help="Job to run")
    parser.add_argument("--config", help="Path to configuration file")
    parser.add_argument("--dashboard", action="store_true", 
                       help="Show dashboard data instead of running jobs")
    parser.add_argument("--cleanup", type=int, metavar="DAYS",
                       help="Cleanup tracking data older than specified days")
    
    args = parser.parse_args()
    
    # Setup logging
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
    )
    
    try:
        runner = JobRunner(args.config)
        
        if args.dashboard:
            # Show dashboard
            dashboard = runner.get_dashboard_data()
            print(json.dumps(dashboard, indent=2, ensure_ascii=False))
            
        elif args.cleanup:
            # Cleanup old data
            result = runner.cleanup_old_tracking_data(args.cleanup)
            print(json.dumps(result, indent=2, ensure_ascii=False))
            
        else:
            # Run jobs
            if args.job == "archiver":
                result = runner.run_archiver()
            elif args.job == "anonymizer":
                result = runner.run_anonymizer()
            elif args.job == "cleaner":
                result = runner.run_cleaner()
            else:  # all
                result = runner.run_all_jobs()
            
            print(json.dumps(result, indent=2, ensure_ascii=False))
            
            # Exit with error code if any job failed
            if isinstance(result, dict) and result.get("status") == "failed":
                sys.exit(1)
            elif isinstance(result, dict) and "overall_status" in result:
                if result["overall_status"]["success_rate"] < 1.0:
                    sys.exit(1)
    
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)