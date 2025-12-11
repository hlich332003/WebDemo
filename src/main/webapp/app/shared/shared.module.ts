import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { AlertComponent } from './alert/alert.component';
import { AlertErrorComponent } from './alert/alert-error.component';
import { WishlistService } from './services/wishlist.service';
import { RecentlyViewedService } from './services/recently-viewed.service';
import { ProductComparisonService } from './services/product-comparison.service';

/**
 * Application wide Module
 */
@NgModule({
  imports: [AlertComponent, AlertErrorComponent],
  exports: [
    CommonModule,
    NgbModule,
    FontAwesomeModule,
    AlertComponent,
    AlertErrorComponent,
  ],
  providers: [WishlistService, RecentlyViewedService, ProductComparisonService],
})
export default class SharedModule {}
