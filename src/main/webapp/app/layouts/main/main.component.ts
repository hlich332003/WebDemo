import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { AccountService } from 'app/core/auth/account.service';
import PageRibbonComponent from '../profiles/page-ribbon.component';
import FooterComponent from '../footer/footer.component';
import { NavbarComponent } from '../navbar/navbar.component';

@Component({
  selector: 'jhi-main',
  standalone: true,
  templateUrl: './main.component.html',
  styleUrls: ['./main.component.scss'],
  imports: [
    RouterOutlet,
    PageRibbonComponent,
    NavbarComponent,
    FooterComponent,
  ],
})
export default class MainComponent implements OnInit {
  private readonly accountService = inject(AccountService);
  private readonly destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe((account) => {
        if (account) {
          console.log('✅ User authenticated on init:', account.email);
        } else {
          console.log('User not authenticated on init.');
        }
      });
  }
}
