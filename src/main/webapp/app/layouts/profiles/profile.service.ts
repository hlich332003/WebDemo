import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, shareReplay } from 'rxjs/operators';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { InfoResponse, ProfileInfo } from './profile-info.model';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);

  private readonly infoUrl =
    this.applicationConfigService.getEndpointFor('management/info');
  private profileInfo$?: Observable<ProfileInfo>;

  getProfileInfo(): Observable<ProfileInfo> {
    if (this.profileInfo$) {
      return this.profileInfo$;
    }

    // Return default profile info without calling /management/info
    this.profileInfo$ = new Observable<ProfileInfo>((observer) => {
      observer.next({
        activeProfiles: ['dev'],
        inProduction: false,
        openAPIEnabled: false,
      });
      observer.complete();
    }).pipe(shareReplay());

    return this.profileInfo$;
  }
}
