import { ActionReducerMap, MetaReducer } from '@ngrx/store';
import { environment } from '../../environments/environment';
import * as fromCart from './cart/cart.reducer';
import * as fromAuth from './auth/auth.reducer';

/**
 * Root State Interface
 */
export interface AppState {
  cart: fromCart.CartState;
  auth: fromAuth.AuthState;
}

/**
 * Root Reducers
 */
export const reducers: ActionReducerMap<AppState> = {
  cart: fromCart.cartReducer,
  auth: fromAuth.authReducer,
};

/**
 * Meta Reducers (for logging, debugging, etc.)
 */
export const metaReducers: MetaReducer<AppState>[] = !environment.production ? [] : [];
