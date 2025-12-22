import { Injectable } from '@angular/core';
import { IProduct } from 'app/entities/product/product.model';

@Injectable({ providedIn: 'root' })
export class ProductFilterService {
  filterProducts(
    products: IProduct[],
    searchTerm: string,
    categorySlug: string,
    minPrice: number | null,
    maxPrice: number | null,
    inStockOnly: boolean,
  ): IProduct[] {
    let filtered = [...products];

    if (searchTerm) {
      filtered = filtered.filter((p) =>
        p.name.toLowerCase().includes(searchTerm.toLowerCase()),
      );
    }

    if (categorySlug && categorySlug !== 'all') {
      filtered = filtered.filter((p) => p.category?.slug === categorySlug);
    }

    if (minPrice !== null) {
      filtered = filtered.filter((p) => (p.price ?? 0) >= minPrice);
    }

    if (maxPrice !== null) {
      filtered = filtered.filter((p) => (p.price ?? 0) <= maxPrice);
    }

    if (inStockOnly) {
      filtered = filtered.filter((p) => (p.quantity ?? 0) > 0);
    }

    return filtered;
  }
}
