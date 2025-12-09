import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';

import { AccountService } from 'app/core/auth/account.service';
import { AppPageTitleStrategy } from 'app/app-page-title-strategy';
import FooterComponent from '../footer/footer.component';
import PageRibbonComponent from '../profiles/page-ribbon.component';

@Component({
  selector: 'jhi-main',
  standalone: true,
  templateUrl: './main.component.html',
  providers: [AppPageTitleStrategy],
  imports: [RouterOutlet, FooterComponent],
})
export default class MainComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly appPageTitleStrategy = inject(AppPageTitleStrategy);
  private readonly accountService = inject(AccountService);

  ngOnInit(): void {
    // Try to log in automatically if token exists
    this.accountService.identity().subscribe({
      next: (account) => {
        if (account) {
          console.log('✅ User authenticated on init:', account.login);
        } else {
          console.log('ℹ️ No user authenticated');
        }
      },
      error: (error) => {
        console.error('❌ Authentication failed on init:', error.status);
        // Ignore 401 error when not logged in
      },
    });
  }
}
