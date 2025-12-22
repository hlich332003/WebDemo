import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { catchError, map, mergeMap, tap } from 'rxjs/operators';
import * as CartActions from './cart.actions';
import { CartService } from '../../shared/services/cart.service';

@Injectable()
export class CartEffects {
  loadCart$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CartActions.loadCart),
      mergeMap(() =>
        this.cartService.getCart().pipe(
          map((items) => CartActions.loadCartSuccess({ items })),
          catchError((error) => of(CartActions.loadCartFailure({ error }))),
        ),
      ),
    ),
  );

  addToCart$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CartActions.addToCart),
      mergeMap(({ productId, quantity }) =>
        this.cartService.addToCart(productId, quantity).pipe(
          map((item) => CartActions.addToCartSuccess({ item })),
          tap(() => console.log('✅ Item added to cart')),
          catchError((error) => of(CartActions.addToCartFailure({ error }))),
        ),
      ),
    ),
  );

  removeFromCart$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CartActions.removeFromCart),
      mergeMap(({ productId }) =>
        this.cartService.removeFromCart(productId).pipe(
          map(() => CartActions.removeFromCartSuccess()),
          tap(() => console.log('✅ Item removed from cart')),
          catchError((error) =>
            of(CartActions.removeFromCartFailure({ error })),
          ),
        ),
      ),
    ),
  );

  clearCart$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CartActions.clearCart),
      mergeMap(() =>
        this.cartService.clearCart().pipe(
          map(() => CartActions.clearCartSuccess()),
          tap(() => console.log('✅ Cart cleared')),
          catchError((error) => of(CartActions.loadCartFailure({ error }))),
        ),
      ),
    ),
  );

  constructor(
    private actions$: Actions,
    private cartService: CartService,
  ) {}
}
