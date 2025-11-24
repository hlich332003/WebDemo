import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { HttpResponse } from '@angular/common/http';

import SharedModule from 'app/shared/shared.module';
import { IProduct } from 'app/entities/product/product.model';
import { ProductService } from 'app/entities/product/product.service';
import { CartService } from 'app/shared/services/cart.service'; // Sửa đường dẫn import
import { UtilsService } from 'app/shared/utils/utils.service';
import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
import { NotificationService } from 'app/shared/notification/notification.service';

@Component({
  selector: 'jhi-product-detail',
  standalone: true,
  templateUrl: './product-detail.component.html',
  styleUrls: ['./product-detail.component.scss'],
  imports: [SharedModule, RouterModule],
})
export class ProductDetailComponent implements OnInit {
  product: IProduct | null = null;
  isLoading = false;
  account = signal<Account | null>(null);

  private productService = inject(ProductService);
  private cartService = inject(CartService); // Inject CartService
  private utils = inject(UtilsService);
  private route = inject(ActivatedRoute);
  private accountService = inject(AccountService);
  private notify = inject(NotificationService);

  ngOnInit(): void {
    this.accountService.getAuthenticationState().subscribe(account => this.account.set(account));
    this.route.paramMap.subscribe(params => {
      const productId = params.get('id');
      if (productId) {
        this.loadProductDetail(+productId);
      }
    });
  }

  loadProductDetail(id: number): void {
    this.isLoading = true;
    this.productService.find(id).subscribe({
      next: (res: HttpResponse<IProduct>) => {
        this.isLoading = false;
        this.product = res.body;
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }

  // Sửa phương thức addToCart để nhận IProduct
  addToCart(product: IProduct): void {
    const productToAdd: IProduct = {
      ...product,
      price: product.price ?? 0,
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

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'content/images/default-product.svg';
  }

  previousState(): void {
    window.history.back();
  }
}
