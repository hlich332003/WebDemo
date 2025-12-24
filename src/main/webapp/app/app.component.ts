import { Component, inject } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import dayjs from 'dayjs/esm';
import { FaIconLibrary } from '@fortawesome/angular-fontawesome';
import { NgbDatepickerConfig } from '@ng-bootstrap/ng-bootstrap';
import locale from '@angular/common/locales/en';
import { fas } from '@fortawesome/free-solid-svg-icons';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { fontAwesomeIcons } from './config/font-awesome-icons';
import MainComponent from './layouts/main/main.component';

@Component({
  selector: 'jhi-app',
  standalone: true,
  template: '<jhi-main />',
  imports: [MainComponent],
})
export default class AppComponent {
  private readonly applicationConfigService = inject(ApplicationConfigService);
  private readonly iconLibrary = inject(FaIconLibrary);
  private readonly dpConfig = inject(NgbDatepickerConfig);

  constructor() {
    this.applicationConfigService.setEndpointPrefix(SERVER_API_URL);
    registerLocaleData(locale);
    this.iconLibrary.addIconPacks(fas);
    this.iconLibrary.addIcons(...fontAwesomeIcons);
    this.dpConfig.minDate = {
      year: dayjs().subtract(100, 'year').year(),
      month: 1,
      day: 1,
    };
  }
}
