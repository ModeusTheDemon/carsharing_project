import logging
import time
from datetime import datetime, timezone
from typing import Dict, Any, Optional, Callable
import json
from enum import Enum


class JobStatus(Enum):
    PENDING = "pending"
    RUNNING = "running"
    SUCCESS = "success"
    FAILED = "failed"
    RETRYING = "retrying"


class JobMonitor:
    def __init__(self, job_name: str, config: Dict[str, Any]):
        self.job_name = job_name
        self.config = config
        self.logger = logging.getLogger(f"job_monitor.{job_name}")
        self.start_time: Optional[datetime] = None
        self.end_time: Optional[datetime] = None
        self.status: JobStatus = JobStatus.PENDING
        self.retry_count = 0
        self.error_details: Optional[Dict[str, Any]] = None
        
        self._setup_logging()
    
    def _setup_logging(self) -> None:
        job_name = self.job_name
        
        class StructuredFormatter(logging.Formatter):
            def __init__(self, job_name):
                super().__init__()
                self.job_name = job_name
            
            def format(self, record):
                log_record = {
                    "timestamp": datetime.now(timezone.utc).isoformat(),
                    "level": record.levelname,
                    "job": self.job_name,
                    "message": record.getMessage(),
                    "module": record.module,
                    "function": record.funcName,
                    "line": record.lineno
                }
                
                if hasattr(record, 'extra'):
                    log_record.update(record.extra)
                
                return json.dumps(log_record, ensure_ascii=False)
        
        handler = logging.StreamHandler()
        handler.setFormatter(StructuredFormatter(job_name))
        self.logger.addHandler(handler)
        self.logger.setLevel(logging.INFO)
    
    def _log_with_context(self, level: str, message: str, **extra) -> None:
        log_method = getattr(self.logger, level.lower())
        record = logging.LogRecord(
            name=self.logger.name,
            level=getattr(logging, level.upper()),
            pathname=__file__,
            lineno=0,
            msg=message,
            args=(),
            exc_info=None
        )
        record.extra = {
            "job_status": self.status.value,
            "retry_count": self.retry_count,
            "job_duration": self._get_duration(),
            **extra
        }
        self.logger.handle(record)
    
    def _get_duration(self) -> Optional[float]:
        if self.start_time and self.end_time:
            return (self.end_time - self.start_time).total_seconds()
        elif self.start_time:
            return (datetime.now(timezone.utc) - self.start_time).total_seconds()
        return None
    
    def execute_with_retry(self, job_func: Callable, *args, **kwargs) -> Any:
        max_retries = self.config.get("max_retries", 3)
        retry_delay = self.config.get("retry_delay_seconds", 60)
        exponential_backoff = self.config.get("exponential_backoff", True)
        
        last_exception = None
        
        while self.retry_count <= max_retries:
            try:
                if self.retry_count > 0:
                    self.status = JobStatus.RETRYING
                    self._log_with_context(
                        "INFO",
                        f"Retrying job (attempt {self.retry_count}/{max_retries})",
                        retry_delay=retry_delay
                    )
                
                return self._execute_job(job_func, *args, **kwargs)
                
            except Exception as e:
                last_exception = e
                self.retry_count += 1
                
                if self.retry_count <= max_retries:
                    delay = retry_delay
                    if exponential_backoff:
                        delay = retry_delay * (2 ** (self.retry_count - 1))
                    
                    self._log_with_context(
                        "ERROR",
                        f"Job failed, retrying in {delay}s: {str(e)}",
                        error_type=type(e).__name__,
                        next_retry_delay=delay
                    )
                    
                    time.sleep(delay)
                else:
                    self.status = JobStatus.FAILED
                    self.error_details = {
                        "exception_type": type(e).__name__,
                        "exception_message": str(e),
                        "retry_count": self.retry_count,
                        "max_retries": max_retries
                    }
                    self._log_with_context(
                        "ERROR",
                        f"Job failed after {max_retries} retries: {str(e)}",
                        **self.error_details
                    )
                    raise
        
        raise last_exception or Exception("Job execution failed")
    
    def _execute_job(self, job_func: Callable, *args, **kwargs) -> Any:
        self.start_time = datetime.now(timezone.utc)
        self.status = JobStatus.RUNNING
        
        self._log_with_context(
            "INFO",
            f"Starting job execution",
            config_summary={
                "max_retries": self.config.get("max_retries"),
                "retry_delay": self.config.get("retry_delay_seconds"),
                "exponential_backoff": self.config.get("exponential_backoff")
            }
        )
        
        try:
            result = job_func(*args, **kwargs)
            
            self.end_time = datetime.now(timezone.utc)
            self.status = JobStatus.SUCCESS
            
            duration = self._get_duration()
            self._log_with_context(
                "INFO",
                f"Job completed successfully in {duration:.2f}s",
                duration=duration,
                result_summary=str(result)[:100] if result else None
            )
            
            return result
            
        except Exception as e:
            self.end_time = datetime.now(timezone.utc)
            self.status = JobStatus.FAILED
            
            duration = self._get_duration()
            self._log_with_context(
                "ERROR",
                f"Job failed after {duration:.2f}s: {str(e)}",
                duration=duration,
                error_type=type(e).__name__
            )
            
            raise
    
    def get_job_report(self) -> Dict[str, Any]:
        return {
            "job_name": self.job_name,
            "status": self.status.value,
            "start_time": self.start_time.isoformat() if self.start_time else None,
            "end_time": self.end_time.isoformat() if self.end_time else None,
            "duration": self._get_duration(),
            "retry_count": self.retry_count,
            "error_details": self.error_details,
            "config": self.config
        }


def create_job_monitor(job_name: str, config: Dict[str, Any]) -> JobMonitor:
    return JobMonitor(job_name, config)


if __name__ == "__main__":
    def example_job(success_on_attempt: int = 2):
        import random
        attempt = getattr(example_job, "_attempt", 0) + 1
        example_job._attempt = attempt
        
        if attempt < success_on_attempt:
            raise ValueError(f"Simulated failure on attempt {attempt}")
        
        return f"Success on attempt {attempt}"
    
    config = {
        "max_retries": 3,
        "retry_delay_seconds": 1,
        "exponential_backoff": True
    }
    
    monitor = JobMonitor("example_job", config)
    
    try:
        result = monitor.execute_with_retry(example_job, success_on_attempt=3)
        print(f"Job result: {result}")
    except Exception as e:
        print(f"Job failed: {e}")
    
    print("\nJob report:")
    print(json.dumps(monitor.get_job_report(), indent=2, ensure_ascii=False))