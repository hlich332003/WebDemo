import { createReducer, on } from '@ngrx/store';
import * as CartActions from './cart.actions';

export interface CartState {
  items: any[];
  totalItems: number;
  totalPrice: number;
  loading: boolean;
  error: any;
}

export const initialState: CartState = {
  items: [],
  totalItems: 0,
  totalPrice: 0,
  loading: false,
  error: null,
};

export const cartReducer = createReducer(
  initialState,

  // Load Cart
  on(CartActions.loadCart, (state) => ({
    ...state,
    loading: true,
  })),
  on(CartActions.loadCartSuccess, (state, { items }) => ({
    ...state,
    items,
    totalItems: items.reduce((sum, item) => sum + item.quantity, 0),
    totalPrice: items.reduce(
      (sum, item) => sum + item.price * item.quantity,
      0,
    ),
    loading: false,
    error: null,
  })),
  on(CartActions.loadCartFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Add to Cart
  on(CartActions.addToCart, (state) => ({
    ...state,
    loading: true,
  })),
  on(CartActions.addToCartSuccess, (state, { item }) => {
    const existingItem = state.items.find(
      (i) => i.productId === item.productId,
    );
    let updatedItems;

    if (existingItem) {
      updatedItems = state.items.map((i) =>
        i.productId === item.productId
          ? { ...i, quantity: i.quantity + item.quantity }
          : i,
      );
    } else {
      updatedItems = [...state.items, item];
    }

    return {
      ...state,
      items: updatedItems,
      totalItems: updatedItems.reduce((sum, i) => sum + i.quantity, 0),
      totalPrice: updatedItems.reduce(
        (sum, i) => sum + i.price * i.quantity,
        0,
      ),
      loading: false,
    };
  }),
  on(CartActions.addToCartFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Remove from Cart
  on(CartActions.removeFromCart, (state) => ({
    ...state,
    loading: true,
  })),
  on(CartActions.removeFromCartSuccess, (state) => ({
    ...state,
    loading: false,
  })),

  // Update Quantity
  on(CartActions.updateQuantity, (state, { productId, quantity }) => {
    const updatedItems = state.items.map((item) =>
      item.productId === productId ? { ...item, quantity } : item,
    );
    return {
      ...state,
      items: updatedItems,
      totalItems: updatedItems.reduce((sum, i) => sum + i.quantity, 0),
      totalPrice: updatedItems.reduce(
        (sum, i) => sum + i.price * i.quantity,
        0,
      ),
    };
  }),

  // Clear Cart
  on(CartActions.clearCart, (state) => ({
    ...state,
    loading: true,
  })),
  on(CartActions.clearCartSuccess, () => initialState),
);
