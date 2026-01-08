import { Component, ChangeDetectorRef, AfterViewInit } from '@angular/core';
import { ThemeOptions } from '../theme-options';
import { animate, query, style, transition, trigger } from '@angular/animations';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-base-layout',
  templateUrl: './base-layout.component.html',
  styleUrls: ['./base-layout.component.scss'],
  animations: [
    trigger('architectUIAnimation', [
      transition('* <=> *', [
        query(':enter, :leave', [
          style({
            opacity: 0,
            display: 'flex',
            flex: '1',
            transform: 'translateY(-20px)',
            flexDirection: 'column'
          }),
        ], { optional: true }),
        query(':enter', [
          animate('100ms ease', style({ opacity: 1, transform: 'translateY(0)' })),
        ], { optional: true }),
        query(':leave', [
          animate('100ms ease', style({ opacity: 0, transform: 'translateY(-20px)' })),
        ], { optional: true })
      ]),
    ])
  ]
})
export class BaseLayoutComponent implements AfterViewInit {

  config$ = this.globals.config$;

  constructor(public globals: ThemeOptions, private cdr: ChangeDetectorRef) {
  }

  ngAfterViewInit() {
    // Prevent ExpressionChangedAfterItHasBeenCheckedError by triggering change detection
    this.cdr.detectChanges();
  }

  getRouteAnimation(outlet: RouterOutlet) {
    return outlet && outlet.activatedRouteData && outlet.activatedRouteData['animation'];
  }

  toggleSidebarMobile() {
    this.globals.toggleSidebarMobile = !this.globals.toggleSidebarMobile;
  }
}
