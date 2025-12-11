import { Injectable, inject } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { LoginPromptComponent } from 'app/shared/components/login-prompt/login-prompt.component';

@Injectable({ providedIn: 'root' })
export class LoginModalService {
  private modalService = inject(NgbModal);

  open(): void {
    this.modalService.open(LoginPromptComponent, {
      centered: true,
      size: 'sm',
    });
  }
}
