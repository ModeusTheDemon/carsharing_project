#!/usr/bin/env python3
import json
import logging
from datetime import datetime, timezone
from typing import Dict, Any, Optional
import sqlite3
from pathlib import Path

from .job_status_tracker import JobStatusTracker, JobType
from policies.rules_engine import RetentionRulesEngine


class EnhancedDashboard:
    def __init__(self, tracker: JobStatusTracker, rules_engine: RetentionRulesEngine):
        self.tracker = tracker
        self.rules_engine = rules_engine
        self.logger = logging.getLogger("enhanced_dashboard")
    
    def generate_html_dashboard(self) -> str:
        dashboard_data = self.tracker.generate_dashboard_data()
        config_summary = self.rules_engine.get_config_summary()
        error_config = self.rules_engine.get_error_handling_config()
        
        charts_data = self._generate_charts_data(dashboard_data)
        
        html = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DLM Job Monitoring Dashboard</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        body {{ font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }}
        .container {{ max-width: 1200px; margin: 0 auto; }}
        .header {{ background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; }}
        .stats-grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin-bottom: 20px; }}
        .stat-card {{ background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }}
        .stat-card h3 {{ margin-top: 0; color: #333; }}
        .stat-value {{ font-size: 2em; font-weight: bold; margin: 10px 0; }}
        .success {{ color: #10b981; }}
        .failure {{ color: #ef4444; }}
        .chart-container {{ background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); margin-bottom: 20px; }}
        .chart-container h3 {{ margin-top: 0; }}
        .config-section {{ background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); margin-bottom: 20px; }}
        .config-section pre {{ background: #f8f9fa; padding: 15px; border-radius: 4px; overflow-x: auto; }}
        table {{ width: 100%; border-collapse: collapse; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }}
        th, td {{ padding: 12px 15px; text-align: left; border-bottom: 1px solid #ddd; }}
        th {{ background-color: #f8f9fa; font-weight: bold; }}
        tr:hover {{ background-color: #f5f5f5; }}
        .status-success {{ color: #10b981; font-weight: bold; }}
        .status-failed {{ color: #ef4444; font-weight: bold; }}
        .status-running {{ color: #3b82f6; font-weight: bold; }}
        .timestamp {{ color: #6b7280; font-size: 0.9em; }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>DLM Job Monitoring Dashboard</h1>
            <p class="timestamp">Last updated: {dashboard_data['timestamp']}</p>
        </div>
        
        <div class="stats-grid">
            <div class="stat-card">
                <h3>Overall Success Rate</h3>
                <div class="stat-value success">{(dashboard_data['overall_stats']['statistics']['success'] / dashboard_data['overall_stats']['statistics']['total'] if dashboard_data['overall_stats']['statistics']['total'] > 0 else 0):.1%}</div>
                <p>{dashboard_data['overall_stats']['statistics']['success']} successful / {dashboard_data['overall_stats']['statistics']['total']} total</p>
            </div>
            
            <div class="stat-card">
                <h3>Average Duration</h3>
                <div class="stat-value">{(dashboard_data['overall_stats']['statistics']['avg_duration'] or 0):.2f}s</div>
                <p>Across all job executions</p>
            </div>
            
            <div class="stat-card">
                <h3>Error Handling</h3>
                <div class="stat-value">{error_config['max_retries']} retries</div>
                <p>{error_config['retry_delay_seconds']}s delay with {'exponential backoff' if error_config['exponential_backoff'] else 'fixed delay'}</p>
            </div>
            
            <div class="stat-card">
                <h3>Active Jobs</h3>
                <div class="stat-value">{dashboard_data['overall_stats']['statistics']['running']}</div>
                <p>Currently running jobs</p>
            </div>
        </div>
        
        <div class="chart-container">
            <h3>Job Execution Success Rate by Type</h3>
            <canvas id="jobSuccessChart" width="400" height="200"></canvas>
        </div>
        
        <div class="chart-container">
            <h3>Hourly Failure Rate Trend (Last 24 Hours)</h3>
            <canvas id="failureTrendChart" width="400" height="200"></canvas>
        </div>
        
        <div class="config-section">
            <h3>Current Configuration</h3>
            <pre>{config_summary}</pre>
        </div>
        
        <h2>Recent Job Executions</h2>
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Job Type</th>
                    <th>Status</th>
                    <th>Start Time</th>
                    <th>Duration</th>
                    <th>Records</th>
                    <th>Error</th>
                </tr>
            </thead>
            <tbody>
                {self._generate_recent_executions_table(dashboard_data)}
            </tbody>
        </table>
        
        <div style="margin-top: 30px; text-align: center; color: #6b7280; font-size: 0.9em;">
            <p>DLM Monitoring Dashboard • Generated at {datetime.now(timezone.utc).isoformat()}</p>
        </div>
    </div>
    
    <script>
        const jobSuccessCtx = document.getElementById('jobSuccessChart').getContext('2d');
        new Chart(jobSuccessCtx, {{
            type: 'bar',
            data: {{
                labels: {json.dumps(charts_data['job_labels'])},
                datasets: [
                    {{
                        label: 'Success Rate',
                        data: {json.dumps(charts_data['success_rates'])},
                        backgroundColor: '#10b981',
                        borderColor: '#059669',
                        borderWidth: 1
                    }}
                ]
            }},
            options: {{
                responsive: true,
                scales: {{
                    y: {{
                        beginAtZero: true,
                        max: 1,
                        ticks: {{
                            callback: function(value) {{
                                return (value * 100).toFixed(0) + '%';
                            }}
                        }}
                    }}
                }}
            }}
        }});
        
        const failureTrendCtx = document.getElementById('failureTrendChart').getContext('2d');
        new Chart(failureTrendCtx, {{
            type: 'line',
            data: {{
                labels: {json.dumps(charts_data['hour_labels'])},
                datasets: [
                    {{
                        label: 'Failure Rate',
                        data: {json.dumps(charts_data['failure_rates'])},
                        backgroundColor: 'rgba(239, 68, 68, 0.2)',
                        borderColor: '#ef4444',
                        borderWidth: 2,
                        fill: true
                    }}
                ]
            }},
            options: {{
                responsive: true,
                scales: {{
                    y: {{
                        beginAtZero: true,
                        ticks: {{
                            callback: function(value) {{
                                return (value * 100).toFixed(0) + '%';
                            }}
                        }}
                    }}
                }}
            }}
        }});
    </script>
</body>
</html>"""
        
        return html
    
    def _generate_charts_data(self, dashboard_data: Dict[str, Any]) -> Dict[str, Any]:
        charts_data = {
            'job_labels': [],
            'success_rates': [],
            'hour_labels': [],
            'failure_rates': []
        }
        
        for job_type, stats in dashboard_data['by_job_type'].items():
            charts_data['job_labels'].append(job_type.capitalize())
            total = stats['statistics']['total']
            success = stats['statistics']['success']
            success_rate = success / total if total > 0 else 0
            charts_data['success_rates'].append(success_rate)
        
        hourly_trends = dashboard_data['overall_stats']['hourly_trends']
        for trend in hourly_trends[:12]:
            hour_label = trend['hour'][11:16]
            charts_data['hour_labels'].append(hour_label)
            charts_data['failure_rates'].append(trend['failure_rate'])
        
        return charts_data
    
    def _generate_recent_executions_table(self, dashboard_data: Dict[str, Any]) -> str:
        rows = []
        recent_executions = dashboard_data['overall_stats']['recent_executions']
        
        for exec_data in recent_executions[:10]:
            status_class = f"status-{exec_data['status']}"
            status_display = exec_data['status'].capitalize()
            
            duration = exec_data['duration_seconds']
            duration_display = f"{duration:.2f}s" if duration else "N/A"
            
            start_time = exec_data['start_time']
            if start_time:
                start_display = start_time[11:19]
            else:
                start_display = "N/A"
            
            error_msg = exec_data['error_message']
            error_display = error_msg[:50] + "..." if error_msg and len(error_msg) > 50 else error_msg or ""
            
            row = f"""
                <tr>
                    <td>{exec_data['id']}</td>
                    <td>{exec_data['job_type']}</td>
                    <td class="{status_class}">{status_display}</td>
                    <td>{start_display}</td>
                    <td>{duration_display}</td>
                    <td>{exec_data['records_processed'] or 0}</td>
                    <td title="{error_msg or ''}">{error_display}</td>
                </tr>
            """
            rows.append(row)
        
        return "\n".join(rows)
    
    def save_html_dashboard(self, output_path: str = "dlm_dashboard.html") -> str:
        html_content = self.generate_html_dashboard()
        
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write(html_content)
        
        self.logger.info(f"Dashboard saved to {output_path}")
        return output_path
    
    def get_dashboard_json(self) -> Dict[str, Any]:
        dashboard_data = self.tracker.generate_dashboard_data()
        config_summary = self.rules_engine.get_config_summary()
        error_config = self.rules_engine.get_error_handling_config()
        
        return {
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "overall_stats": dashboard_data["overall_stats"],
            "job_type_stats": dashboard_data["by_job_type"],
            "configuration": {
                "summary": config_summary,
                "error_handling": error_config,
                "scheduling": self.rules_engine.get_schedule_config()
            },
            "system_status": {
                "database_connected": self._check_database_connection(),
                "database_path": self.tracker.db_path if hasattr(self.tracker, 'db_path') else "N/A"
            }
        }
    
    def _check_database_connection(self) -> bool:
        try:
            from database.session import get_db
            with get_db() as db:
                db.execute("SELECT 1")
            return True
        except Exception:
            return False


if __name__ == "__main__":
    import argparse
    import sys
    
    parser = argparse.ArgumentParser(description="Generate enhanced DLM monitoring dashboard")
    parser.add_argument("--output", "-o", default="dlm_dashboard.html", 
                       help="Output HTML file path")
    parser.add_argument("--json", action="store_true", 
                       help="Output as JSON instead of HTML")
    parser.add_argument("--config", help="Path to configuration file")
    
    args = parser.parse_args()
    
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
    )
    
    try:
        tracker = JobStatusTracker()
        rules_engine = RetentionRulesEngine(args.config)
        
        dashboard = EnhancedDashboard(tracker, rules_engine)
        
        if args.json:
            json_data = dashboard.get_dashboard_json()
            print(json.dumps(json_data, indent=2, ensure_ascii=False))
        else:
            output_path = dashboard.save_html_dashboard(args.output)
            print(f"Dashboard generated: {output_path}")
            print(f"Open {output_path} in a web browser to view the dashboard")
    
    except Exception as e:
        print(f"Error generating dashboard: {e}", file=sys.stderr)
        sys.exit(1)