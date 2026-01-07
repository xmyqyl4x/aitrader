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

  save(): void {
    if (!this.token) {
      this.message = 'Token cleared.';
      this.authService.clearToken();
      return;
    }
    this.authService.setToken(this.token);
    this.message = 'Token saved.';
  }
}
