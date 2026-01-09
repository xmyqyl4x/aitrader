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
    
    // Mock the initial ngOnInit calls
    const oauthReq = httpMock.expectOne(`${environment.apiUrl}/etrade/oauth/status`);
    oauthReq.flush({ connected: false, hasAccounts: false, accountCount: 0 });
    const accountsReq = httpMock.expectOne(`${environment.apiUrl}/etrade/accounts`);
    accountsReq.flush([]);
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
      
      // Should make a new accounts request (in addition to the one from ngOnInit)
      const req = httpMock.expectOne(`${environment.apiUrl}/etrade/accounts`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
      
      // Then checkOAuthStatus is called
      const oauthReq = httpMock.expectOne(`${environment.apiUrl}/etrade/oauth/status`);
      oauthReq.flush({ connected: false, hasAccounts: false, accountCount: 0 });
      
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
      
      const req = httpMock.expectOne(`${environment.apiUrl}/etrade/accounts`);
      req.flush(mockAccounts);
      
      // Mock the OAuth status call from loadAccounts
      const oauthReq2 = httpMock.expectOne(`${environment.apiUrl}/etrade/oauth/status`);
      oauthReq2.flush({ connected: true, hasAccounts: true, accountCount: 1 });
      
      fixture.detectChanges();
      
      expect(component.accounts.length).toBe(1);
      const compiled = fixture.nativeElement;
      expect(compiled.textContent).toContain('Test Account');
      expect(compiled.querySelector('table')).toBeTruthy();
    });

    it('should show empty state when no accounts exist', () => {
      component.loadAccounts();
      
      const req = httpMock.expectOne(`${environment.apiUrl}/etrade/accounts`);
      req.flush([]);
      
      // Mock the OAuth status call from loadAccounts
      const oauthReq2 = httpMock.expectOne(`${environment.apiUrl}/etrade/oauth/status`);
      oauthReq2.flush({ connected: false, hasAccounts: false, accountCount: 0 });
      
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
      const req = httpMock.expectOne(`${environment.apiUrl}/etrade/accounts/sync?accountId=1`);
      expect(req.request.method).toBe('POST');
      req.flush([mockAccount]);
      
      // Mock the loadAccounts call
      const accountsReq = httpMock.expectOne(`${environment.apiUrl}/etrade/accounts`);
      accountsReq.flush([mockAccount]);
      
      // Mock the OAuth status call from loadAccounts
      const oauthReq2 = httpMock.expectOne(`${environment.apiUrl}/etrade/oauth/status`);
      oauthReq2.flush({ connected: true, hasAccounts: true, accountCount: 1 });
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

      // Mock window.location.href assignment
      const originalLocation = window.location;
      delete (window as any).location;
      (window as any).location = { ...originalLocation, href: '' };

      component.connectAccount(mockAccount);

      // First call to syncAccounts fails with 401
      const syncReq = httpMock.expectOne(`${environment.apiUrl}/etrade/accounts/sync?accountId=1`);
      syncReq.flush({ error: 'Token expired' }, { status: 401, statusText: 'Unauthorized' });

      // Should then initiate OAuth
      const oauthReq2 = httpMock.expectOne(`${environment.apiUrl}/etrade/oauth/authorize`);
      expect(oauthReq2.request.method).toBe('GET');
      oauthReq2.flush({ authorizationUrl: 'https://etrade.com/authorize', state: 'test' });
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

      const req = httpMock.expectOne(`${environment.apiUrl}/etrade/accounts/sync?accountId=1`);
      req.flush([mockAccount]);
      
      // Mock the loadAccounts call
      const accountsReq = httpMock.expectOne(`${environment.apiUrl}/etrade/accounts`);
      accountsReq.flush([mockAccount]);
      
      // Mock the OAuth status call from loadAccounts
      const oauthReq2 = httpMock.expectOne(`${environment.apiUrl}/etrade/oauth/status`);
      oauthReq2.flush({ connected: true, hasAccounts: true, accountCount: 1 });

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

      const req = httpMock.expectOne(`${environment.apiUrl}/etrade/accounts/sync?accountId=1`);
      req.flush({ error: 'Token expired' }, { status: 401, statusText: 'Unauthorized' });

      // OAuth initiation should happen
      const oauthReq2 = httpMock.expectOne(`${environment.apiUrl}/etrade/oauth/authorize`);
      oauthReq2.flush({ authorizationUrl: 'https://etrade.com/authorize', state: 'test' });
      
      expect(component.oauthFlowActive).toBe(true);
    });

    it('should display error message when account loading fails', () => {
      component.loadAccounts();

      // This is the second accounts call (first was in ngOnInit)
      const req = httpMock.expectOne(`${environment.apiUrl}/etrade/accounts`);
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
