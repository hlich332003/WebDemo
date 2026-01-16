import { Component, OnInit, OnDestroy, inject, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { interval, Subject } from 'rxjs';
import { takeUntil, switchMap } from 'rxjs/operators';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
@Component({
  selector: 'jhi-zalopay-payment',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule],
  templateUrl: './zalopay-payment.component.html',
  styleUrls: ['./zalopay-payment.component.scss'],
})
export class ZaloPayPaymentComponent implements OnInit, OnDestroy {
  @Input() orderId!: number;
  paymentUrl: string | null = null;
  safePaymentUrl: SafeResourceUrl | null = null;
  appTransId: string | null = null;
  paymentStatus: 'loading' | 'pending' | 'success' | 'failed' | 'timeout' = 'loading';
  errorMessage: string | null = null;
  private readonly destroy$ = new Subject<void>();
  private readonly CHECK_INTERVAL = 3000; // Check every 3 seconds
  private readonly TIMEOUT_DURATION = 300000; // 5 minutes timeout
  private startTime: number = 0;
  private http = inject(HttpClient);
  private sanitizer = inject(DomSanitizer);
  ngOnInit(): void {
    this.startTime = Date.now();
    this.createPaymentOrder();
  }
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  private createPaymentOrder(): void {
    this.paymentStatus = 'loading';
    this.http
      .post<any>('/api/zalopay/create-order', null, {
        params: { orderId: this.orderId.toString() },
      })
      .subscribe({
        next: response => {
          if (response.return_code === 1) {
            this.paymentUrl = response.order_url;
            this.safePaymentUrl = this.sanitizer.bypassSecurityTrustResourceUrl(response.order_url);
            this.appTransId = response.app_trans_id;
            this.paymentStatus = 'pending';
            this.startPollingPaymentStatus();
          } else {
            this.paymentStatus = 'failed';
            this.errorMessage = response.return_message || 'Không thể tạo đơn thanh toán';
          }
        },
        error: error => {
          console.error('Error creating ZaloPay order:', error);
          this.paymentStatus = 'failed';
          this.errorMessage = 'Lỗi kết nối đến cổng thanh toán';
        },
      });
  }
  private startPollingPaymentStatus(): void {
    interval(this.CHECK_INTERVAL)
      .pipe(
        takeUntil(this.destroy$),
        switchMap(() => this.checkPaymentStatus()),
      )
      .subscribe({
        next: response => {
          // Check for timeout
          if (Date.now() - this.startTime > this.TIMEOUT_DURATION) {
            this.paymentStatus = 'timeout';
            this.destroy$.next();
            return;
          }
          if (response.status === 'SUCCESS') {
            this.paymentStatus = 'success';
            this.destroy$.next();
          }
        },
        error: error => {
          console.error('Error checking payment status:', error);
        },
      });
  }
  private checkPaymentStatus() {
    return this.http.get<any>('/api/zalopay/status/' + this.appTransId);
  }
  openPaymentInNewWindow(): void {
    if (this.paymentUrl) {
      window.open(this.paymentUrl, '_blank', 'width=500,height=700');
    }
  }
  retryPayment(): void {
    this.paymentStatus = 'loading';
    this.errorMessage = null;
    this.startTime = Date.now();
    this.createPaymentOrder();
  }
  getTimeElapsed(): number {
    return Math.floor((Date.now() - this.startTime) / 1000);
  }
}
