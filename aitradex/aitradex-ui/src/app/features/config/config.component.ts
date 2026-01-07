import { Component } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-config',
  templateUrl: './config.component.html',
  styleUrls: ['./config.component.css']
})
export class ConfigComponent {
  token = '';
  message = '';

  constructor(private authService: AuthService) {
    this.token = this.authService.getToken() ?? '';
  }

  get hasToken(): boolean {
    return this.token.trim().length > 0;
  }

  get tokenLength(): number {
    return this.token.trim().length;
  }

  onTokenChange(): void {
    this.message = '';
  }

  save(): void {
    const trimmedToken = this.token.trim();
    if (!trimmedToken) {
      this.clear();
      return;
    }
    this.token = trimmedToken;
    this.authService.setToken(trimmedToken);
    this.message = 'Token saved locally.';
  }

  clear(): void {
    this.token = '';
    this.authService.clearToken();
    this.message = 'Token cleared locally.';
  }
}
