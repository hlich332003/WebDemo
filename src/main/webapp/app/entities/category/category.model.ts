import { IProduct } from 'app/entities/product/product.model';

export interface ICategory {
  id: number;
  name?: string | null;
  slug?: string | null;
  description?: string | null;
  products?: IProduct[] | null;
}

export type NewCategory = Omit<ICategory, 'id'> & { id: null };
