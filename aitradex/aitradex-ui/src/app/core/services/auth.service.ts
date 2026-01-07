import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly storageKey = 'aitradex.jwt';

  getToken(): string | null {
    return localStorage.getItem(this.storageKey);
  }

  setToken(token: string): void {
    localStorage.setItem(this.storageKey, token);
  }

  clearToken(): void {
    localStorage.removeItem(this.storageKey);
  }
}
