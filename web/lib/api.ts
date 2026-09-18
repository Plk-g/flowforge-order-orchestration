import type { Order } from "./types";

const liveApi = process.env.NEXT_PUBLIC_API_URL?.replace(/\/$/, "");

export const isLiveApi = Boolean(liveApi);

function demo(path: string) {
  return `/api/demo${path}`;
}

export async function createOrder(input: {
  customerId: string;
  fulfillmentType: string;
  sku: string;
  quantity: number;
  unitPrice: number;
}): Promise<{ order: Order; traceId?: string }> {
  const body = {
    customerId: input.customerId,
    fulfillmentType: input.fulfillmentType,
    currency: "USD",
    items: [{ sku: input.sku, quantity: input.quantity, unitPrice: input.unitPrice }]
  };
  const url = liveApi ? `${liveApi}/api/orders` : demo("/orders");
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Create failed (${res.status})`);
  }
  const order = (await res.json()) as Order;
  return { order, traceId: res.headers.get("X-Trace-Id") ?? undefined };
}

export async function listOrders(): Promise<Order[]> {
  const url = liveApi ? `${liveApi}/api/orders` : demo("/orders");
  const res = await fetch(url, { cache: "no-store" });
  if (!res.ok) throw new Error("Unable to list orders");
  return res.json();
}

export async function getOrder(id: string): Promise<Order> {
  if (liveApi) {
    const res = await fetch(`${liveApi}/graphql`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        query: `query($id: ID!) { order(id: $id) { id customerId status fulfillmentType currency totalAmount items { sku quantity unitPrice } timeline { status detail occurredAt } createdAt updatedAt } }`,
        variables: { id }
      })
    });
    const payload = await res.json();
    if (payload.errors?.length) throw new Error(payload.errors[0].message);
    return payload.data.order as Order;
  }
  const res = await fetch(demo(`/orders/${id}`), { cache: "no-store" });
  if (!res.ok) throw new Error("Order not found");
  return res.json();
}
