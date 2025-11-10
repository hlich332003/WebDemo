import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { NotificationService } from 'app/shared/notification/notification.service';
import { FormsModule } from '@angular/forms'; // Import FormsModule

@Component({
  selector: 'jhi-import',
  standalone: true,
  imports: [CommonModule, FormsModule], // Thêm FormsModule
  templateUrl: './import.component.html',
  styleUrls: ['./import.component.scss'],
})
export class ImportComponent {
  private http = inject(HttpClient);
  private notify = inject(NotificationService);

  isImportingProducts = signal(false);
  isImportingUsers = signal(false);
  isImportingProductsFromUrl = signal(false); // Trạng thái loading cho URL
  isImportingUsersFromUrl = signal(false); // Trạng thái loading cho URL

  productImportUrl: string = ''; // Biến để lưu URL sản phẩm
  userImportUrl: string = ''; // Biến để lưu URL người dùng

  onFileSelected(event: Event, type: 'products' | 'users'): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      const formData = new FormData();
      formData.append('file', file, file.name);

      if (type === 'products') {
        this.isImportingProducts.set(true);
      } else {
        this.isImportingUsers.set(true);
      }

      this.http.post(`/api/admin/import/${type}`, formData).subscribe({
        next: () => {
          this.notify.success(`Import ${type} thành công!`);
          if (type === 'products') {
            this.isImportingProducts.set(false);
          } else {
            this.isImportingUsers.set(false);
          }
          input.value = ''; // Clear file input
        },
        error: (error: HttpErrorResponse) => {
          const errorMessage = error.error?.detail || error.message || `Import ${type} thất bại.`;
          this.notify.error(errorMessage);
          if (type === 'products') {
            this.isImportingProducts.set(false);
          } else {
            this.isImportingUsers.set(false);
          }
          input.value = ''; // Clear file input
        },
      });
    }
  }

  onUrlImport(type: 'products' | 'users'): void {
    let urlToImport: string;
    let loadingSignal: (value: boolean) => void;
    let endpoint: string;

    if (type === 'products') {
      urlToImport = this.productImportUrl;
      loadingSignal = this.isImportingProductsFromUrl.set;
      endpoint = '/api/admin/import/products-from-url';
    } else {
      urlToImport = this.userImportUrl;
      loadingSignal = this.isImportingUsersFromUrl.set;
      endpoint = '/api/admin/import/users-from-url';
    }

    if (!urlToImport) {
      this.notify.error('URL không được để trống!');
      return;
    }

    loadingSignal(true);
    this.http.post(endpoint, urlToImport, { headers: { 'Content-Type': 'text/plain' } }).subscribe({
      next: () => {
        this.notify.success(`Import ${type} từ URL thành công!`);
        loadingSignal(false);
        if (type === 'products') {
          this.productImportUrl = '';
        } else {
          this.userImportUrl = '';
        }
      },
      error: (error: HttpErrorResponse) => {
        const errorMessage = error.error?.detail || error.message || `Import ${type} từ URL thất bại.`;
        this.notify.error(errorMessage);
        loadingSignal(false);
      },
    });
  }
}
