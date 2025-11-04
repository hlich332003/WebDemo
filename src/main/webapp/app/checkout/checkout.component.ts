import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Router } from '@angular/router';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms'; // Import các module cần thiết

import { CartService } from 'app/shared/cart/cart.service';
import { UtilsService } from 'app/shared/utils/utils.service';
import { NotificationService } from 'app/shared/notification/notification.service';

@Component({
  selector: 'jhi-checkout',
  standalone: true,
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.scss'],
  imports: [CommonModule, RouterModule, ReactiveFormsModule], // Thêm ReactiveFormsModule
})
export class CheckoutComponent implements OnInit {
  cart: any[] = [];
  total = 0;

  // Biểu thức chính quy cho SĐT Việt Nam (10 số, bắt đầu bằng 0)
  vietnamesePhonePattern = /^(0[3|5|7|8|9])+([0-9]{8})$/;

  checkoutForm = new FormGroup({
    fullName: new FormControl('', [Validators.required, Validators.minLength(5)]),
    phone: new FormControl('', [Validators.required, Validators.pattern(this.vietnamesePhonePattern)]),
    email: new FormControl('', [Validators.required, Validators.email]),
    address: new FormControl('', [Validators.required, Validators.minLength(10)]),
    paymentMethod: new FormControl('cod', [Validators.required]),
  });

  constructor(
    public cartService: CartService,
    private utils: UtilsService,
    private notify: NotificationService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.cart = this.cartService.getSelectedItems();
    this.total = this.cartService.getCartTotal();
  }

  formatPrice(price: string | number): string {
    return this.utils.formatPrice(price);
  }

  confirmPayment(): void {
    if (this.checkoutForm.invalid) {
      this.notify.error('Vui lòng kiểm tra lại thông tin khách hàng!');
      // Đánh dấu tất cả các trường là đã chạm vào để hiển thị lỗi
      Object.values(this.checkoutForm.controls).forEach(control => {
        control.markAsTouched();
      });
      return;
    }

    if (this.cart.length === 0) {
      this.notify.error('Giỏ hàng trống hoặc chưa có sản phẩm nào được chọn!');
      return;
    }

    const order = this.cartService.processPayment();
    if (order) {
      this.notify.success('✅ Thanh toán thành công! Cảm ơn bạn đã mua sắm.');
      setTimeout(() => {
        this.router.navigate(['/']);
      }, 2000);
    }
  }
}
