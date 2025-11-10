import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal, computed } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { Subject, forkJoin, Observable } from 'rxjs';
import { takeUntil, map } from 'rxjs/operators';
import { HttpResponse } from '@angular/common/http';

import SharedModule from 'app/shared/shared.module';
import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
import { ProductService } from 'app/entities/product/product.service';
import { IProduct } from 'app/entities/product/product.model';
import { CategoryService } from 'app/entities/category/category.service';
import { ICategory } from 'app/entities/category/category.model';
import { UtilsService } from 'app/shared/utils/utils.service';
import { NotificationService } from 'app/shared/notification/notification.service';
import { CartService } from 'app/shared/services/cart.service';

@Component({
  selector: 'jhi-home',
  standalone: true,
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
  imports: [SharedModule, RouterModule, CommonModule],
})
export class HomeComponent implements OnInit, OnDestroy {
  account = signal<Account | null>(null);
  featuredCategories: ICategory[] = [];
  products: IProduct[] = [];
  featuredProducts: IProduct[] = [];
  isLoading = false;

  private readonly destroy$ = new Subject<void>();

  private readonly accountService = inject(AccountService);
  private readonly router = inject(Router);
  private readonly productService = inject(ProductService);
  private readonly categoryService = inject(CategoryService);
  private readonly utils = inject(UtilsService);
  private readonly notify = inject(NotificationService);
  private readonly cartService = inject(CartService);

  isAdmin = computed(() => {
    const currentAccount = this.account();
    return currentAccount && currentAccount.authorities.includes('ROLE_ADMIN');
  });

  ngOnInit(): void {
    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe(account => this.account.set(account));

    this.loadAllData();
  }

  loadAllData(): void {
    this.isLoading = true;
    forkJoin([
      this.productService.getFeaturedProducts().pipe(map((res: HttpResponse<IProduct[]>) => res.body ?? [])),
      this.categoryService.getFeaturedCategories().pipe(map((res: HttpResponse<ICategory[]>) => res.body ?? [])),
      this.productService.query({ size: 1000 }).pipe(map((res: HttpResponse<IProduct[]>) => res.body ?? [])),
    ]).subscribe({
      next: ([featuredProds, featuredCats, allProducts]) => {
        this.featuredProducts = featuredProds;
        this.featuredCategories = featuredCats;
        this.products = allProducts;

        this.featuredCategories.forEach(category => {
          category.products = this.products.filter(product => product.category?.id === category.id);
        });
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        console.error('Error loading home page data');
      },
    });
  }

  login(): void {
    this.router.navigate(['/login']);
  }

  viewProductDetail(id: number): void {
    this.router.navigate(['/product', id]);
  }

  addToCart(product: IProduct): void {
    const productToAdd: IProduct = {
      ...product,
      price: product.price ?? 0,
      quantity: product.quantity ?? 0,
    };
    this.cartService.addToCart(productToAdd);
    this.notify.success('Đã thêm sản phẩm vào giỏ hàng!');
  }

  formatPrice(price: number | null | undefined): string {
    if (price === null || price === undefined) {
      return this.utils.formatPrice(0);
    }
    return this.utils.formatPrice(price);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
