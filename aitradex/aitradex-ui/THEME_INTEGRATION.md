# ArchitectUI Theme Integration

This document describes how the ArchitectUI Angular Pro theme has been integrated into the `aitradex-ui` application.

## Overview

The application uses **ArchitectUI Angular Pro** as its base theme and dashboard framework. The theme provides:
- Professional layout with sidebar navigation and header
- Bootstrap 5-based styling and components
- Responsive design with mobile support
- Comprehensive component library
- SCSS-based theming system

## Architecture

### Layout Structure

The application uses a layout-based architecture with the following structure:

```
app.component
  └── BaseLayoutComponent (ArchitectUI shell)
       ├── HeaderComponent
       ├── SidebarComponent
       ├── FooterComponent
       └── Router Outlet (for page content)
```

### Key Components

1. **BaseLayoutComponent** (`src/app/layout/base-layout.component.ts`)
   - Main layout wrapper that provides the ArchitectUI shell
   - Handles sidebar/header toggle states
   - Manages route animations

2. **SidebarComponent** (`src/app/layout/components/sidebar/`)
   - Navigation sidebar with menu items
   - Responsive behavior (collapses on mobile)
   - Theme-aware styling

3. **HeaderComponent** (`src/app/layout/components/header/`)
   - Top navigation bar
   - User information display
   - Mobile menu toggle

4. **FooterComponent** (`src/app/layout/components/footer/`)
   - Application footer
   - Copyright and version information

### Theme Configuration

Theme state is managed via the `ThemeOptions` service (`src/app/theme-options.ts`):

```typescript
import { ThemeOptions } from './theme-options';

constructor(public globals: ThemeOptions) {}

// Subscribe to theme changes
this.globals.config$.subscribe(config => {
  // Handle theme updates
});

// Update theme
this.globals.updateConfig({ 
  headerTheme: 'bg-primary', 
  sidebarTheme: 'bg-dark' 
});
```

## Assets Location

All ArchitectUI theme assets are located in:
```
src/assets/architectui/
├── components/      # Component-specific styles
├── layout/          # Layout styles
├── themes/          # Theme variables
├── images/          # Images and icons
├── elements/        # UI elements
├── widgets/         # Widget components
└── base.scss        # Main stylesheet entry point
```

## Styling

### SCSS Structure

The main stylesheet (`src/styles.scss`) imports the ArchitectUI base:

```scss
@import "~assets/architectui/base";
```

This imports:
- Bootstrap 5 framework
- ArchitectUI theme variables
- Layout styles
- Component styles
- Widget styles
- Responsive utilities

### Customization

To customize theme colors and variables, modify files in:
- `src/assets/architectui/themes/layout-variables.scss` - Theme color variables
- `src/assets/architectui/components/bootstrap5/variables-override.scss` - Bootstrap overrides

### Component Styles

Component-specific SCSS files can be added alongside component TypeScript files:
```typescript
@Component({
  selector: 'app-my-component',
  templateUrl: './my-component.html',
  styleUrls: ['./my-component.scss']  // Custom styles
})
```

## Dependencies

The theme integration requires the following npm packages:

### Core Theme Dependencies
- `bootstrap@^5.3.3` - Bootstrap 5 CSS framework
- `@ng-bootstrap/ng-bootstrap@^17.0.0` - Angular Bootstrap components
- `@fortawesome/angular-fontawesome@^0.15.0` - FontAwesome icons
- `@fortawesome/fontawesome-svg-core@^6.5.1` - FontAwesome core
- `@fortawesome/free-solid-svg-icons@^6.5.1` - FontAwesome icons pack

### UI Utilities
- `ngx-scrollbar@^12.0.1` - Custom scrollbar component
- `@ngx-loading-bar/router@^6.0.1` - Loading bar for route changes
- `ngx-toastr@^18.0.0` - Toast notifications

### Build Tools
- `sass@^1.77.0` - SCSS compiler

## Angular Configuration

### angular.json

The build configuration includes:

```json
{
  "styles": ["src/styles.scss"],
  "scripts": [
    "node_modules/apexcharts/dist/apexcharts.min.js"
  ],
  "assets": ["src/favicon.ico", "src/assets"]
}
```

### Module Imports

The `AppModule` includes necessary theme modules:

```typescript
imports: [
  BrowserAnimationsModule,  // Required for animations
  FontAwesomeModule,        // FontAwesome icons
  NgbModule,                // ng-bootstrap components
  NgScrollbarModule,        // Custom scrollbars
  LoadingBarRouterModule    // Route loading indicator
]
```

## Routing

All routes are configured in `app-routing.module.ts`. The default route redirects to `/dashboard`:

```typescript
const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent },
  // ... other routes
];
```

All routes render within the `BaseLayoutComponent` (via router-outlet).

## Building and Running

### Development

```bash
cd aitradex-ui
npm install --legacy-peer-deps  # Install dependencies
ng serve                        # Start dev server
```

The application will be available at `http://localhost:4200`.

### Production Build

```bash
ng build --configuration=production
```

Output will be in `dist/aitradex-ui/`.

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## Troubleshooting

### SCSS Import Errors

If you see errors about missing SCSS files:
1. Verify assets are in `src/assets/architectui/`
2. Check that `sass` is installed: `npm install --save-dev sass`
3. Ensure `angular.json` uses `src/styles.scss` (not `.css`)

### Missing Icons/Fonts

1. Verify FontAwesome is installed and imported in `app.module.ts`
2. Check browser console for 404 errors on font files
3. Ensure assets are included in `angular.json` assets array

### Layout Not Rendering

1. Verify `BaseLayoutComponent` is declared in `AppModule`
2. Check that `app.component.html` contains `<app-base-layout></app-base-layout>`
3. Ensure `BrowserAnimationsModule` is imported

### Theme Not Applying

1. Verify `ThemeOptions` service is provided (default is `providedIn: 'root'`)
2. Check that SCSS files compile without errors
3. Inspect browser DevTools to see if CSS classes are applied

## Future Enhancements

Potential areas for expansion:
- Additional dashboard widgets and components
- Custom theme color schemes
- More navigation menu items with accordions
- User profile and settings pages
- Notification system integration
- Search functionality in header

## References

- [ArchitectUI Documentation](https://dashboardpack.com/)
- [Bootstrap 5 Documentation](https://getbootstrap.com/docs/5.3/)
- [ng-bootstrap Documentation](https://ng-bootstrap.github.io/)
- [FontAwesome Icons](https://fontawesome.com/icons)
