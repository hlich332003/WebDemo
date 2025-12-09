import { Injectable } from '@angular/core';
import { CONFIG } from 'app/shared/config/config.constants';

export interface MockProduct {
  id: number;
  name: string;
  price: string;
  image: string;
  quantity: number;
}

export interface MockCategory {
  id: number;
  name: string;
  slug: string;
  products: MockProduct[];
}

@Injectable({ providedIn: 'root' })
export class MockProductService {
  private categoriesData: MockCategory[] = [];

  constructor() {
    this.loadProductsData();
  }

  initSampleData(): void {
    this.categoriesData = [
      {
        id: 1,
        name: '🖥️ Màn Hình',
        slug: 'man-hinh',
        products: [
          {
            id: 101,
            name: 'MSI G274QPF',
            price: '6.990.000',
            image: 'https://via.placeholder.com/200x150/004080/ffffff?text=MSI',
            quantity: 15,
          },
          {
            id: 102,
            name: 'LG 27GN800-B',
            price: '7.500.000',
            image: 'https://via.placeholder.com/200x150/008000/ffffff?text=LG',
            quantity: 8,
          },
          {
            id: 103,
            name: 'ASUS TUF Gaming',
            price: '8.200.000',
            image:
              'https://via.placeholder.com/200x150/ff0000/ffffff?text=ASUS',
            quantity: 12,
          },
          {
            id: 104,
            name: 'Dell UltraSharp',
            price: '9.500.000',
            image:
              'https://via.placeholder.com/200x150/007acc/ffffff?text=Dell',
            quantity: 6,
          },
          {
            id: 105, // Sản phẩm mới
            name: 'ViewSonic VX2780J',
            price: '5.800.000',
            image:
              'https://via.placeholder.com/200x150/f0ad4e/ffffff?text=ViewSonic',
            quantity: 10,
          },
        ],
      },
      {
        id: 2,
        name: '⌨️ Bàn Phím',
        slug: 'ban-phim',
        products: [
          {
            id: 201,
            name: 'Akko 3068B Plus',
            price: '1.290.000',
            image:
              'https://via.placeholder.com/200x150/ff00ff/ffffff?text=Akko',
            quantity: 20,
          },
          {
            id: 202,
            name: 'Keychron K8 Pro',
            price: '1.650.000',
            image:
              'https://via.placeholder.com/200x150/ffff00/000000?text=Keychron',
            quantity: 15,
          },
          {
            id: 203,
            name: 'Razer BlackWidow',
            price: '2.800.000',
            image:
              'https://via.placeholder.com/200x150/00ff00/ffffff?text=Razer',
            quantity: 10,
          },
          {
            id: 204,
            name: 'Logitech MX Keys',
            price: '2.200.000',
            image:
              'https://via.placeholder.com/200x150/000000/ffffff?text=Logitech',
            quantity: 18,
          },
        ],
      },
      {
        id: 3,
        name: '💺 Bàn & Ghế',
        slug: 'ban-ghe',
        products: [
          {
            id: 301,
            name: 'Ghế Gaming DXRacer',
            price: '4.990.000',
            image: 'https://via.placeholder.com/200x150/ff6b35/ffffff?text=Ghế',
            quantity: 7,
          },
          {
            id: 302,
            name: 'Bàn Gaming L-shaped',
            price: '3.500.000',
            image: 'https://via.placeholder.com/200x150/4ecdc4/ffffff?text=Bàn',
            quantity: 5,
          },
          {
            id: 303,
            name: 'Ghế Văn Phòng',
            price: '2.800.000',
            image:
              'https://via.placeholder.com/200x150/45b7d1/ffffff?text=Ghế2',
            quantity: 12,
          },
        ],
      },
      {
        id: 4, // Danh mục mới
        name: '🖱️ Chuột Máy Tính',
        slug: 'chuot-may-tinh',
        products: [
          {
            id: 401,
            name: 'Logitech G Pro X Superlight',
            price: '2.990.000',
            image:
              'https://via.placeholder.com/200x150/d9534f/ffffff?text=LogitechG',
            quantity: 25,
          },
          {
            id: 402,
            name: 'Razer Viper Ultimate',
            price: '2.500.000',
            image:
              'https://via.placeholder.com/200x150/5cb85c/ffffff?text=RazerViper',
            quantity: 18,
          },
        ],
      },
    ];

    this.saveProductsData();
  }

  saveProductsData(): void {
    localStorage.setItem(
      CONFIG.PRODUCTS_STORAGE_KEY,
      JSON.stringify(this.categoriesData),
    );
  }

  loadProductsData(): void {
    const stored = localStorage.getItem(CONFIG.PRODUCTS_STORAGE_KEY);
    try {
      this.categoriesData = stored ? JSON.parse(stored) : [];
    } catch (e) {
      console.error('❌ Lỗi parse dữ liệu:', e);
      this.initSampleData();
    }

    if (this.categoriesData.length === 0) {
      this.initSampleData();
    }
  }

  getAllProducts(): MockProduct[] {
    if (!this.categoriesData || this.categoriesData.length === 0) {
      this.loadProductsData();
    }

    return this.categoriesData.flatMap((category) => category.products || []);
  }

  findById(productId: number): MockProduct | null {
    for (const category of this.categoriesData) {
      const product = category.products.find((p) => p.id === productId);
      if (product) return product;
    }
    return null;
  }

  updateStock(productId: number, newQuantity: number): void {
    const product = this.findById(productId);
    if (product) {
      product.quantity = newQuantity;
      this.saveProductsData();
    }
  }

  getCategories(): MockCategory[] {
    return [...this.categoriesData];
  }
}
