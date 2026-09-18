"use client";

import { FormEvent, useEffect, useMemo, useState, type CSSProperties } from "react";
import { createOrder, getOrder, isLiveApi, listOrders } from "@/lib/api";
import type { Order, OrderStatus } from "@/lib/types";

const STEPS: OrderStatus[] = [
  "CREATED",
  "VALIDATED",
  "PAYMENT_PENDING",
  "PAID",
  "SHIPMENT_PENDING",
  "SHIPPED"
];

export default function HomePage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [selected, setSelected] = useState<Order | null>(null);
  const [traceId, setTraceId] = useState<string>();
  const [error, setError] = useState<string>();
  const [busy, setBusy] = useState(false);
  const [customerId, setCustomerId] = useState("cust-42");
  const [fulfillmentType, setFulfillmentType] = useState("PHYSICAL");
  const [sku, setSku] = useState("SKU-100");
  const [quantity, setQuantity] = useState(2);
  const [unitPrice, setUnitPrice] = useState(19.99);

  async function refreshList() {
    const next = await listOrders();
    setOrders(next);
  }

  useEffect(() => {
    refreshList().catch((err) => setError(String(err.message ?? err)));
  }, []);

  useEffect(() => {
    if (!selectedId) return;
    let cancelled = false;
    const tick = async () => {
      try {
        const order = await getOrder(selectedId);
        if (!cancelled) setSelected(order);
      } catch (err) {
        if (!cancelled) setError(String((err as Error).message));
      }
    };
    tick();
    const id = setInterval(tick, 1200);
    return () => {
      cancelled = true;
      clearInterval(id);
    };
  }, [selectedId]);

  const selectedFromList = useMemo(
    () => orders.find((order) => order.id === selectedId) ?? selected,
    [orders, selected, selectedId]
  );
  const view = selected ?? selectedFromList;

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(undefined);
    try {
      const result = await createOrder({
        customerId,
        fulfillmentType,
        sku,
        quantity,
        unitPrice
      });
      setTraceId(result.traceId);
      setSelectedId(result.order.id);
      setSelected(result.order);
      await refreshList();
    } catch (err) {
      setError(String((err as Error).message));
    } finally {
      setBusy(false);
    }
  }

  return (
    <main style={styles.page}>
      <header style={styles.header}>
        <div>
          <p style={styles.kicker}>Cloud-native order platform</p>
          <h1 style={styles.title}>FlowForge</h1>
          <p style={styles.subtitle}>
            REST writes, GraphQL reads, Apache Camel validation, Kafka events, Postgres, OpenTelemetry.
          </p>
        </div>
        <div style={styles.links}>
          <a href="https://github.com/Plk-g/flowforge-order-orchestration" style={styles.link}>
            GitHub
          </a>
          {isLiveApi ? (
            <>
              <a href="http://localhost:8080/swagger-ui.html" style={styles.link}>
                Swagger
              </a>
              <a href="http://localhost:8080/graphiql" style={styles.link}>
                GraphiQL
              </a>
              <a href="http://localhost:16686" style={styles.link}>
                Jaeger
              </a>
            </>
          ) : null}
        </div>
      </header>

      <div style={isLiveApi ? styles.liveBanner : styles.demoBanner}>
        {isLiveApi
          ? "Connected to the local Java stack through Spring Cloud Gateway."
          : "Hosted preview: create an order and watch payment → shipment status advance. The Java/Kafka/Postgres stack runs with docker compose."}
      </div>

      <section style={styles.grid}>
        <form onSubmit={onSubmit} style={styles.card}>
          <h2 style={styles.cardTitle}>Place an order</h2>
          <label style={styles.label}>
            Customer ID
            <input style={styles.input} value={customerId} onChange={(e) => setCustomerId(e.target.value)} />
          </label>
          <label style={styles.label}>
            Fulfillment
            <select
              style={styles.input}
              value={fulfillmentType}
              onChange={(e) => setFulfillmentType(e.target.value)}
            >
              <option value="PHYSICAL">PHYSICAL</option>
              <option value="DIGITAL">DIGITAL</option>
            </select>
          </label>
          <label style={styles.label}>
            SKU
            <input style={styles.input} value={sku} onChange={(e) => setSku(e.target.value)} />
          </label>
          <div style={styles.row}>
            <label style={styles.label}>
              Qty
              <input
                style={styles.input}
                type="number"
                min={1}
                value={quantity}
                onChange={(e) => setQuantity(Number(e.target.value))}
              />
            </label>
            <label style={styles.label}>
              Unit price
              <input
                style={styles.input}
                type="number"
                min={0}
                step="0.01"
                value={unitPrice}
                onChange={(e) => setUnitPrice(Number(e.target.value))}
              />
            </label>
          </div>
          <button style={styles.button} disabled={busy} type="submit">
            {busy ? "Submitting…" : "POST /api/orders"}
          </button>
          {error ? <p style={styles.error}>{error}</p> : null}
          {traceId ? (
            <p style={styles.trace}>
              Trace ID <code>{traceId}</code>
            </p>
          ) : null}
        </form>

        <div style={styles.card}>
          <h2 style={styles.cardTitle}>Live status</h2>
          {!view ? (
            <p style={styles.muted}>Submit an order to watch Camel + events move it to SHIPPED.</p>
          ) : (
            <>
              <p style={styles.mono}>{view.id}</p>
              <p>
                <StatusPill status={view.status} /> {view.customerId} · {view.fulfillmentType} · $
                {view.totalAmount}
              </p>
              <ol style={styles.steps}>
                {STEPS.filter((step) => view.fulfillmentType === "PHYSICAL" || step !== "SHIPMENT_PENDING").map(
                  (step) => (
                    <li key={step} style={styles.step}>
                      <span style={stepReached(view.status, step) ? styles.dotOn : styles.dotOff} />
                      {step}
                    </li>
                  )
                )}
              </ol>
              <ul style={styles.timeline}>
                {(view.timeline ?? []).map((entry, index) => (
                  <li key={`${entry.status}-${index}`}>
                    <strong>{entry.status}</strong> — {entry.detail}
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
      </section>

      <section style={styles.card}>
        <h2 style={styles.cardTitle}>Recent orders</h2>
        <table style={styles.table}>
          <thead>
            <tr>
              <th>ID</th>
              <th>Customer</th>
              <th>Type</th>
              <th>Status</th>
              <th>Total</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr
                key={order.id}
                onClick={() => {
                  setSelectedId(order.id);
                  setSelected(order);
                }}
                style={order.id === selectedId ? styles.selectedRow : styles.rowClick}
              >
                <td style={styles.mono}>{order.id.slice(0, 8)}</td>
                <td>{order.customerId}</td>
                <td>{order.fulfillmentType}</td>
                <td>
                  <StatusPill status={order.status} />
                </td>
                <td>${order.totalAmount}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </main>
  );
}

function StatusPill({ status }: { status: OrderStatus }) {
  const color =
    status === "SHIPPED" ? "#7dffa6" : status === "FAILED" ? "#ff8d8d" : status === "PAID" ? "#ffd27d" : "#8ec8ff";
  return (
    <span
      style={{
        display: "inline-block",
        padding: "2px 8px",
        borderRadius: 999,
        border: `1px solid ${color}`,
        color,
        fontSize: 12,
        letterSpacing: "0.04em"
      }}
    >
      {status}
    </span>
  );
}

function stepReached(current: OrderStatus, step: OrderStatus) {
  return STEPS.indexOf(current) >= STEPS.indexOf(step);
}

const styles: Record<string, CSSProperties> = {
  page: { maxWidth: 1120, margin: "0 auto", padding: "32px 20px 64px" },
  header: { display: "flex", justifyContent: "space-between", gap: 24, alignItems: "flex-start" },
  kicker: { textTransform: "uppercase", letterSpacing: "0.18em", color: "#c7a36a", margin: 0, fontSize: 12 },
  title: { fontSize: 48, margin: "6px 0 8px", fontWeight: 700 },
  subtitle: { margin: 0, maxWidth: 560, color: "#b7c4cc", lineHeight: 1.5 },
  links: { display: "flex", gap: 12, flexWrap: "wrap" },
  link: {
    border: "1px solid #35515c",
    padding: "8px 12px",
    borderRadius: 999,
    textDecoration: "none",
    fontSize: 13
  },
  demoBanner: {
    marginTop: 24,
    padding: "12px 16px",
    borderRadius: 12,
    background: "#1d2a22",
    border: "1px solid #3d6b4f",
    color: "#c8f3d4"
  },
  liveBanner: {
    marginTop: 24,
    padding: "12px 16px",
    borderRadius: 12,
    background: "#1a2733",
    border: "1px solid #3d5f7a",
    color: "#cde4f7"
  },
  grid: { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))", gap: 16, marginTop: 16 },
  card: {
    background: "rgba(16, 24, 30, 0.88)",
    border: "1px solid #24343c",
    borderRadius: 16,
    padding: 20,
    marginTop: 16
  },
  cardTitle: { marginTop: 0, fontSize: 18 },
  label: { display: "flex", flexDirection: "column", gap: 6, fontSize: 13, color: "#9fb0ba", marginBottom: 12 },
  input: {
    background: "#0e161b",
    color: "#e8eef2",
    border: "1px solid #314049",
    borderRadius: 8,
    padding: "10px 12px",
    fontSize: 14
  },
  row: { display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 },
  button: {
    width: "100%",
    marginTop: 8,
    background: "#d7b07a",
    color: "#1a1208",
    border: 0,
    borderRadius: 10,
    padding: "12px 16px",
    fontWeight: 700,
    cursor: "pointer"
  },
  error: { color: "#ffb4b4", whiteSpace: "pre-wrap" },
  trace: { fontSize: 12, color: "#9fb0ba" },
  muted: { color: "#8ea0aa" },
  mono: { fontFamily: '"IBM Plex Mono", monospace', fontSize: 13, color: "#c7a36a" },
  steps: { listStyle: "none", padding: 0, display: "flex", flexWrap: "wrap", gap: 10 },
  step: { display: "flex", alignItems: "center", gap: 6, fontSize: 12, color: "#b7c4cc" },
  dotOn: { width: 8, height: 8, borderRadius: 99, background: "#7dffa6", display: "inline-block" },
  dotOff: { width: 8, height: 8, borderRadius: 99, background: "#314049", display: "inline-block" },
  timeline: { paddingLeft: 18, color: "#c5d0d6", lineHeight: 1.7 },
  table: { width: "100%", borderCollapse: "collapse", fontSize: 14 },
  rowClick: { cursor: "pointer" },
  selectedRow: { cursor: "pointer", background: "#18242b" }
};
