export interface IUser {
  id: number;
  login?: string;
}

export type NewUser = Omit<IUser, 'id'> & { id: null };

export function getUserIdentifier(user: IUser): number {
  return user.id;
}
