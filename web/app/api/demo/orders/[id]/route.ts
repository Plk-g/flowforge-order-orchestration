import { NextResponse } from "next/server";
import { getDemoOrder } from "@/lib/demo-store";

export async function GET(_: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const order = getDemoOrder(id);
  if (!order) return NextResponse.json({ detail: "not found" }, { status: 404 });
  return NextResponse.json(order);
}
