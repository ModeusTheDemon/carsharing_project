#!/usr/bin/env python3
"""
Simple web server to serve DLM monitoring dashboard
"""
import json
import logging
from http.server import HTTPServer, BaseHTTPRequestHandler
from typing import Dict, Any
import threading
import time

from .dashboard import EnhancedDashboard
from .job_status_tracker import JobStatusTracker
from policies.rules_engine import RetentionRulesEngine


class DashboardRequestHandler(BaseHTTPRequestHandler):
    """HTTP request handler for dashboard"""
    
    def __init__(self, dashboard: EnhancedDashboard, *args, **kwargs):
        self.dashboard = dashboard
        super().__init__(*args, **kwargs)
    
    def do_GET(self):
        """Handle GET requests"""
        if self.path == '/':
            # Serve HTML dashboard
            html_content = self.dashboard.generate_html_dashboard()
            self.send_response(200)
            self.send_header('Content-Type', 'text/html; charset=utf-8')
            self.send_header('Content-Length', str(len(html_content.encode('utf-8'))))
            self.end_headers()
            self.wfile.write(html_content.encode('utf-8'))
        
        elif self.path == '/api/data':
            # Serve JSON data
            json_data = self.dashboard.get_dashboard_json()
            response = json.dumps(json_data, ensure_ascii=False)
            
            self.send_response(200)
            self.send_header('Content-Type', 'application/json; charset=utf-8')
            self.send_header('Content-Length', str(len(response.encode('utf-8'))))
            self.end_headers()
            self.wfile.write(response.encode('utf-8'))
        
        elif self.path == '/health':
            # Health check endpoint
            health_data = {
                "status": "healthy",
                "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
                "service": "dlm-dashboard"
            }
            response = json.dumps(health_data, ensure_ascii=False)
            
            self.send_response(200)
            self.send_header('Content-Type', 'application/json; charset=utf-8')
            self.send_header('Content-Length', str(len(response.encode('utf-8'))))
            self.end_headers()
            self.wfile.write(response.encode('utf-8'))
        
        else:
            # 404 for other paths
            self.send_response(404)
            self.send_header('Content-Type', 'text/plain; charset=utf-8')
            self.end_headers()
            self.wfile.write(b'404 Not Found')
    
    def log_message(self, format, *args):
        """Custom log message format"""
        logging.info(f"{self.address_string()} - {format % args}")


class DashboardServer:
    """Dashboard web server"""
    
    def __init__(self, host: str = 'localhost', port: int = 8080, config_path: str = None):
        self.host = host
        self.port = port
        self.config_path = config_path
        self.server = None
        self.thread = None
        self.logger = logging.getLogger("dashboard_server")
        
        # Initialize components
        self.tracker = JobStatusTracker()
        self.rules_engine = RetentionRulesEngine(config_path)
        self.dashboard = EnhancedDashboard(self.tracker, self.rules_engine)
    
    def start(self, background: bool = False):
        """Start the dashboard server"""
        def handler_factory(*args, **kwargs):
            return DashboardRequestHandler(self.dashboard, *args, **kwargs)
        
        self.server = HTTPServer((self.host, self.port), handler_factory)
        
        if background:
            # Start in background thread
            self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
            self.thread.start()
            self.logger.info(f"Dashboard server started in background on http://{self.host}:{self.port}")
        else:
            # Start in foreground
            self.logger.info(f"Dashboard server starting on http://{self.host}:{self.port}")
            self.logger.info("Press Ctrl+C to stop")
            try:
                self.server.serve_forever()
            except KeyboardInterrupt:
                self.logger.info("Server stopped by user")
            finally:
                self.stop()
    
    def stop(self):
        """Stop the dashboard server"""
        if self.server:
            self.server.shutdown()
            self.server.server_close()
            self.logger.info("Dashboard server stopped")
    
    def generate_static_dashboard(self, output_path: str = "dlm_dashboard.html"):
        """Generate static HTML dashboard file"""
        return self.dashboard.save_html_dashboard(output_path)
    
    def get_dashboard_data(self) -> Dict[str, Any]:
        """Get current dashboard data"""
        return self.dashboard.get_dashboard_json()


# Command line interface
if __name__ == "__main__":
    import argparse
    import sys
    
    parser = argparse.ArgumentParser(description="DLM Monitoring Dashboard Server")
    parser.add_argument("--host", default="localhost", help="Server host (default: localhost)")
    parser.add_argument("--port", type=int, default=8080, help="Server port (default: 8080)")
    parser.add_argument("--config", help="Path to configuration file")
    parser.add_argument("--static", metavar="OUTPUT_FILE", 
                       help="Generate static HTML file instead of starting server")
    
    args = parser.parse_args()
    
    # Setup logging
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
    )
    
    try:
        server = DashboardServer(args.host, args.port, args.config)
        
        if args.static:
            # Generate static file
            output_path = server.generate_static_dashboard(args.static)
            print(f"Static dashboard generated: {output_path}")
        else:
            # Start web server
            server.start(background=False)
    
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)