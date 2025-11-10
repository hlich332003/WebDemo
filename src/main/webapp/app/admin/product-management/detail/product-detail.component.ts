import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { IProduct } from 'app/entities/product/product.model'; // Import từ file model mới

@Component({
  selector: 'jhi-product-detail',
  standalone: true,
  imports: [SharedModule, RouterModule],
  templateUrl: './product-detail.component.html',
})
export default class ProductDetailComponent implements OnInit {
  product: IProduct | null = null;

  protected activatedRoute = inject(ActivatedRoute);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ product }) => {
      this.product = product;
    });
  }

  previousState(): void {
    window.history.back();
  }
}
