import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { ProductService, Product, Category } from 'app/shared/product/product.service';
import { CartService } from 'app/shared/cart/cart.service';
import { UtilsService } from 'app/shared/utils/utils.service';

@Component({
  selector: 'jhi-product-list',
  standalone: true,
  templateUrl: './product-list.component.html',
  styleUrls: ['./product-list.component.scss'],
  imports: [CommonModule, FormsModule, RouterModule],
})
export class ProductListComponent implements OnInit {
  allProducts: Product[] = [];
  filteredProducts: Product[] = [];
  categories: Category[] = [];
  selectedCategory = 'all';
  searchTerm = '';

  constructor(
    private productService: ProductService,
    private cartService: CartService,
    private utils: UtilsService,
  ) {}

  ngOnInit(): void {
    this.categories = this.productService.getCategories();
    this.allProducts = this.productService.getAllProducts();
    this.filteredProducts = [...this.allProducts];
  }

  filterByCategory(): void {
    if (this.selectedCategory === 'all') {
      this.filteredProducts = [...this.allProducts];
    } else {
      const category = this.categories.find(c => c.slug === this.selectedCategory);
      this.filteredProducts = category ? [...category.products] : [];
    }
    this.searchProducts();
  }

  searchProducts(): void {
    const term = this.searchTerm.toLowerCase();
    this.filteredProducts = this.filteredProducts.filter(p => p.name.toLowerCase().includes(term));
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.selectedCategory = 'all';
    this.filteredProducts = [...this.allProducts];
  }

  addToCart(id: number): void {
    this.cartService.addToCart(id);
  }

  formatPrice(price: string | number): string {
    return this.utils.formatPrice(price);
  }
}
