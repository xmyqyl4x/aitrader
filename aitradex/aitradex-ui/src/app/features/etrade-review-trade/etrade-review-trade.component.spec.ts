import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { EtradeReviewTradeComponent } from './etrade-review-trade.component';
import { EtradeService } from '../../core/services/etrade.service';
import { environment } from '../../../environments/environment';

describe('EtradeReviewTradeComponent', () => {
  let component: EtradeReviewTradeComponent;
  let fixture: ComponentFixture<EtradeReviewTradeComponent>;
  let httpMock: HttpTestingController;
  let router: Router;
  let etradeService: EtradeService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [EtradeReviewTradeComponent],
      imports: [
        HttpClientTestingModule,
        RouterTestingModule.withRoutes([
          { path: 'etrade-review-trade', component: EtradeReviewTradeComponent },
          { path: 'etrade-review-trade/accounts/:accountIdKey', component: {} as any }
        ])
      ],
      providers: [EtradeService]
    }).compileComponents();

    fixture = TestBed.createComponent(EtradeReviewTradeComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    etradeService = TestBed.inject(EtradeService);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Landing Page', () => {
    it('should render navigation menu with all sections', () => {
      fixture.detectChanges();
      const compiled = fixture.nativeElement;
      
      expect(compiled.querySelector('.nav-tabs')).toBeTruthy();
      expect(compiled.textContent).toContain('Accounts');
      expect(compiled.textContent).toContain('Market');
      expect(compiled.textContent).toContain('Order');
      expect(compiled.textContent).toContain('Alerts');
      expect(compiled.textContent).toContain('Research and Analysis');
      expect(compiled.textContent).toContain('Algorithm');
      expect(compiled.textContent).toContain('Quant');
    });

    it('should show Link E*TRADE Account button when not connected', () => {
      component.oauthStatus = {
        connected: false,
        hasAccounts: false,
        accountCount: 0
      };
      fixture.detectChanges();
      
      const compiled = fixture.nativeElement;
      const linkButton = compiled.querySelector('button[class*="btn-primary"]');
      expect(linkButton).toBeTruthy();
      expect(linkButton.textContent).toContain('Link E*TRADE Account');
    });

    it('should display status banner with correct class for not connected', () => {
      component.oauthStatus = {
        connected: false,
        hasAccounts: false,
        accountCount: 0
      };
      fixture.detectChanges();
      
      const banner = fixture.nativeElement.querySelector('.alert');
      expect(banner).toBeTruthy();
      expect(banner.classList.contains('alert-warning')).toBe(true);
    });

    it('should display status banner with correct class for authorized', () => {
      component.oauthStatus = {
        connected: true,
        hasAccounts: true,
        accountCount: 1,
        tokenStatus: 'VALID'
      };
      fixture.detectChanges();
      
      const banner = fixture.nativeElement.querySelector('.alert');
      expect(banner.classList.contains('alert-success')).toBe(true);
    });
  });

  describe('Link E*TRADE Account Flow', () => {
    it('should call backend to load accounts when Link Account is clicked', () => {
      component.onLinkAccountClick();
      
      const req = httpMock.expectOne(`${environment.apiUrl}/api/etrade/accounts`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
      
      expect(component.activeSection).toBe('accounts');
    });

    it('should display accounts table when accounts are loaded', () => {
      const mockAccounts = [
        {
          id: '1',
          userId: 'user1',
          accountIdKey: 'key1',
          accountType: 'BROKERAGE',
          accountName: 'Test Account',
          accountStatus: 'ACTIVE',
          linkedAt: '2024-01-01',
          lastSyncedAt: '2024-01-02'
        }
      ];

      component.loadAccounts();
      
      const req = httpMock.expectOne(`${environment.apiUrl}/api/etrade/accounts`);
      req.flush(mockAccounts);
      
      fixture.detectChanges();
      
      expect(component.accounts.length).toBe(1);
      const compiled = fixture.nativeElement;
      expect(compiled.textContent).toContain('Test Account');
      expect(compiled.querySelector('table')).toBeTruthy();
    });

    it('should show empty state when no accounts exist', () => {
      component.loadAccounts();
      
      const req = httpMock.expectOne(`${environment.apiUrl}/api/etrade/accounts`);
      req.flush([]);
      
      fixture.detectChanges();
      
      const compiled = fixture.nativeElement;
      expect(compiled.textContent).toContain('No E*TRADE Accounts Linked');
    });
  });

  describe('Connect Account Workflow', () => {
    it('should check token status and validate when Connect Account is clicked', () => {
      const mockAccount = {
        id: '1',
        userId: 'user1',
        accountIdKey: 'key1',
        accountType: 'BROKERAGE',
        accountName: 'Test Account',
        accountStatus: 'ACTIVE',
        linkedAt: '2024-01-01',
        lastSyncedAt: null
      };

      component.accounts = [mockAccount];
      fixture.detectChanges();

      spyOn(router, 'navigate');
      component.connectAccount(mockAccount);

      // Should call syncAccounts which validates token
      const req = httpMock.expectOne(`${environment.apiUrl}/api/etrade/accounts/sync?accountId=1`);
      expect(req.request.method).toBe('POST');
      req.flush([mockAccount]);
    });

    it('should initiate OAuth flow when token is invalid', () => {
      const mockAccount = {
        id: '1',
        userId: 'user1',
        accountIdKey: 'key1',
        accountType: 'BROKERAGE',
        accountName: 'Test Account',
        accountStatus: 'ACTIVE',
        linkedAt: '2024-01-01',
        lastSyncedAt: null
      };

      component.accounts = [mockAccount];
      fixture.detectChanges();

      spyOn(window, 'location', 'get').and.returnValue({ href: '' } as Location);
      Object.defineProperty(window, 'location', {
        writable: true,
        value: { href: '' }
      });

      component.connectAccount(mockAccount);

      // First call to syncAccounts fails with 401
      const syncReq = httpMock.expectOne(`${environment.apiUrl}/api/etrade/accounts/sync?accountId=1`);
      syncReq.flush({ error: 'Token expired' }, { status: 401, statusText: 'Unauthorized' });

      // Should then initiate OAuth
      const oauthReq = httpMock.expectOne(`${environment.apiUrl}/api/etrade/oauth/authorize`);
      expect(oauthReq.request.method).toBe('GET');
      oauthReq.flush({ authorizationUrl: 'https://etrade.com/authorize', state: 'test' });
    });

    it('should update UI statuses after successful connection', () => {
      const mockAccount = {
        id: '1',
        userId: 'user1',
        accountIdKey: 'key1',
        accountType: 'BROKERAGE',
        accountName: 'Test Account',
        accountStatus: 'ACTIVE',
        linkedAt: '2024-01-01',
        lastSyncedAt: null
      };

      component.accounts = [mockAccount];
      component.connectAccount(mockAccount);

      const req = httpMock.expectOne(`${environment.apiUrl}/api/etrade/accounts/sync?accountId=1`);
      req.flush([mockAccount]);

      expect(component.successMessage).toContain('connected successfully');
    });
  });

  describe('View Details', () => {
    it('should navigate to account details page', () => {
      const mockAccount = {
        id: '1',
        userId: 'user1',
        accountIdKey: 'key1',
        accountType: 'BROKERAGE',
        accountName: 'Test Account',
        accountStatus: 'ACTIVE',
        linkedAt: '2024-01-01',
        lastSyncedAt: null
      };

      spyOn(router, 'navigate');
      component.viewAccountDetails(mockAccount);

      expect(router.navigate).toHaveBeenCalledWith(['/etrade-review-trade/accounts', 'key1']);
    });
  });

  describe('Error Handling', () => {
    it('should display error message when token is invalid', () => {
      const mockAccount = {
        id: '1',
        userId: 'user1',
        accountIdKey: 'key1',
        accountType: 'BROKERAGE',
        accountName: 'Test Account',
        accountStatus: 'ACTIVE',
        linkedAt: '2024-01-01',
        lastSyncedAt: null
      };

      component.accounts = [mockAccount];
      component.connectAccount(mockAccount);

      const req = httpMock.expectOne(`${environment.apiUrl}/api/etrade/accounts/sync?accountId=1`);
      req.flush({ error: 'Token expired' }, { status: 401, statusText: 'Unauthorized' });

      // OAuth initiation should happen
      const oauthReq = httpMock.expectOne(`${environment.apiUrl}/api/etrade/oauth/authorize`);
      oauthReq.flush({ authorizationUrl: 'https://etrade.com/authorize', state: 'test' });
    });

    it('should display error message when account loading fails', () => {
      component.loadAccounts();

      const req = httpMock.expectOne(`${environment.apiUrl}/api/etrade/accounts`);
      req.flush({ error: 'Server error' }, { status: 500, statusText: 'Internal Server Error' });

      expect(component.error).toBeTruthy();
      expect(component.error).toContain('Failed to load accounts');
    });
  });

  describe('Navigation', () => {
    it('should set active section when navigation item is clicked', () => {
      component.setActiveSection('market');
      expect(component.activeSection).toBe('market');

      component.setActiveSection('order');
      expect(component.activeSection).toBe('order');
    });
  });
});
