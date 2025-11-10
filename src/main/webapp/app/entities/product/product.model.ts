import { ICategory } from 'app/entities/category/category.model';

export interface IProduct {
  id: number;
  name: string;
  description: string | null;
  price?: number | null; // Đã thay đổi kiểu
  quantity?: number | null; // Đã thay đổi kiểu
  imageUrl: string | null; // Đổi từ 'image' sang 'imageUrl'
  isFeatured?: boolean | null; // Thêm trường isFeatured
  category?: ICategory | null;
}

export type NewProduct = Omit<IProduct, 'id'> & { id: null };
