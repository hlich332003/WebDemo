export interface IProductSearchDTO {
  id?: number;
  name?: string;
  description?: string;
  price?: number;
  imageUrl?: string;
  categoryName?: string;
}

export type NewProductSearchDTO = Omit<IProductSearchDTO, 'id'> & { id: null };
