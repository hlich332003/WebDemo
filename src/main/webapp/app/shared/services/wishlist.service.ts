import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { IProduct } from 'app/entities/product/product.model';
import { AccountService } from 'app/core/auth/account.service';
import { Observable, of, BehaviorSubject, forkJoin, Subject } from 'rxjs';
import {
  tap,
  catchError,
  map,
  shareReplay,
  switchMap,
  takeUntil,
} from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class WishlistService {
  private readonly API_URL = 'api/wishlist';
  private readonly PRODUCT_API_URL = 'api/products';

  private http = inject(HttpClient);
  private accountService = inject(AccountService);

  private _wishlistItemsSubject = new BehaviorSubject<IProduct[]>([]);
  public items$ = this._wishlistItemsSubject.asObservable();
  public count$ = this.items$.pipe(
    map((items) => items.length),
    shareReplay(1),
  );
  private destroy$ = new Subject<void>();

  constructor() {
    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe((account) => {
        if (account) {
          this.loadWishlist();
        } else {
          this._wishlistItemsSubject.next([]); // Clear wishlist on logout
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadWishlist(): void {
    this.http
      .get<IProduct[]>(this.API_URL)
      .pipe(
        switchMap((wishlistProducts: IProduct[]) => {
          if (wishlistProducts.length === 0) {
            return of([]);
          }
          const productRequests: Observable<IProduct>[] = wishlistProducts.map(
            (p) => this.http.get<IProduct>(`${this.PRODUCT_API_URL}/${p.id}`),
          );
          return forkJoin(productRequests);
        }),
        catchError(() => of([])),
      )
      .subscribe((fullProducts) => {
        this._wishlistItemsSubject.next(fullProducts);
      });
  }

  toggleWishlist(product: IProduct): Observable<boolean> {
    if (!product.id) {
      return of(false);
    }
    const isInWishlist = this.isInWishlist(product.id);
    const apiCall = isInWishlist
      ? this.http.delete(`${this.API_URL}/${product.id}`)
      : this.http.post(`${this.API_URL}/${product.id}`, {});

    return apiCall.pipe(
      tap(() => this.loadWishlist()),
      map(() => !isInWishlist),
      catchError((err) => {
        this.loadWishlist();
        throw err;
      }),
    );
  }

  isInWishlist(productId: number): boolean {
    return this._wishlistItemsSubject.value.some((p) => p.id === productId);
  }
}
