import { Component } from '@angular/core';
import { AuditService } from '../../core/services/audit.service';
import { AuditLog } from '../../core/models/audit-log.model';

@Component({
  selector: 'app-audit',
  templateUrl: './audit.component.html',
  styleUrls: ['./audit.component.css']
})
export class AuditComponent {
  logs: AuditLog[] = [];
  actor = '';
  entityRef = '';
  error = '';

  constructor(private auditService: AuditService) {}

  load(): void {
    this.auditService.list(this.actor || undefined, this.entityRef || undefined).subscribe({
      next: (logs) => (this.logs = logs),
      error: () => (this.error = 'Unable to load audit logs.')
    });
  }
}
