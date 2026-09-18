import { NextRequest, NextResponse } from "next/server";
import { createDemoOrder, listDemoOrders } from "@/lib/demo-store";

export async function GET() {
  return NextResponse.json(listDemoOrders());
}

export async function POST(request: NextRequest) {
  const body = await request.json();
  const items = body.items ?? [];
  if (!body.customerId || !items.length) {
    return NextResponse.json({ detail: "customerId and items are required" }, { status: 400 });
  }
  const order = createDemoOrder({
    customerId: body.customerId,
    fulfillmentType: body.fulfillmentType === "DIGITAL" ? "DIGITAL" : "PHYSICAL",
    items: items.map((item: { sku: string; quantity: number; unitPrice: number }) => ({
      sku: item.sku,
      quantity: Number(item.quantity),
      unitPrice: Number(item.unitPrice)
    }))
  });
  const response = NextResponse.json(order, { status: 201 });
  response.headers.set("X-Trace-Id", `demo-${order.id.slice(0, 8)}`);
  return response;
}
