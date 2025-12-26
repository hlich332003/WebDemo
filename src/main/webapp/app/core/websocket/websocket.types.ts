export enum WSRole {
  USER = 'USER',
  ADMIN = 'ADMIN',
}

export interface WSMessage {
  type: string;
  payload: any;
}
