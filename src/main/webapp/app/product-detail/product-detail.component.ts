import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';

import { ProductService, Product } from 'app/shared/product/product.service';
import { CartService } from 'app/shared/cart/cart.service';
import { UtilsService } from 'app/shared/utils/utils.service';

@Component({
  selector: 'jhi-product-detail',
  standalone: true,
  templateUrl: './product-detail.component.html',
  styleUrls: ['./product-detail.component.scss'],
  imports: [CommonModule, RouterModule],
})
export class ProductDetailComponent implements OnInit {
  product: Product | null = null;

  constructor(
    private route: ActivatedRoute,
    private productService: ProductService,
    private cartService: CartService,
    private utils: UtilsService,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.queryParamMap.get('id'));
    if (id) {
      this.product = this.productService.findById(id);
    }
  }

  addToCart(id: number): void {
    this.cartService.addToCart(id);
  }

  formatPrice(price: string | number): string {
    return this.utils.formatPrice(price);
  }
}
