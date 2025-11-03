import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router'; // Import Router

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
    private route: ActivatedRoute,
    private router: Router, // Inject Router
  ) {}

  ngOnInit(): void {
    this.categories = this.productService.getCategories();
    this.allProducts = this.productService.getAllProducts();

    this.route.queryParams.subscribe(params => {
      const category = params['category'] ?? 'all';
      this.selectedCategory = category;

      const search = params['search'] ?? '';
      this.searchTerm = search;

      this.filterAndSearch();
    });
  }

  filterAndSearch(): void {
    const term = this.searchTerm.toLowerCase().trim();

    // Chỉ tìm theo danh mục nếu có từ khóa tìm kiếm thực sự
    if (term) {
      const matchingCategory = this.categories.find(c => c.name.toLowerCase().includes(term));
      if (matchingCategory) {
        this.filteredProducts = [...matchingCategory.products];
        return; // Dừng lại sau khi đã tìm thấy sản phẩm theo danh mục
      }
    }

    // Nếu không, tiếp tục tìm kiếm theo tên sản phẩm như cũ
    let base: Product[] = [];
    if (this.selectedCategory === 'all') {
      base = [...this.allProducts];
    } else {
      const category = this.categories.find(c => c.slug === this.selectedCategory);
      base = category ? [...category.products] : [];
    }

    this.filteredProducts = base.filter(p => p.name.toLowerCase().includes(term));
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.selectedCategory = 'all';
    this.filterAndSearch();
  }

  addToCart(id: number): void {
    this.cartService.addToCart(id);
  }

  formatPrice(price: string | number): string {
    return this.utils.formatPrice(price);
  }

  viewProductDetail(id: number): void {
    this.router.navigate(['/product', id]);
  }
}
