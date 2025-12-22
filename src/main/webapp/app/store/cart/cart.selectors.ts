import { createFeatureSelector, createSelector } from '@ngrx/store';
import { CartState } from './cart.reducer';

export const selectCartState = createFeatureSelector<CartState>('cart');

export const selectCartItems = createSelector(
  selectCartState,
  (state) => state.items,
);

export const selectTotalItems = createSelector(
  selectCartState,
  (state) => state.totalItems,
);

export const selectTotalPrice = createSelector(
  selectCartState,
  (state) => state.totalPrice,
);

export const selectCartLoading = createSelector(
  selectCartState,
  (state) => state.loading,
);

export const selectCartError = createSelector(
  selectCartState,
  (state) => state.error,
);
