import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { IProduct } from 'app/entities/product/product.model';
import { AccountService } from 'app/core/auth/account.service';
import { Observable, of, BehaviorSubject, Subject } from 'rxjs';
import { map, shareReplay, catchError, takeUntil, tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class WishlistService {
  private readonly API_URL = 'api/wishlist';

  private http = inject(HttpClient);
  private accountService = inject(AccountService);

  private _itemsSubject = new BehaviorSubject<IProduct[]>([]);
  public items$ = this._itemsSubject.asObservable();

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
          this._itemsSubject.next([]);
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
      .pipe(catchError(() => of([])))
      .subscribe((products) => {
        this._itemsSubject.next(products);
      });
  }

  toggleWishlist(product: IProduct): Observable<boolean> {
    if (!product || !product.id) {
      return of(false);
    }

    const isInWishlist = this.isInWishlist(product.id);
    const apiCall = isInWishlist
      ? this.http.delete(`${this.API_URL}/${product.id}`)
      : this.http.post(`${this.API_URL}/${product.id}`, {});

    return apiCall.pipe(
      tap(() => {
        // Optimistic update: update UI immediately before reload
        const currentItems = this._itemsSubject.value;
        if (isInWishlist) {
          // Remove from wishlist
          this._itemsSubject.next(
            currentItems.filter((p) => p.id !== product.id),
          );
        } else {
          // Add to wishlist
          this._itemsSubject.next([...currentItems, product]);
        }
        // Then load fresh data from server
        this.loadWishlist();
      }),
      map(() => !isInWishlist),
      catchError((err) => {
        this.loadWishlist(); // Reload on error to sync with server
        throw err;
      }),
    );
  }

  isInWishlist(productId: number): boolean {
    return this._itemsSubject.value.some((p) => p.id === productId);
  }
}
