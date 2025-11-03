import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
import { ProductService, Category } from 'app/shared/product/product.service';
import { CartService } from 'app/shared/cart/cart.service';
import { UtilsService } from 'app/shared/utils/utils.service';

@Component({
  selector: 'jhi-home',
  standalone: true,
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
  imports: [SharedModule, RouterModule, CommonModule],
})
export class HomeComponent implements OnInit, OnDestroy {
  account = signal<Account | null>(null);
  featuredCategories: Category[] = [];

  private readonly destroy$ = new Subject<void>();

  private readonly accountService = inject(AccountService);
  private readonly router = inject(Router);
  private readonly productService = inject(ProductService);
  private readonly cartService = inject(CartService);
  private readonly utils = inject(UtilsService);

  ngOnInit(): void {
    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe(account => this.account.set(account));

    const allCategories = this.productService.getCategories();
    this.featuredCategories = allCategories.slice(0, 3);
  }

  login(): void {
    this.router.navigate(['/login']);
  }

  viewProductDetail(id: number): void {
    this.router.navigate(['/product', id]);
  }

  addToCart(id: number): void {
    this.cartService.addToCart(id);
  }

  formatPrice(price: string | number): string {
    return this.utils.formatPrice(price);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
