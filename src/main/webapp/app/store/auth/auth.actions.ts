import { createAction, props } from '@ngrx/store';

/**
 * Auth Actions
 */

// Login
export const login = createAction(
  '[Auth] Login',
  props<{ email: string; password: string }>(),
);
export const loginSuccess = createAction(
  '[Auth] Login Success',
  props<{ user: any; token: string }>(),
);
export const loginFailure = createAction(
  '[Auth] Login Failure',
  props<{ error: any }>(),
);

// Logout
export const logout = createAction('[Auth] Logout');
export const logoutSuccess = createAction('[Auth] Logout Success');

// Load User
export const loadUser = createAction('[Auth] Load User');
export const loadUserSuccess = createAction(
  '[Auth] Load User Success',
  props<{ user: any }>(),
);
export const loadUserFailure = createAction(
  '[Auth] Load User Failure',
  props<{ error: any }>(),
);
