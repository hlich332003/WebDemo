import { IUser } from 'app/entities/user/user.model';
import { IProduct } from 'app/entities/product/product.model';
import { Dayjs } from 'dayjs';

export interface IReview {
  id: number;
  rating?: number | null;
  comment?: string | null;
  createdDate?: Dayjs | null;
  user?: Pick<IUser, 'id' | 'login'> | null;
  product?: IProduct | null;
}

export type NewReview = Omit<IReview, 'id'> & { id: null };
