import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface ThemeConfig {
  headerTheme: string;
  sidebarTheme: string;
}

@Injectable({
  providedIn: 'root'
})
export class ThemeOptions {
  sidebarHover = false;
  toggleSidebar = false;
  toggleSidebarMobile = false;
  toggleHeaderMobile = false;
  toggleThemeOptions = false;
  toggleDrawer = false;
  toggleFixedFooter = false;

  // Theme configuration (migrated from Redux store)
  private configSubject = new BehaviorSubject<ThemeConfig>({
    headerTheme: 'bg-primary',
    sidebarTheme: 'bg-dark'
  });

  config$: Observable<ThemeConfig> = this.configSubject.asObservable();

  updateConfig(config: Partial<ThemeConfig>): void {
    this.configSubject.next({
      ...this.configSubject.value,
      ...config
    });
  }

  getConfig(): ThemeConfig {
    return this.configSubject.value;
  }
}
