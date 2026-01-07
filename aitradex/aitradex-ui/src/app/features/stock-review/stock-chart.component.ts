import { Component, Input, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import {
  ChartComponent,
  ApexAxisChartSeries,
  ApexChart,
  ApexXAxis,
  ApexYAxis,
  ApexTitleSubtitle,
  ApexTooltip,
  ApexTheme
} from 'ng-apexcharts';
import { StockQuoteHistory } from '../../core/models/stock-quote.model';

export type ChartOptions = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  xaxis: ApexXAxis;
  yaxis: ApexYAxis;
  title: ApexTitleSubtitle;
  tooltip: ApexTooltip;
  theme: ApexTheme;
};

@Component({
  selector: 'app-stock-chart',
  template: `
    <div class="stock-chart-container">
      <apx-chart
        [series]="chartOptions.series"
        [chart]="chartOptions.chart"
        [xaxis]="chartOptions.xaxis"
        [yaxis]="chartOptions.yaxis"
        [tooltip]="chartOptions.tooltip"
        [theme]="chartOptions.theme"
        *ngIf="data && data.length > 0"
      ></apx-chart>
      <div *ngIf="!data || data.length === 0" class="no-data">
        <p>No chart data available</p>
      </div>
    </div>
  `,
  styles: [`
    .stock-chart-container {
      width: 100%;
      height: 100%;
      min-height: 400px;
    }
    .no-data {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 400px;
      color: #999;
    }
  `]
})
export class StockChartComponent implements OnInit, OnChanges {
  @Input() data: StockQuoteHistory[] = [];
  @Input() symbol: string = '';
  @Input() chartType: 'line' | 'candlestick' = 'line';
  
  public chartOptions: Partial<ChartOptions> = {
    series: [],
    chart: {
      type: 'line',
      height: 400,
      toolbar: {
        show: true
      },
      zoom: {
        enabled: true
      }
    },
    xaxis: {
      type: 'datetime'
    },
    yaxis: {
      title: {
        text: 'Price'
      }
    },
    tooltip: {
      shared: true,
      intersect: false
    },
    theme: {
      mode: 'light'
    }
  };

  ngOnInit() {
    if (this.data && this.data.length > 0) {
      this.updateChart();
    }
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['data'] || changes['chartType'] || changes['symbol']) {
      this.updateChart();
    }
  }

  private updateChart() {
    if (!this.data || this.data.length === 0) {
      return;
    }

    const chartData = this.prepareChartData();
    const chartType = this.chartType === 'candlestick' ? 'candlestick' : 'line';

    this.chartOptions = {
      ...this.chartOptions,
      series: [{
        name: this.symbol,
        data: chartData as any
      }],
      chart: {
        ...this.chartOptions.chart,
        type: chartType as any
      }
    };
  }

  private prepareChartData(): any[] {
    if (this.chartType === 'candlestick' && this.data[0]?.open !== null) {
      return this.data
        .filter(d => d.open !== null && d.high !== null && d.low !== null && d.close !== null)
        .map(d => ({
          x: new Date(d.timestamp).getTime(),
          y: [d.open, d.high, d.low, d.close].map(v => v || 0)
        }));
    } else {
      return this.data
        .filter(d => d.close !== null)
        .map(d => ({
          x: new Date(d.timestamp).getTime(),
          y: d.close || 0
        }));
    }
  }
}
