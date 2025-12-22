import { Component, OnInit, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import {
  FaIconLibrary,
  FontAwesomeModule,
} from '@fortawesome/angular-fontawesome'; // Import FontAwesomeModule
import {
  faFacebookF,
  faTwitter,
  faInstagram,
  faLinkedinIn,
} from '@fortawesome/free-brands-svg-icons';
import {
  faMapMarkerAlt,
  faPhone,
  faEnvelope,
} from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'jhi-footer',
  standalone: true,
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.scss'],
  imports: [RouterModule, FontAwesomeModule], // Thêm FontAwesomeModule vào imports
})
export default class FooterComponent implements OnInit {
  currentYear: number = new Date().getFullYear();

  private readonly iconLibrary = inject(FaIconLibrary);

  ngOnInit(): void {
    this.iconLibrary.addIcons(
      faFacebookF,
      faTwitter,
      faInstagram,
      faLinkedinIn,
      faMapMarkerAlt,
      faPhone,
      faEnvelope,
    );
  }
}
