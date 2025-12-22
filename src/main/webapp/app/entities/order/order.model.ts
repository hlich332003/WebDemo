export interface IOrder {
  id: number;
}

export type NewOrder = Omit<IOrder, 'id'> & { id: null };

export function getOrderIdentifier(order: IOrder): number {
  return order.id;
}
