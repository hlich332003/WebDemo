import { Injectable, inject, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of, Subject, forkJoin } from 'rxjs';
import {
  map,
  shareReplay,
  catchError,
  takeUntil,
  switchMap,
  tap,
} from 'rxjs/operators';
import { IProduct } from 'app/entities/product/product.model';
import { AccountService } from 'app/core/auth/account.service';

export interface ICartItem {
  product: IProduct;
  quantity: number;
}

@Injectable({
  providedIn: 'root',
})
export class CartService implements OnDestroy {
  private readonly API_URL = 'api/cart';
  private readonly PRODUCT_API_URL = 'api/products';

  private http = inject(HttpClient);
  private accountService = inject(AccountService);

  private cartItemsSubject = new BehaviorSubject<ICartItem[]>([]);
  public cartItems$ = this.cartItemsSubject.asObservable();

  public totalQuantity$ = this.cartItems$.pipe(
    map((items) => items.reduce((total, item) => total + item.quantity, 0)),
    shareReplay(1),
  );
  public totalPrice$ = this.cartItems$.pipe(
    map((items) =>
      items.reduce(
        (total, item) => total + (item.product.price ?? 0) * item.quantity,
        0,
      ),
    ),
    shareReplay(1),
  );

  private destroy$ = new Subject<void>();

  constructor() {
    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe((account) => {
        if (account) {
          this.loadCart();
        } else {
          this.cartItemsSubject.next([]); // Clear cart on logout
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadCart(): void {
    this.http
      .get<any[]>(this.API_URL)
      .pipe(
        switchMap((cartDtos: any[]) => {
          if (cartDtos.length === 0) {
            return of([]);
          }
          const productRequests: Observable<IProduct>[] = cartDtos.map((dto) =>
            this.http.get<IProduct>(`${this.PRODUCT_API_URL}/${dto.productId}`),
          );
          return forkJoin(productRequests).pipe(
            map((products) =>
              cartDtos.map((dto) => {
                const product = products.find((p) => p.id === dto.productId);
                return { product: product!, quantity: dto.quantity };
              }),
            ),
          );
        }),
        catchError(() => of([])), // On error, return empty cart
      )
      .subscribe((cartItems) => {
        this.cartItemsSubject.next(cartItems as ICartItem[]);
      });
  }

  addToCart(productId: number, quantity = 1): Observable<any> {
    return this.http.post(this.API_URL, { productId, quantity });
  }

  updateQuantity(productId: number, quantity: number): Observable<any> {
    return this.http.put(this.API_URL, { productId, quantity });
  }

  removeFromCart(productId: number): Observable<any> {
    return this.http.delete(`${this.API_URL}/${productId}`);
  }

  clearCart(): Observable<any> {
    return this.http.delete(this.API_URL).pipe(
      tap(() => {
        this.cartItemsSubject.next([]);
      }),
    );
  }

  getCartItems(): ICartItem[] {
    return this.cartItemsSubject.value;
  }
}
