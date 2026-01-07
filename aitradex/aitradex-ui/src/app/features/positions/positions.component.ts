import { Component } from '@angular/core';
import { PositionsService } from '../../core/services/positions.service';
import { Position } from '../../core/models/position.model';

@Component({
  selector: 'app-positions',
  templateUrl: './positions.component.html',
  styleUrls: ['./positions.component.css']
})
export class PositionsComponent {
  positions: Position[] = [];
  accountId = '';
  openOnly = true;
  error = '';

  constructor(private positionsService: PositionsService) {}

  loadPositions(): void {
    this.positionsService.list(this.accountId || undefined, this.openOnly).subscribe({
      next: (positions) => (this.positions = positions),
      error: () => (this.error = 'Unable to load positions.')
    });
  }
}
