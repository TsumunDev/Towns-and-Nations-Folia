# Grafana & Prometheus Monitoring Setup

**Last Updated**: 2025-12-31  
**Plugin**: Towns & Nations v0.16.0

---

## 📊 Architecture

```
┌─────────────────┐     ┌──────────────┐     ┌─────────────┐
│  Towns & Nations│────▶│  Prometheus  │◀────│   Grafana   │
│    Plugin       │     │   Metrics    │     │  Dashboards │
│  (Port 9090)    │     │   Server     │     │             │
└─────────────────┘     └──────────────┘     └─────────────┘
                              │
                              ▼
                        ┌──────────────┐
                        │ Alertmanager │
                        │  (Alerts)    │
                        └──────────────┘
```

---

## 🚀 Quick Start

### 1. Install Prometheus

```bash
# Download Prometheus
wget https://github.com/prometheus/prometheus/releases/download/v2.48.0/prometheus-2.48.0.linux-amd64.tar.gz
tar xvfz prometheus-*.tar.gz
cd prometheus-*

# Create config
cat > prometheus.yml << 'EOF'
global:
  scrape_interval: 15s
  evaluation_interval: 15s

# Load alert rules
rule_files:
  - "prometheus-alerts.yml"

scrape_configs:
  - job_name: 'towns-and-nations'
    static_configs:
      - targets: ['localhost:9090']  # Plugin metrics endpoint
        labels:
          instance: 'survival-1'
          environment: 'production'
EOF

# Copy alert rules
cp /path/to/Towns-and-Nations/monitoring/prometheus-alerts.yml .

# Start Prometheus
./prometheus --config.file=prometheus.yml
```

**Access**: http://localhost:9090

---

### 2. Install Grafana

```bash
# Ubuntu/Debian
sudo apt-get install -y adduser libfontconfig1
wget https://dl.grafana.com/enterprise/release/grafana-enterprise_10.2.3_amd64.deb
sudo dpkg -i grafana-enterprise_10.2.3_amd64.deb

# Start Grafana
sudo systemctl start grafana-server
sudo systemctl enable grafana-server
```

**Access**: http://localhost:3000 (admin/admin)

---

### 3. Configure Grafana Data Source

1. Open Grafana → **Configuration** → **Data Sources**
2. Click **Add data source**
3. Select **Prometheus**
4. Configure:
   - **URL**: `http://localhost:9090`
   - **Scrape interval**: `15s`
   - **HTTP Method**: `GET`
5. Click **Save & Test**

---

### 4. Import Dashboard

1. Go to **Dashboards** → **Import**
2. Upload `/monitoring/grafana-dashboard.json`
3. Select Prometheus data source
4. Click **Import**

**Dashboard Includes**:
- Database connection pool usage
- Query latency (p95, p99)
- Cache hit rate
- Redis latency
- Circuit breaker status
- GUI open duration
- JVM memory usage
- Active players
- Batch write queue
- Thread pool stats
- Server TPS

---

## 📈 Metrics Reference

### Database Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `hikaricp_connections_active` | Gauge | Active database connections |
| `hikaricp_connections_idle` | Gauge | Idle connections in pool |
| `hikaricp_connections_pending` | Gauge | Threads waiting for connection |
| `hikaricp_connections_max` | Gauge | Maximum pool size |
| `tan_database_query_duration_seconds` | Histogram | Query execution time |
| `tan_database_operations_total` | Counter | Total DB operations (read/write) |
| `tan_batch_write_queue_size` | Gauge | Current batch write queue size |

### Cache Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `tan_cache_hits_total` | Counter | Cache hit count |
| `tan_cache_misses_total` | Counter | Cache miss count |
| `tan_cache_evictions_total` | Counter | Cache evictions |
| `tan_cache_size` | Gauge | Current cache entry count |
| `tan_redis_operation_duration_seconds` | Histogram | Redis operation latency |

### Circuit Breaker Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `tan_circuit_breaker_state` | Gauge | Circuit state (0=CLOSED, 1=OPEN, 2=HALF_OPEN) |
| `tan_circuit_breaker_failures_total` | Counter | Total circuit breaker failures |

### GUI Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `tan_gui_open_duration_seconds` | Histogram | Time to open GUI |
| `tan_gui_open_total` | Counter | Total GUI opens by type |

### JVM Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `jvm_memory_used_bytes` | Gauge | JVM memory usage |
| `jvm_memory_max_bytes` | Gauge | JVM max memory |
| `jvm_threads_live` | Gauge | Live thread count |
| `jvm_threads_daemon` | Gauge | Daemon thread count |

### Application Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `tan_players_online` | Gauge | Current online players |
| `tan_server_tps` | Gauge | Server TPS (estimated) |
| `tan_errors_total` | Counter | Application errors |
| `tan_async_operation_failures_total` | Counter | Failed async operations |

---

## 🚨 Alert Rules

### Alert Severity Levels

- **CRITICAL**: Immediate action required (pager duty)
- **WARNING**: Investigate within 1 hour
- **INFO**: FYI, track trends

### Configured Alerts

#### CRITICAL

1. **DatabasePoolExhaustion**: Pool >90% for 5 min
2. **DatabaseCircuitBreakerOpen**: Circuit breaker triggered
3. **RedisCircuitBreakerOpen**: Redis unavailable
4. **CriticalMemoryUsage**: Heap >95% for 2 min
5. **CriticalServerTPS**: TPS <15 for 2 min
6. **PluginDown**: Health check failing
7. **DatabaseConnectionLost**: Zero connections

#### WARNING

1. **DatabaseHighLatency**: p95 >100ms for 5 min
2. **LowCacheHitRate**: Hit rate <70% for 10 min
3. **RedisHighLatency**: p95 >50ms for 5 min
4. **SlowGUIOpen**: p95 >50ms for 5 min
5. **HighMemoryUsage**: Heap >85% for 5 min
6. **LowServerTPS**: TPS <18 for 5 min
7. **HighErrorRate**: Errors >10/sec for 5 min

#### INFO

1. **CacheEvictionHigh**: Evictions >100/sec
2. **ThreadPoolSaturation**: Thread usage >80%

---

## 🔧 Alertmanager Configuration

**Install Alertmanager**:
```bash
wget https://github.com/prometheus/alertmanager/releases/download/v0.26.0/alertmanager-0.26.0.linux-amd64.tar.gz
tar xvfz alertmanager-*.tar.gz
cd alertmanager-*
```

**Configure Routing** (`alertmanager.yml`):
```yaml
global:
  resolve_timeout: 5m

route:
  group_by: ['alertname', 'severity']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 12h
  receiver: 'discord-webhook'
  
  routes:
    - match:
        severity: critical
      receiver: 'discord-critical'
      repeat_interval: 5m
    
    - match:
        severity: warning
      receiver: 'discord-webhook'
      repeat_interval: 1h

receivers:
  - name: 'discord-webhook'
    webhook_configs:
      - url: 'https://discord.com/api/webhooks/YOUR_WEBHOOK_ID/YOUR_TOKEN'
        send_resolved: true
  
  - name: 'discord-critical'
    webhook_configs:
      - url: 'https://discord.com/api/webhooks/YOUR_CRITICAL_WEBHOOK'
        send_resolved: true

inhibit_rules:
  - source_match:
      severity: 'critical'
    target_match:
      severity: 'warning'
    equal: ['alertname', 'instance']
```

**Start Alertmanager**:
```bash
./alertmanager --config.file=alertmanager.yml
```

**Update Prometheus** to use Alertmanager:
```yaml
# prometheus.yml
alerting:
  alertmanagers:
    - static_configs:
        - targets: ['localhost:9093']
```

---

## 📱 Discord Webhook Integration

### Prometheus Alert Webhook Format

Alertmanager sends webhooks in this format:
```json
{
  "version": "4",
  "groupKey": "{}/{}:{alertname=\"DatabasePoolExhaustion\"}",
  "status": "firing",
  "alerts": [
    {
      "status": "firing",
      "labels": {
        "alertname": "DatabasePoolExhaustion",
        "severity": "critical",
        "component": "database"
      },
      "annotations": {
        "summary": "Database connection pool exhausted",
        "description": "Connection pool usage is 92% (>90%) for 5 minutes."
      },
      "startsAt": "2025-12-31T12:00:00Z"
    }
  ]
}
```

### Discord Bot (Optional - Better Formatting)

Create webhook relay bot for rich embeds:
```python
# discord-alert-relay.py
from flask import Flask, request
import requests

app = Flask(__name__)
DISCORD_WEBHOOK = "YOUR_WEBHOOK_URL"

@app.route('/alert', methods=['POST'])
def relay_alert():
    data = request.json
    
    for alert in data['alerts']:
        severity = alert['labels'].get('severity', 'info')
        color = {
            'critical': 0xFF0000,  # Red
            'warning': 0xFFA500,   # Orange
            'info': 0x00BFFF       # Blue
        }.get(severity, 0x808080)
        
        embed = {
            "embeds": [{
                "title": f"🚨 {alert['labels']['alertname']}",
                "description": alert['annotations']['description'],
                "color": color,
                "fields": [
                    {"name": "Severity", "value": severity.upper(), "inline": True},
                    {"name": "Component", "value": alert['labels']['component'], "inline": True}
                ],
                "timestamp": alert['startsAt']
            }]
        }
        
        requests.post(DISCORD_WEBHOOK, json=embed)
    
    return "OK", 200

if __name__ == '__main__':
    app.run(port=5001)
```

**Update Alertmanager**:
```yaml
receivers:
  - name: 'discord-webhook'
    webhook_configs:
      - url: 'http://localhost:5001/alert'
```

---

## 🎨 Dashboard Customization

### Add Custom Panel

1. Click **Add panel** in dashboard
2. Select visualization type (Graph, Gauge, Table, etc.)
3. Configure query:
   ```promql
   # Example: Top 5 slowest GUI by p95
   topk(5, histogram_quantile(0.95, rate(tan_gui_open_duration_seconds_bucket[5m])))
   ```
4. Set legend, thresholds, colors
5. Save panel

### Useful PromQL Queries

**Database pool usage percentage**:
```promql
(hikaricp_connections_active / hikaricp_connections_max) * 100
```

**Cache hit rate**:
```promql
rate(tan_cache_hits_total[5m]) / 
(rate(tan_cache_hits_total[5m]) + rate(tan_cache_misses_total[5m])) * 100
```

**Top 3 slowest GUI**:
```promql
topk(3, histogram_quantile(0.95, rate(tan_gui_open_duration_seconds_bucket[5m])))
```

**Memory usage percentage**:
```promql
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100
```

---

## 🧪 Testing Alerts

### Trigger Test Alert

1. **Database Pool Exhaustion**: Create 90+ concurrent connections
   ```bash
   for i in {1..100}; do
     mysql -h localhost -u tan_user -p -e "SELECT SLEEP(30);" &
   done
   ```

2. **High Memory**: Force garbage collection delay
   ```java
   // In plugin - temporary test code
   List<byte[]> memoryHog = new ArrayList<>();
   for (int i = 0; i < 1000; i++) {
       memoryHog.add(new byte[1024 * 1024]);  // 1MB each
   }
   ```

3. **Low Cache Hit Rate**: Flush cache manually
   ```java
   QueryCacheManager.getInstance().clearAllCaches();
   ```

4. **Slow GUI**: Add artificial delay
   ```java
   Thread.sleep(100);  // Block for 100ms
   ```

---

## 📊 Performance Baseline

### Expected Metrics (500 Players)

| Metric | Healthy Range | Warning Threshold | Critical Threshold |
|--------|---------------|-------------------|-------------------|
| DB Pool Usage | 20-60% | >70% | >90% |
| DB Query p95 | <30ms | >50ms | >100ms |
| Cache Hit Rate | >85% | <75% | <70% |
| Redis p95 | <15ms | >30ms | >50ms |
| GUI Open p95 | <15ms | >30ms | >50ms |
| Heap Usage | 40-70% | >80% | >90% |
| Server TPS | 19.5-20 | <19 | <15 |

---

## ✅ Setup Checklist

**Prometheus**:
- [ ] Prometheus installed and running
- [ ] Config pointing to plugin:9090
- [ ] Alert rules loaded
- [ ] Targets showing as UP

**Grafana**:
- [ ] Grafana installed and accessible
- [ ] Prometheus data source configured
- [ ] Dashboard imported successfully
- [ ] Panels displaying data

**Alertmanager** (Optional):
- [ ] Alertmanager installed
- [ ] Webhook configured (Discord/Slack/Email)
- [ ] Test alerts firing correctly
- [ ] Alert routing working

**Plugin Config**:
- [ ] `monitoring.enabled = true` in config.yml
- [ ] `prometheus.enabled = true`
- [ ] Port 9090 accessible
- [ ] Metrics endpoint responding: `curl http://localhost:9090/metrics`

---

## 🔧 Troubleshooting

### Issue: No Metrics in Prometheus

**Check**:
1. Plugin config: `monitoring.prometheus.enabled = true`
2. Port accessible: `curl http://localhost:9090/metrics`
3. Prometheus scrape config has correct target
4. Check Prometheus logs: `./prometheus --log.level=debug`

### Issue: Alerts Not Firing

**Check**:
1. Alert rules loaded: `curl http://localhost:9090/api/v1/rules`
2. Prometheus evaluating rules (see /rules page)
3. Alertmanager receiving alerts
4. Webhook URL correct

### Issue: Dashboard Panels Empty

**Check**:
1. Data source connected (green checkmark)
2. PromQL query valid (test in Explore)
3. Time range includes data
4. Metric names match code

---

## 📚 Resources

- **Prometheus Docs**: https://prometheus.io/docs/
- **Grafana Docs**: https://grafana.com/docs/
- **PromQL Cheat Sheet**: https://promlens.com/cheat-sheet/
- **Alert Best Practices**: https://prometheus.io/docs/practices/alerting/

---

**Next Steps**: 
1. Setup Prometheus + Grafana on production server
2. Import dashboard and configure alerts
3. Monitor during 500-player load test
4. Tune alert thresholds based on baseline
