export interface IOrderItem {
  id?: number;
  productId?: number | null;
  productName?: string | null;
  quantity?: number | null;
  price?: number | null;
}

export type NewOrderItem = Omit<IOrderItem, 'id'> & { id: null };
