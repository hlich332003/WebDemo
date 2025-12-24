import { IUser } from './user.model';

export const sampleWithRequiredData: IUser = {
  id: 24814,
};

export const sampleWithPartialData: IUser = {
  id: 966,
};

export const sampleWithFullData: IUser = {
  id: 5440,
};
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
