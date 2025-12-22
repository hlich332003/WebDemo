import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { AccountService } from 'app/core/auth/account.service';
import FooterComponent from '../footer/footer.component';
import { ChatWidgetComponent } from '../chat-widget/chat-widget.component';

@Component({
  selector: 'jhi-main',
  standalone: true,
  templateUrl: './main.component.html',
  styleUrls: ['./main.component.scss'],
  imports: [CommonModule, RouterOutlet, FooterComponent, ChatWidgetComponent],
})
export default class MainComponent implements OnInit, OnDestroy {
  showChatWidget = false;

  private readonly accountService = inject(AccountService);
  private readonly destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe((account) => {
        if (account) {
          // Chỉ hiển thị chat widget cho người dùng thường (không phải admin/CSKH)
          this.showChatWidget = !this.accountService.hasAnyAuthority([
            'ROLE_ADMIN',
            'ROLE_CSKH',
          ]);
        } else {
          this.showChatWidget = false;
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
