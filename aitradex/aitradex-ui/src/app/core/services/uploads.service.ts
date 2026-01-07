import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Upload } from '../models/upload.model';

@Injectable({ providedIn: 'root' })
export class UploadsService {
  private readonly baseUrl = `${environment.apiUrl}/uploads`;

  constructor(private http: HttpClient) {}

  list(userId?: string, status?: string): Observable<Upload[]> {
    let params = new HttpParams();
    if (userId) {
      params = params.set('userId', userId);
    }
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<Upload[]>(this.baseUrl, { params });
  }

  uploadFile(userId: string, type: string, file: File): Observable<Upload> {
    const formData = new FormData();
    formData.append('userId', userId);
    formData.append('type', type);
    formData.append('file', file);
    return this.http.post<Upload>(`${this.baseUrl}/file`, formData);
  }

  validate(id: string): Observable<Upload> {
    return this.http.post<Upload>(`${this.baseUrl}/${id}/validate`, {});
  }
}
