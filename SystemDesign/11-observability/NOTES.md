# 11 — Observability

> Source: the Primer doesn't cover this. Use these instead:
> - Google *SRE Book*: ch. 4 (*Service Level Objectives*), ch. 6 (*Monitoring
>   Distributed Systems*: the four golden signals)
> - *SRE Workbook* ch. 5 (*Alerting on SLOs*: burn rates)
> - the [OpenTelemetry docs](https://opentelemetry.io/docs/concepts/) for traces and
>   context propagation

09 was about containing failures. This topic is about **noticing them, and finding
out why**, ideally before users tell you. A distributed system fails in ways nobody
predicted: one request crosses ten services, two queues and three data stores. If you
can't see inside it, every incident becomes guesswork.

- **Monitoring** answers questions you knew to ask in advance: dashboards and alerts
  for failures you predicted.
- **Observability** lets you ask **new** questions about failures you didn't predict,
  from the telemetry the system already emits.

The raw material is **metrics, logs and traces**. Each is good at something the
others aren't.

| | **Metrics** | **Logs** | **Traces** |
|---|---|---|---|
| What | numbers over time | timestamped event records | one request's path through every service |
| Answers | *is* something wrong? how much? | *what* exactly happened here? | *where* did the time / the error come from? |
| Cost | cheap (aggregated) | expensive (per event) | expensive, so **sampled** |
| Use for | dashboards, **alerts** | debugging detail, audits | latency breakdowns, finding the failing hop |

---

## 1. Metrics

A metric is a **named time series with labels**:
`http_requests_total{service="checkout", status="500"}`.

### Types

| Type | Behaviour | Example |
|---|---|---|
| **Counter** | only goes up (resets on restart) | requests served, errors. Graph its *rate*. |
| **Gauge** | goes up and down | memory in use, queue depth, active connections |
| **Histogram** | counts values into buckets | request latency, used to compute p50/p95/p99 |

**Collection:** **pull** (Prometheus scrapes each service's `/metrics` endpoint) or
**push** (the service sends metrics to a collector: StatsD, OpenTelemetry). Pull makes
it easy to tell when a target has stopped responding; push suits short-lived jobs.

### What to measure

| Framework | For | Signals |
|---|---|---|
| **Four golden signals** (Google SRE) | any user-facing system | **latency, traffic, errors, saturation** |
| **RED** | request-driven services | **R**ate, **E**rrors, **D**uration |
| **USE** | resources (CPU, disk, pools, queues) | **U**tilisation, **S**aturation, **E**rrors |

**Saturation** gets missed most often: how *full* is the thing (thread pool, queue,
connection pool, disk)? It warns you *before* latency and errors go up. Section 2 of
09 was about exactly this.

### Two metric traps

- **Cardinality explosion:** every unique combination of label values is a separate
  time series. Labelling by `user_id`, `request_id` or full URL creates millions of
  series and a huge bill, or brings down the metrics system. **Labels should have a
  small, bounded set of values** (status code, endpoint *template*, region).
- **You can't average percentiles.** The average of 10 servers' p99s is *not* the
  fleet's p99. Collect **histograms**, merge the buckets across servers, and compute
  the percentile from the merged result.

Also measure **error latency separately**. Fast failures (an instant 500) pull the
average latency *down* just when things are breaking.

---

## 2. Logs

- **Structured, not free text.** Use JSON with consistent fields (`timestamp`, `level`,
  `service`, `trace_id`, `user_id`, `msg`), so logs can be filtered and queried, not
  just grepped.
- **Always include a `trace_id` / `request_id`**, so all the log lines from one request
  across all services can be pulled up together.
- **Canonical log lines:** one wide, structured line per request at the end, holding
  everything about it (route, status, duration, user, DB time, cache hit, …). It
  answers most questions with one query (an approach Stripe popularised).
- **Use log levels honestly.** `ERROR` means someone should look; an `ERROR` for every
  expected 404 drowns real problems.
- **Never log secrets or personal data:** passwords, tokens, card numbers. Logs get
  copied, shipped and kept for a long time. Redact at the source.
- **Logs are for detail, metrics are for counting.** Counting errors by searching logs
  is slow and expensive. Emit a counter as well.
- **Volume is cost:** sample noisy debug logs, and keep recent logs hot and older ones
  in cheaper storage (10's lifecycle tiers).

Typical stacks: ELK / OpenSearch, Grafana Loki, cloud logging services.

---

## 3. Distributed tracing

**Problem:** a checkout request takes 3s. It touched the gateway, 6 services, Redis,
Postgres and Kafka. Which part was slow?

A **trace** is the tree of **spans** for one request. Each span is one unit of work
(an HTTP call, a DB query) with a start time, a duration, attributes and a parent.

```
checkout  ████████████████████████████████████  3000 ms
 ├ auth     ██                                     80 ms
 ├ cart       ███                                 120 ms
 ├ pricing       █████████████████████████       2400 ms   ← here
 │  ├ db query    ██                               90 ms
 │  └ tax-api       ██████████████████████        2250 ms   ← a slow third-party call
 └ payment                                ██      300 ms
```

What traces show at a glance:
- **The slowest hop** in the request
- **Calls made one after another that could run in parallel**
- **N+1 call patterns**: 50 identical small spans in a row (02, 05)
- **Retry storms** (09): the same call repeated

### Context propagation

Every service passes the trace context to the next one in a header (the W3C
**`traceparent`** standard), so spans from different services join into one trace.
**For queues (08), put the context in the message headers**, or the trace stops at
the broker.

### Sampling

Tracing every request is expensive, so keep a sample:

| | How | Trade-off |
|---|---|---|
| **Head-based** | decide at the *start* (e.g. keep 1%) | cheap and simple, but **misses rare errors and slow outliers** (99% are dropped before anyone knows they were interesting) |
| **Tail-based** | decide *after* the request completes: keep all errors and slow requests, plus a sample of normal ones | keeps what matters, but a collector must buffer whole traces first |

### OpenTelemetry

**OpenTelemetry (OTel)** is the vendor-neutral standard: one API and SDK for metrics,
logs and traces, plus a **collector** that sends them to whichever backend you use
(Jaeger, Grafana Tempo, Zipkin, Datadog, …). Instrument with OTel and you can switch
vendors without touching code. In Java, the OTel agent instruments common libraries
(HTTP clients, JDBC, Kafka) automatically.

---

## 4. Using all three together

The investigation flow:

```
1. ALERT (metric)       checkout error rate above SLO burn rate
2. DASHBOARD (metric)   errors only in eu-west, began 14:02, right at a deploy marker
3. TRACE                failing requests: pricing → tax-api spans erroring
4. LOGS (by trace_id)   "tax-api: TLS handshake failed: certificate expired"
```

Two features link them:
- **trace_id in every log line**: go from a trace to its logs.
- **Exemplars**: a metric data point (e.g. one slow request in a latency histogram)
  carries a sample trace ID, so you can jump from the graph to an actual slow request.

---

## 5. SLIs, SLOs, SLAs and error budgets

| | What | Example |
|---|---|---|
| **SLI** (indicator) | a measurement: **good events ÷ valid events** | % of checkout requests that return non-5xx **and** take < 300 ms |
| **SLO** (objective) | an internal **target** for the SLI over a window | 99.9% over a rolling 28 days |
| **SLA** (agreement) | a **contractual** promise to customers, with penalties | 99.5%, or service credits are owed |

**The SLA is looser than the SLO**, so you notice and fix problems before you owe
anybody money.

### Choosing SLOs

- **Measure what users experience,** at the load balancer or the client, not CPU usage
  on a server.
- **Never 100%.** It can't be reached, every extra nine costs roughly 10× more (01),
  and users can't tell 99.99% from 100% through their own Wi-Fi and phone network.
- **Your SLO can't beat your dependencies combined** (01's sequence maths). A service
  that needs three dependencies at 99.9% each can't promise 99.99%.

### Error budget

```
SLO 99.9%  →  error budget = 0.1%  →  ~43 min of failure per 30 days (01's table)
```

The budget turns reliability into a decision **engineering and product share**:
- **Budget left:** ship features, take risks, run experiments.
- **Budget spent:** freeze risky launches and work on reliability until it recovers.

This settles the "move fast" vs "be stable" argument with data instead of opinions.

---

## 6. Alerting

**Page a human only when it's urgent, user-affecting, and needs action from them.**
Everything else is a ticket or a dashboard. Alert fatigue (ignoring alerts because
most are noise) is how real alerts get missed.

### Alert on symptoms, not causes

| ❌ Cause-based | ✅ Symptom-based |
|---|---|
| CPU > 90% | checkout success rate is burning the SLO budget |
| one pod restarted | p99 latency above the SLO threshold |
| DB connections > 80% | users are seeing errors |

High CPU with happy users isn't an emergency. Users seeing errors with normal CPU
still is. Causes belong on dashboards, to use **once** a symptom alert has fired.

### Burn-rate alerts

**Burn rate** = how fast you're using up the error budget (1× = exactly on track to
use it all by the end of the window). Example with a 30-day window (from the *SRE
Workbook*):

| Burn rate | Over | Uses | Action |
|---|---|---|---|
| **14.4×** | 1 hour | 2% of the monthly budget | **page** (fast burn, a big outage) |
| **6×** | 6 hours | 5% | **page** |
| **1×** | 3 days | 10% | **ticket** (slow leak) |

Pair each with a **short window** too (e.g. 1h *and* 5m both burning), so the alert
also **stops** quickly once the problem is fixed.

### Other alerts worth having

- **Missing data:** a service that stops reporting looks "fine" on most dashboards.
  Alert when metrics go silent.
- **Queue lag and oldest-message age, DLQ size** (08): the silent outage.
- **Certificates expiring**, **disks filling up** (alert on *predicted* time to full),
  and **replication lag** (06).
- **Every alert links to a runbook**: what it means, how to check, how to fix.

---

## 7. Dashboards and black-box monitoring

- **One top-level dashboard per service** with the golden signals, plus links to
  dependencies.
- **Show deploy and config-change markers on every graph.** Most outages are changes
  (09), and a marker at the moment a line jumps answers "what changed?" instantly.
- **White-box monitoring** (internal metrics) shows how the system works inside.
  **Black-box / synthetic monitoring** (probes from outside, in several regions,
  running real user flows like log in → add to cart) shows what users experience,
  including DNS, CDN and certificate failures your internal metrics never see.
- **Real user monitoring (RUM)** measures the frontend in real browsers: page load,
  JS errors, actual network conditions.

---

## 8. Incidents

- **Mitigate first, investigate later.** Roll back, fail over, switch off the feature
  flag (09), *then* find the root cause. Users care about recovery, not explanations.
- **Clear roles:** an incident commander coordinates while others investigate, plus
  someone who communicates with stakeholders and customers.
- **Metrics that matter:** **MTTD** (mean time to detect) and **MTTR** (mean time to
  recover). Observability improves the first, while rollbacks and runbooks improve
  the second.
- **Blameless postmortems:** timeline, impact, contributing factors, and **action items
  with owners**. Ask "how did the system let this happen?", not "who did it?". Blame
  teaches people to hide problems.

---

## 9. What to instrument, by topic

Each earlier topic, and the signal that warns you it's in trouble:

| Topic | Watch |
|---|---|
| 03 Load balancing | healthy host count, 5xx from the LB vs from backends, connection count |
| 04 Caching | **hit ratio**, evictions, memory, hot keys |
| 05 Databases | slow queries, connection pool usage, lock waits, disk |
| 06 Replication | **replication lag**, failover events |
| 07 Consensus | leader changes (frequent elections = instability) |
| 08 Messaging | **consumer lag, oldest message age, DLQ size**, rebalances |
| 09 Reliability | circuit breaker state, rate-limit rejections, retry rate, shed requests |
| 10 Storage & search | upload failures, search refresh lag, index size, query latency |

---

## 10. Cost

Observability can reach **10–30% of the infrastructure bill**. The usual reasons, and
their fixes:
- **High-cardinality metrics** → bounded labels.
- **Log volume** → structured canonical log lines instead of many chatty lines; sample
  debug logs; tiered retention.
- **Traces** → tail-based sampling (keep errors and slow requests, sample the rest).

---

## Gotchas

- **Monitoring ≠ observability.** Dashboards cover known failures; you also need to be
  able to ask new questions.
- **Unbounded labels (user_id, request_id, raw URL) explode metrics cardinality.**
- **Never average percentiles.** Merge histograms, then compute the percentile.
- **Fast errors make latency look *better*.** Track error latency separately.
- **Put trace_id in every log line**, and propagate trace context through queues too.
- **Head-based sampling drops the interesting traces.** Use tail-based sampling to keep
  errors and slow requests.
- **Page on user-facing symptoms (SLO burn), not on CPU.** Causes go on dashboards.
- **SLA < SLO < 100%.** The gap is your early warning, and the error budget is how
  reliability and feature work share time.
- **Alert when data goes missing.** A dead service that stops reporting can look
  perfectly healthy.
- **Every page needs a runbook and an action.** Otherwise it's alert fatigue.
- **Mark deploys on graphs.** "What changed?" is the first question in every incident.
- **Mitigate before you investigate,** and run postmortems blameless.
- **Never log secrets or personal data.**
