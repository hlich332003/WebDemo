import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { CartService } from 'app/shared/cart/cart.service';
import { UtilsService } from 'app/shared/utils/utils.service';

@Component({
  selector: 'jhi-cart',
  standalone: true,
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.scss'],
  imports: [CommonModule, RouterModule],
})
export class CartComponent implements OnInit {
  cart: any[] = [];

  constructor(
    private cartService: CartService,
    private utils: UtilsService,
  ) {}

  ngOnInit(): void {
    this.cart = this.cartService.getCart();
  }

  updateQuantity(id: number, quantity: number): void {
    this.cartService.updateQuantity(id, quantity);
    this.cart = this.cartService.getCart();
  }

  remove(id: number): void {
    this.cartService.removeFromCart(id);
    this.cart = this.cartService.getCart();
  }

  getTotal(): number {
    return this.cartService.getCartTotal();
  }

  formatPrice(price: string | number): string {
    return this.utils.formatPrice(price);
  }
}
