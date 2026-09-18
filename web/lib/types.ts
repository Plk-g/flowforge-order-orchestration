export type FulfillmentType = "DIGITAL" | "PHYSICAL";
export type OrderStatus =
  | "CREATED"
  | "VALIDATED"
  | "PAYMENT_PENDING"
  | "PAID"
  | "SHIPMENT_PENDING"
  | "SHIPPED"
  | "FAILED";

export type TimelineEntry = {
  status: OrderStatus;
  detail: string;
  occurredAt: string;
};

export type Order = {
  id: string;
  customerId: string;
  status: OrderStatus;
  fulfillmentType: FulfillmentType;
  currency: string;
  totalAmount: number | string;
  items: { sku: string; quantity: number; unitPrice: number | string }[];
  timeline: TimelineEntry[];
  createdAt: string;
  updatedAt: string;
};
