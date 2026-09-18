import type { Order, OrderStatus } from "./types";

type Store = {
  orders: Map<string, Order>;
};

const globalStore = globalThis as typeof globalThis & { __flowforge?: Store };

function store(): Store {
  if (!globalStore.__flowforge) {
    globalStore.__flowforge = { orders: new Map() };
  }
  return globalStore.__flowforge;
}

function now() {
  return new Date().toISOString();
}

function append(order: Order, status: OrderStatus, detail: string) {
  order.status = status;
  order.updatedAt = now();
  order.timeline.push({ status, detail, occurredAt: now() });
}

export function listDemoOrders(): Order[] {
  return [...store().orders.values()].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
}

export function getDemoOrder(id: string): Order | undefined {
  return store().orders.get(id);
}

export function createDemoOrder(input: {
  customerId: string;
  fulfillmentType: "DIGITAL" | "PHYSICAL";
  items: { sku: string; quantity: number; unitPrice: number }[];
}): Order {
  const created = now();
  const total = input.items.reduce((sum, item) => sum + item.quantity * item.unitPrice, 0);
  const order: Order = {
    id: crypto.randomUUID(),
    customerId: input.customerId,
    status: "PAYMENT_PENDING",
    fulfillmentType: input.fulfillmentType,
    currency: "USD",
    totalAmount: total.toFixed(2),
    items: input.items,
    timeline: [
      { status: "CREATED", detail: "Order accepted", occurredAt: created },
      { status: "VALIDATED", detail: "Camel route validated items and totals", occurredAt: created },
      { status: "PAYMENT_PENDING", detail: "ORDER_CREATED queued on the outbox", occurredAt: created }
    ],
    createdAt: created,
    updatedAt: created
  };
  store().orders.set(order.id, order);

  setTimeout(() => {
    const current = store().orders.get(order.id);
    if (!current || current.status === "SHIPPED") return;
    append(current, "PAID", "Payment authorized (hosted preview)");
    if (current.fulfillmentType === "DIGITAL") {
      append(current, "SHIPPED", "Digital fulfillment complete");
    } else {
      append(current, "SHIPMENT_PENDING", "Waiting for warehouse pickup");
      setTimeout(() => {
        const later = store().orders.get(order.id);
        if (!later || later.status === "SHIPPED") return;
        append(later, "SHIPPED", "Shipped via FlowForge Logistics FF-DEMO");
      }, 1600);
    }
  }, 1600);

  return order;
}
