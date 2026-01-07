import { Component } from '@angular/core';
import { UploadsService } from '../../core/services/uploads.service';
import { Upload } from '../../core/models/upload.model';

@Component({
  selector: 'app-uploads',
  templateUrl: './uploads.component.html',
  styleUrls: ['./uploads.component.css']
})
export class UploadsComponent {
  uploads: Upload[] = [];
  userId = '';
  type = 'CSV';
  file?: File;
  error = '';

  constructor(private uploadsService: UploadsService) {}

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.file = input.files ? input.files[0] : undefined;
  }

  upload(): void {
    if (!this.userId || !this.file) {
      this.error = 'User ID and file are required.';
      return;
    }
    this.error = '';
    this.uploadsService.uploadFile(this.userId, this.type, this.file).subscribe({
      next: (upload) => (this.uploads = [upload, ...this.uploads]),
      error: () => (this.error = 'Unable to upload file.')
    });
  }

  validate(upload: Upload): void {
    this.uploadsService.validate(upload.id).subscribe({
      next: (updated) => {
        this.uploads = this.uploads.map((item) => (item.id === updated.id ? updated : item));
      },
      error: () => (this.error = 'Unable to validate upload.')
    });
  }

  loadUploads(): void {
    this.uploadsService.list(this.userId || undefined).subscribe({
      next: (uploads) => (this.uploads = uploads),
      error: () => (this.error = 'Unable to load uploads.')
    });
  }
}
