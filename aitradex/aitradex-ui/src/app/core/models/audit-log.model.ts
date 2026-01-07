export interface AuditLog {
  id: string;
  actor: string;
  actorType: string;
  action: string;
  entityRef: string;
  beforeState?: string;
  afterState?: string;
  occurredAt: string;
}
