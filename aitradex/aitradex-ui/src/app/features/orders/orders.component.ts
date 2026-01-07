import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { OrdersService } from '../../core/services/orders.service';
import { Order } from '../../core/models/order.model';

@Component({
  selector: 'app-orders',
  templateUrl: './orders.component.html',
  styleUrls: ['./orders.component.css']
})
export class OrdersComponent {
  orders: Order[] = [];
  error = '';

  form = this.fb.group({
    accountId: ['', Validators.required],
    symbol: ['', Validators.required],
    side: ['BUY', Validators.required],
    type: ['MARKET', Validators.required],
    source: ['MANUAL', Validators.required],
    quantity: [1, [Validators.required, Validators.min(0.0001)]],
    limitPrice: [''],
    stopPrice: [''],
    notes: ['']
  });

  constructor(private ordersService: OrdersService, private fb: FormBuilder) {}

  loadOrders(): void {
    const accountId = this.form.get('accountId')?.value || undefined;
    this.ordersService.list(accountId ?? undefined).subscribe({
      next: (orders) => (this.orders = orders),
      error: () => (this.error = 'Unable to load orders.')
    });
  }

  createOrder(): void {
    if (this.form.invalid) {
      this.error = 'Please fill in the required fields.';
      return;
    }
    this.error = '';
    this.ordersService.create(this.form.value).subscribe({
      next: (order) => {
        this.orders = [order, ...this.orders];
        this.form.patchValue({ symbol: '', quantity: 1, limitPrice: '', stopPrice: '', notes: '' });
      },
      error: () => (this.error = 'Unable to create order. Check API rules.')
    });
  }
}
