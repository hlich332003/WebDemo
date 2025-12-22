/* eslint-disable @typescript-eslint/member-ordering */
import { Injectable, inject, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { IProduct } from 'app/entities/product/product.model';
import { AccountService } from 'app/core/auth/account.service';
import { Observable, of, BehaviorSubject, Subject } from 'rxjs';
import { map, catchError, takeUntil, tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class WishlistService implements OnDestroy {
  private readonly API_URL = 'api/wishlist';

  private http = inject(HttpClient);
  private accountService = inject(AccountService);

  private _itemsSubject = new BehaviorSubject<IProduct[]>([]);
  public items$ = this._itemsSubject.asObservable();

  private _countSubject = new BehaviorSubject<number>(0);
  public count$ = this._countSubject.asObservable();

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
          this._countSubject.next(0);
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
        map((products) => {
          const uniqueProducts = Array.from(
            new Map(products.map((p) => [p.id, p])).values(),
          );
          return uniqueProducts;
        }),
        catchError(() => of([])),
      )
      .subscribe((products) => {
        this._itemsSubject.next(products);
        this._countSubject.next(products.length);
      });
  }

  toggleWishlist(product: IProduct): Observable<boolean> {
    if (!product.id) {
      return of(false);
    }

    const currentItems = this._itemsSubject.value;
    const isInWishlist = currentItems.some((p) => p.id === product.id);

    const apiCall = isInWishlist
      ? this.http.delete(`${this.API_URL}/${product.id}`)
      : this.http.post(`${this.API_URL}/${product.id}`, {});

    return apiCall.pipe(
      tap(() => {
        // Reload the wishlist from the server to ensure consistency
        this.loadWishlist();
      }),
      map(() => !isInWishlist),
      catchError((err) => {
        // On error, revert by reloading the original state from the server
        this.loadWishlist();
        // Re-throw the error to be handled by the component
        throw err;
      }),
    );
  }

  isInWishlist(productId: number): boolean {
    return this._itemsSubject.value.some((p) => p.id === productId);
  }
}
