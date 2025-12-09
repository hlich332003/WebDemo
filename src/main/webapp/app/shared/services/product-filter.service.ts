import { Injectable, signal } from '@angular/core';

export interface PriceRange {
  min: number;
  max: number;
}

export interface FilterOptions {
  priceRange?: PriceRange;
  categories?: number[];
  inStock?: boolean;
  minRating?: number;
  sortBy?:
    | 'price-asc'
    | 'price-desc'
    | 'name-asc'
    | 'name-desc'
    | 'newest'
    | 'popular';
}

@Injectable({
  providedIn: 'root',
})
export class ProductFilterService {
  // Signal để reactive updates
  private filterOptions = signal<FilterOptions>({
    priceRange: undefined,
    categories: [],
    inStock: undefined,
    minRating: undefined,
    sortBy: 'newest',
  });

  // Public readonly signal
  public readonly filters = this.filterOptions.asReadonly();

  /**
   * Cập nhật price range filter
   */
  setPriceRange(min: number, max: number): void {
    this.filterOptions.update((current) => ({
      ...current,
      priceRange: { min, max },
    }));
  }

  /**
   * Xóa price range filter
   */
  clearPriceRange(): void {
    this.filterOptions.update((current) => ({
      ...current,
      priceRange: undefined,
    }));
  }

  /**
   * Toggle category filter
   */
  toggleCategory(categoryId: number): void {
    this.filterOptions.update((current) => {
      const categories = current.categories ?? [];
      const index = categories.indexOf(categoryId);

      if (index > -1) {
        // Remove
        return {
          ...current,
          categories: categories.filter((id) => id !== categoryId),
        };
      } else {
        // Add
        return {
          ...current,
          categories: [...categories, categoryId],
        };
      }
    });
  }

  /**
   * Set in-stock filter
   */
  setInStockFilter(inStock: boolean | undefined): void {
    this.filterOptions.update((current) => ({
      ...current,
      inStock,
    }));
  }

  /**
   * Set minimum rating filter
   */
  setMinRating(rating: number | undefined): void {
    this.filterOptions.update((current) => ({
      ...current,
      minRating: rating,
    }));
  }

  /**
   * Set sort option
   */
  setSortBy(sortBy: FilterOptions['sortBy']): void {
    this.filterOptions.update((current) => ({
      ...current,
      sortBy,
    }));
  }

  /**
   * Reset tất cả filters
   */
  resetFilters(): void {
    this.filterOptions.set({
      priceRange: undefined,
      categories: [],
      inStock: undefined,
      minRating: undefined,
      sortBy: 'newest',
    });
  }

  /**
   * Get current filters
   */
  getCurrentFilters(): FilterOptions {
    return this.filterOptions();
  }

  /**
   * Kiểm tra có filter nào active không
   */
  hasActiveFilters(): boolean {
    const current = this.filterOptions();
    return !!(
      current.priceRange ||
      (current.categories && current.categories.length > 0) ||
      current.inStock !== undefined ||
      current.minRating !== undefined
    );
  }
}
