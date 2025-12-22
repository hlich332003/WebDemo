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
import { Router } from '@angular/router';

export interface ICartItem {
  product: IProduct;
  quantity: number;
  selected: boolean; // Add selected state
}

@Injectable({
  providedIn: 'root',
})
export class CartService implements OnDestroy {
  private readonly API_URL = 'api/cart';
  private readonly PRODUCT_API_URL = 'api/products';
  private readonly BUY_NOW_KEY = 'buy_now_item';

  private http = inject(HttpClient);
  private accountService = inject(AccountService);
  private router = inject(Router);

  private cartItemsSubject = new BehaviorSubject<ICartItem[]>([]);
  public cartItems$ = this.cartItemsSubject.asObservable();

  private isLoading = false;

  // Observable for selected items
  public selectedItems$ = this.cartItems$.pipe(
    map((items) => items.filter((item) => item.selected)),
    shareReplay(1),
  );

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

  // Total price for selected items
  public selectedTotalPrice$ = this.selectedItems$.pipe(
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
          this.cartItemsSubject.next([]);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadCart(): void {
    if (this.isLoading) {
      return;
    }
    this.isLoading = true;
    this.http
      .get<any[]>(this.API_URL)
      .pipe(
        switchMap((cartDtos: any[]) => {
          if (cartDtos.length === 0) {
            return of([]);
          }
          const productRequests = cartDtos.map((dto) =>
            this.http
              .get<IProduct>(`${this.PRODUCT_API_URL}/${dto.productId}`)
              .pipe(catchError(() => of(null))),
          );
          return forkJoin(productRequests).pipe(
            map((products) =>
              cartDtos
                .map((dto, index) => {
                  const product = products[index];
                  if (!product) return null;
                  // Initialize selected state, default to true
                  return {
                    product,
                    quantity: dto.quantity,
                    selected: true,
                  };
                })
                .filter((item): item is ICartItem => item !== null),
            ),
          );
        }),
        catchError(() => of([])),
      )
      .subscribe((cartItems) => {
        this.cartItemsSubject.next(cartItems);
        this.isLoading = false;
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

  // New methods for item selection
  toggleItemSelected(productId: number): void {
    const items = this.getCartItems();
    const item = items.find((i) => i.product.id === productId);
    if (item) {
      item.selected = !item.selected;
      this.cartItemsSubject.next([...items]);
    }
  }

  selectAllItems(selected: boolean): void {
    const items = this.getCartItems().map((item) => ({ ...item, selected }));
    this.cartItemsSubject.next(items);
  }

  // Store selected items for checkout
  private itemsForCheckout: ICartItem[] = [];

  proceedToCheckout(): void {
    // Clear any previous buy now item
    this.clearBuyNowItem();
    this.itemsForCheckout = this.getCartItems().filter((item) => item.selected);
    this.router.navigate(['/checkout']);
  }

  getItemsForCheckout(): ICartItem[] {
    // If checkout is accessed directly, return all items
    if (this.itemsForCheckout.length === 0) {
      return this.getCartItems();
    }
    return this.itemsForCheckout;
  }

  getCart(): Observable<any[]> {
    return this.http.get<any[]>(this.API_URL).pipe(catchError(() => of([])));
  }

  // Buy Now Logic
  setBuyNowItem(product: IProduct, quantity: number): void {
    const item: ICartItem = {
      product,
      quantity,
      selected: true
    };
    sessionStorage.setItem(this.BUY_NOW_KEY, JSON.stringify(item));
    this.router.navigate(['/checkout']);
  }

  getBuyNowItem(): ICartItem | null {
    const itemStr = sessionStorage.getItem(this.BUY_NOW_KEY);
    if (itemStr) {
        try {
            return JSON.parse(itemStr);
        } catch (e) {
            console.error('Error parsing buy now item', e);
            return null;
        }
    }
    return null;
  }

  clearBuyNowItem(): void {
    sessionStorage.removeItem(this.BUY_NOW_KEY);
  }
}
