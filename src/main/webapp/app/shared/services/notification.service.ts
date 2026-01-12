import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { INotification } from 'app/shared/model/notification.model';
import { ApplicationConfigService } from 'app/core/config/application-config.service';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private http = inject(HttpClient);
  private applicationConfigService = inject(ApplicationConfigService);
  private resourceUrl = this.applicationConfigService.getEndpointFor('api/notifications');

  query(): Observable<INotification[]> {
    return this.http.get<INotification[]>(this.resourceUrl);
  }

  getUnreadCount(): Observable<number> {
    return this.http.get<number>(`${this.resourceUrl}/unread-count`);
  }

  markAsRead(id: number): Observable<Record<string, never>> {
    return this.http.post<Record<string, never>>(`${this.resourceUrl}/${id}/mark-as-read`, {});
  }

  markAllAsRead(): Observable<Record<string, never>> {
    return this.http.post<Record<string, never>>(`${this.resourceUrl}/mark-all-as-read`, {});
  }

  deleteReadNotifications(): Observable<Record<string, never>> {
    return this.http.delete<Record<string, never>>(`${this.resourceUrl}/delete-read`);
  }

  deleteAll(): Observable<Record<string, never>> {
    return this.http.delete<Record<string, never>>(`${this.resourceUrl}/delete-all`);
  }

  delete(id: number): Observable<Record<string, never>> {
    return this.http.delete<Record<string, never>>(`${this.resourceUrl}/${id}`);
  }
}
