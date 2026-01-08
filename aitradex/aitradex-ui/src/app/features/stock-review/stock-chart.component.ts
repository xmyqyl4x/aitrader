import { Component, Input, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import {
  ChartComponent,
  ApexAxisChartSeries,
  ApexChart,
  ApexXAxis,
  ApexYAxis,
  ApexTitleSubtitle,
  ApexTooltip,
  ApexTheme,
  ApexStroke,
  ApexFill,
  ApexDataLabels,
  ApexPlotOptions
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
  stroke?: ApexStroke;
  fill?: ApexFill;
  dataLabels?: ApexDataLabels;
  plotOptions?: ApexPlotOptions | any;
};

export type ChartType = 'line' | 'area' | 'bar' | 'candlestick' | 'ohlc';

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
        [stroke]="chartOptions.stroke"
        [fill]="chartOptions.fill"
        [dataLabels]="chartOptions.dataLabels"
        [plotOptions]="chartOptions.plotOptions"
        *ngIf="data && data.length > 0"
      ></apx-chart>
      <div *ngIf="!data || data.length === 0" class="no-data">
        <i class="pe-7s-graph1" style="font-size: 48px; color: #ccc;"></i>
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
      flex-direction: column;
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
  @Input() chartType: ChartType = 'line';
  
  public chartOptions: Partial<ChartOptions> = {
    series: [],
    chart: {
      type: 'line',
      height: 450,
      toolbar: {
        show: true,
        tools: {
          zoom: true,
          zoomin: true,
          zoomout: true,
          pan: true,
          reset: true,
          download: true
        }
      },
      zoom: {
        enabled: true,
        type: 'x',
        autoScaleYaxis: true
      }
    },
    xaxis: {
      type: 'datetime',
      labels: {
        datetimeFormatter: {
          year: 'yyyy',
          month: 'MMM \'yy',
          day: 'dd MMM',
          hour: 'HH:mm'
        }
      }
    },
    yaxis: {
      title: {
        text: 'Price ($)'
      },
      labels: {
        formatter: (value: number) => '$' + value.toFixed(2)
      }
    },
    tooltip: {
      shared: true,
      intersect: false,
      x: {
        format: 'dd MMM yyyy HH:mm'
      },
      y: {
        formatter: (value: number) => '$' + value.toFixed(2)
      }
    },
    theme: {
      mode: 'light'
    },
    stroke: {
      curve: 'smooth',
      width: 2
    },
    fill: {
      type: 'gradient',
      gradient: {
        shadeIntensity: 1,
        opacityFrom: 0.7,
        opacityTo: 0.3,
        stops: [0, 90, 100]
      }
    },
    dataLabels: {
      enabled: false
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
    const chartConfig = this.getChartConfig();

    this.chartOptions = {
      ...this.chartOptions,
      ...chartConfig,
      series: [{
        name: this.symbol,
        data: chartData as any
      }]
    };
  }

  private getChartConfig(): Partial<ChartOptions> {
    const baseConfig: Partial<ChartOptions> = {
      chart: {
        ...this.chartOptions.chart,
        type: this.chartType as any
      }
    };

    switch (this.chartType) {
      case 'line':
        return {
          ...baseConfig,
          stroke: {
            curve: 'smooth',
            width: 2
          },
          fill: {
            type: 'solid',
            opacity: 0
          }
        };

      case 'area':
        return {
          ...baseConfig,
          stroke: {
            curve: 'smooth',
            width: 2
          },
          fill: {
            type: 'gradient',
            gradient: {
              shadeIntensity: 1,
              opacityFrom: 0.7,
              opacityTo: 0.3,
              stops: [0, 90, 100]
            }
          }
        };

      case 'bar':
        return {
          ...baseConfig,
          plotOptions: {
            bar: {
              columnWidth: '60%',
              dataLabels: {
                position: 'top'
              }
            }
          },
          dataLabels: {
            enabled: false
          },
          stroke: {
            show: false
          },
          fill: {
            type: 'solid',
            opacity: 1
          }
        };

      case 'candlestick':
      case 'ohlc':
        return {
          ...baseConfig,
          chart: {
            ...baseConfig.chart,
            type: this.chartType === 'candlestick' ? 'candlestick' : 'ohlc' as any
          },
          plotOptions: {
            candlestick: {
              colors: {
                upward: '#26a69a',
                downward: '#ef5350'
              }
            },
            ohlc: {
              colors: {
                upward: '#26a69a',
                downward: '#ef5350'
              }
            }
          }
        };

      default:
        return baseConfig;
    }
  }

  private prepareChartData(): any[] {
    if (!this.data || this.data.length === 0) {
      return [];
    }

    // For candlestick and OHLC, we need OHLC data
    if ((this.chartType === 'candlestick' || this.chartType === 'ohlc') && 
        this.data[0]?.open !== null) {
      return this.data
        .filter(d => d.open !== null && d.high !== null && d.low !== null && d.close !== null)
        .map(d => ({
          x: new Date(d.timestamp).getTime(),
          y: [d.open, d.high, d.low, d.close].map(v => v || 0)
        }));
    } 
    
    // For line, area, and bar charts, use close price
    return this.data
      .filter(d => d.close !== null)
      .map(d => ({
        x: new Date(d.timestamp).getTime(),
        y: d.close || 0
      }));
  }
}
