import json
import sqlite3
from datetime import datetime, timezone
from typing import Dict, Any, List, Optional
from enum import Enum
from pathlib import Path
import logging


class JobType(Enum):
    ARCHIVER = "archiver"
    ANONYMIZER = "anonymizer"
    CLEANER = "cleaner"


class JobStatusTracker:
    def __init__(self, db_path: Optional[str] = None):
        if db_path is None:
            db_path = "job_status.db"
        
        self.db_path = db_path
        self._memory_mode = db_path == ":memory:"
        
        if self._memory_mode:
            self._conn = sqlite3.connect(db_path)
            self._init_database()
        else:
            self._init_database()
        
        self.logger = logging.getLogger("job_status_tracker")
    
    def _init_database(self) -> None:
        if self._memory_mode:
            conn = self._conn
        else:
            conn = sqlite3.connect(self.db_path)
        
        try:
            conn.execute("""
                CREATE TABLE IF NOT EXISTS job_executions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    job_type TEXT NOT NULL,
                    status TEXT NOT NULL,
                    start_time TIMESTAMP NOT NULL,
                    end_time TIMESTAMP,
                    duration_seconds REAL,
                    records_processed INTEGER,
                    error_message TEXT,
                    config_used TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """)
            
            conn.execute("""
                CREATE TABLE IF NOT EXISTS job_metrics (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    job_type TEXT NOT NULL,
                    metric_name TEXT NOT NULL,
                    metric_value REAL NOT NULL,
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """)
            
            conn.execute("""
                CREATE INDEX IF NOT EXISTS idx_job_type_status 
                ON job_executions(job_type, status)
            """)
            
            conn.execute("""
                CREATE INDEX IF NOT EXISTS idx_job_timestamp 
                ON job_executions(created_at)
            """)
            
            if not self._memory_mode:
                conn.commit()
        finally:
            if not self._memory_mode:
                conn.close()
    
    def _get_connection(self):
        if self._memory_mode:
            return self._conn
        else:
            return sqlite3.connect(self.db_path)
    
    def _execute_with_connection(self, operation, *args, **kwargs):
        if self._memory_mode:
            result = operation(self._conn, *args, **kwargs)
            self._conn.commit()
            return result
        else:
            with sqlite3.connect(self.db_path) as conn:
                result = operation(conn, *args, **kwargs)
                conn.commit()
                return result
    
    def record_job_start(self, job_type: JobType, config: Dict[str, Any]) -> int:
        def _record_start(conn):
            cursor = conn.execute("""
                INSERT INTO job_executions 
                (job_type, status, start_time, config_used)
                VALUES (?, ?, ?, ?)
            """, (
                job_type.value,
                "running",
                datetime.now(timezone.utc).isoformat(),
                json.dumps(config, ensure_ascii=False)
            ))
            return cursor.lastrowid
        
        return self._execute_with_connection(_record_start)
    
    def record_job_success(
        self, 
        execution_id: int, 
        records_processed: int = 0,
        metrics: Optional[Dict[str, float]] = None
    ) -> None:
        def _record_success(conn):
            end_time = datetime.now(timezone.utc)
            
            cursor = conn.execute(
                "SELECT start_time FROM job_executions WHERE id = ?",
                (execution_id,)
            )
            start_time_str = cursor.fetchone()[0]
            start_time = datetime.fromisoformat(start_time_str)
            duration = (end_time - start_time).total_seconds()
            
            conn.execute("""
                UPDATE job_executions 
                SET status = ?,
                    end_time = ?,
                    duration_seconds = ?,
                    records_processed = ?
                WHERE id = ?
            """, (
                "success",
                end_time.isoformat(),
                duration,
                records_processed,
                execution_id
            ))
            
            if metrics:
                for metric_name, metric_value in metrics.items():
                    conn.execute("""
                        INSERT INTO job_metrics (job_type, metric_name, metric_value)
                        SELECT job_type, ?, ?
                        FROM job_executions 
                        WHERE id = ?
                    """, (metric_name, metric_value, execution_id))
        
        self._execute_with_connection(_record_success)
    
    def record_job_failure(
        self, 
        execution_id: int, 
        error_message: str,
        records_processed: int = 0
    ) -> None:
        def _record_failure(conn):
            end_time = datetime.now(timezone.utc)
            
            cursor = conn.execute(
                "SELECT start_time FROM job_executions WHERE id = ?",
                (execution_id,)
            )
            start_time_str = cursor.fetchone()[0]
            start_time = datetime.fromisoformat(start_time_str)
            duration = (end_time - start_time).total_seconds()
            
            conn.execute("""
                UPDATE job_executions 
                SET status = ?,
                    end_time = ?,
                    duration_seconds = ?,
                    records_processed = ?,
                    error_message = ?
                WHERE id = ?
            """, (
                "failed",
                end_time.isoformat(),
                duration,
                records_processed,
                error_message[:500],
                execution_id
            ))
        
        self._execute_with_connection(_record_failure)
    
    def get_job_stats(self, job_type: Optional[JobType] = None) -> Dict[str, Any]:
        def _get_stats(conn):
            where_clause = "WHERE job_type = ?" if job_type else ""
            params = (job_type.value,) if job_type else ()
            
            cursor = conn.execute(f"""
                SELECT 
                    COUNT(*) as total_executions,
                    SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) as success_count,
                    SUM(CASE WHEN status = 'failed' THEN 1 ELSE 0 END) as failure_count,
                    SUM(CASE WHEN status = 'running' THEN 1 ELSE 0 END) as running_count,
                    AVG(duration_seconds) as avg_duration,
                    AVG(records_processed) as avg_records_processed
                FROM job_executions
                {where_clause}
            """, params)
            
            stats = dict(zip(
                ["total", "success", "failure", "running", "avg_duration", "avg_records"],
                cursor.fetchone()
            ))
            
            cursor = conn.execute(f"""
                SELECT 
                    id, job_type, status, start_time, end_time,
                    duration_seconds, records_processed, error_message
                FROM job_executions
                {where_clause}
                ORDER BY created_at DESC
                LIMIT 10
            """, params)
            
            columns = [desc[0] for desc in cursor.description]
            recent_executions = [
                dict(zip(columns, row))
                for row in cursor.fetchall()
            ]
            
            cursor = conn.execute(f"""
                SELECT 
                    strftime('%Y-%m-%d %H:00:00', start_time) as hour,
                    COUNT(*) as total,
                    SUM(CASE WHEN status = 'failed' THEN 1 ELSE 0 END) as failures
                FROM job_executions
                {where_clause}
                GROUP BY hour
                ORDER BY hour DESC
                LIMIT 24
            """, params)
            
            hourly_stats = [
                {
                    "hour": row[0],
                    "total": row[1],
                    "failures": row[2],
                    "failure_rate": row[2] / row[1] if row[1] > 0 else 0
                }
                for row in cursor.fetchall()
            ]
            
            return {
                "statistics": stats,
                "recent_executions": recent_executions,
                "hourly_trends": hourly_stats,
                "job_type": job_type.value if job_type else "all"
            }
        
        return self._execute_with_connection(_get_stats)
    
    def generate_dashboard_data(self) -> Dict[str, Any]:
        dashboard_data = {
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "overall_stats": self.get_job_stats(),
            "by_job_type": {}
        }
        
        for job_type in JobType:
            dashboard_data["by_job_type"][job_type.value] = self.get_job_stats(job_type)
        
        return dashboard_data
    
    def cleanup_old_records(self, days_to_keep: int = 30) -> int:
        def _cleanup_records(conn):
            cutoff_date = datetime.now(timezone.utc).replace(
                tzinfo=None
            ).timestamp() - (days_to_keep * 24 * 60 * 60)
            
            cursor = conn.execute("""
                DELETE FROM job_executions 
                WHERE strftime('%s', created_at) < ?
            """, (cutoff_date,))
            
            executions_deleted = cursor.rowcount
            
            cursor = conn.execute("""
                DELETE FROM job_metrics 
                WHERE strftime('%s', timestamp) < ?
            """, (cutoff_date,))
            
            metrics_deleted = cursor.rowcount
            
            self.logger.info(
                f"Cleaned up {executions_deleted} job executions "
                f"and {metrics_deleted} metrics older than {days_to_keep} days"
            )
            
            return executions_deleted + metrics_deleted
        
        return self._execute_with_connection(_cleanup_records)


def track_job_execution(
    tracker: JobStatusTracker,
    job_type: JobType,
    config: Dict[str, Any],
    job_func,
    *args,
    **kwargs
) -> Any:
    execution_id = tracker.record_job_start(job_type, config)
    
    try:
        result = job_func(*args, **kwargs)
        
        records_processed = 0
        if isinstance(result, dict) and "records_processed" in result:
            records_processed = result["records_processed"]
        elif isinstance(result, (list, tuple)):
            records_processed = len(result)
        
        tracker.record_job_success(execution_id, records_processed)
        return result
        
    except Exception as e:
        tracker.record_job_failure(execution_id, str(e))
        raise


if __name__ == "__main__":
    import logging
    logging.basicConfig(level=logging.INFO)
    
    tracker = JobStatusTracker(":memory:")
    
    for i in range(5):
        job_type = list(JobType)[i % len(JobType)]
        config = {"max_retries": 3, "test_run": True}
        
        execution_id = tracker.record_job_start(job_type, config)
        
        if i % 3 == 0:
            tracker.record_job_failure(
                execution_id, 
                f"Simulated failure in test run {i}",
                records_processed=i * 10
            )
        else:
            tracker.record_job_success(
                execution_id,
                records_processed=i * 100,
                metrics={"processing_rate": i * 50.5}
            )
    
    dashboard = tracker.generate_dashboard_data()
    print("Dashboard data:")
    print(json.dumps(dashboard, indent=2, ensure_ascii=False))
    
    archiver_stats = tracker.get_job_stats(JobType.ARCHIVER)
    print("\nArchiver stats:")
    print(json.dumps(archiver_stats, indent=2, ensure_ascii=False))