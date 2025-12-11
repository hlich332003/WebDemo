import { Component, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'jhi-login-prompt',
  standalone: true,
  imports: [CommonModule, RouterModule, FontAwesomeModule],
  template: `
    <div class="modal-header border-0">
      <h4 class="modal-title w-100 text-center">Yêu Cầu Đăng Nhập</h4>
      <button
        type="button"
        class="btn-close"
        aria-label="Close"
        (click)="activeModal.dismiss('Cross click')"
      ></button>
    </div>
    <div class="modal-body text-center py-4">
      <p>Bạn cần đăng nhập để sử dụng chức năng này.</p>
    </div>
    <div class="modal-footer border-0 d-flex justify-content-center">
      <button
        type="button"
        class="btn btn-secondary"
        (click)="activeModal.dismiss('cancel')"
      >
        Để Sau
      </button>
      <button type="button" class="btn btn-primary" (click)="login()">
        Đăng Nhập
      </button>
    </div>
  `,
})
export class LoginPromptComponent {
  activeModal = inject(NgbActiveModal);
  private router = inject(Router);

  login(): void {
    this.activeModal.close('login');
    this.router.navigate(['/login']);
  }
}
