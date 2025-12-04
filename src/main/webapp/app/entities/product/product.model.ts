import { ICategory } from 'app/entities/category/category.model';

export interface IProduct {
  id: number;
  name: string;
  description: string | null;
  price?: number | null;
  quantity?: number | null; // Số lượng tồn kho
  imageUrl: string | null;
  isPinned?: boolean | null;
  salesCount?: number | null;
  category?: ICategory | null;
}

export type NewProduct = Omit<IProduct, 'id'> & { id: null };
