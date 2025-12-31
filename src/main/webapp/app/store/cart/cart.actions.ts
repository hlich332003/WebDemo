import { createAction, props } from '@ngrx/store';

/**
 * Cart Actions
 */

// Load Cart
export const loadCart = createAction('[Cart] Load Cart');
export const loadCartSuccess = createAction('[Cart] Load Cart Success', props<{ items: any[] }>());
export const loadCartFailure = createAction('[Cart] Load Cart Failure', props<{ error: any }>());

// Add to Cart
export const addToCart = createAction('[Cart] Add Item', props<{ productId: number; quantity: number }>());
export const addToCartSuccess = createAction('[Cart] Add Item Success', props<{ item: any }>());
export const addToCartFailure = createAction('[Cart] Add Item Failure', props<{ error: any }>());

// Remove from Cart
export const removeFromCart = createAction('[Cart] Remove Item', props<{ productId: number }>());
export const removeFromCartSuccess = createAction('[Cart] Remove Item Success');
export const removeFromCartFailure = createAction('[Cart] Remove Item Failure', props<{ error: any }>());

// Update Quantity
export const updateQuantity = createAction('[Cart] Update Quantity', props<{ productId: number; quantity: number }>());
export const updateQuantitySuccess = createAction('[Cart] Update Quantity Success');

// Clear Cart
export const clearCart = createAction('[Cart] Clear Cart');
export const clearCartSuccess = createAction('[Cart] Clear Cart Success');
