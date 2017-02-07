// @flow

// libs
import React from 'react';
import _ from 'lodash';

// funcs
import { percentDifferenceFromAvg } from '../analytics';

// components
import {
  VictoryBar,
  VictoryChart,
  VictoryAxis,
} from 'victory';
import ProductConversionToolTip from './product-conversion-tooltip';

const axisTickColor = '#9BA3A7';
const yAxisStyle = {
  axis: { stroke: axisTickColor },
  grid: { stroke: axisTickColor, strokeWidth: 0.25, strokeDasharray: 2.5, opacity: 0.6 },
  tickLabels: { fontSize: 8, fill: axisTickColor },
};
const xAxisStyle = {
  axis: { stroke: axisTickColor },
  tickLabels: { fontSize: 6, fill: axisTickColor },
};
const barStyle = {
  data: { fill: '#4FC2C9', width: 15 },
  labels: { fontSize: 12 },
  parent: { border: '1px solid #ccc' },
};

const dataTickValues = [
  'Category/Search View',
  'PDP View',
  'Add to Cart',
  'Enter Checkout',
  'Purchased',
];

// Debug response payload for UI testing
const debugJsonData = {
  'SearchViews': 7446,
  'PdpViews': 236,
  'CartClicks': 84,
  'CheckoutClicks': 224,
  'Purchases': 6,
  'SearchToPdp': 0.0316948697287134,
  'PdpToCart': 0.3559322033898305,
  'CartToCheckout': 2.6666666666666665,
  'CheckoutPurchased': 0.026785714285714284,
  'Average': {
    'SearchViews': 89665,
    'PdpViews': 2649,
    'CartClicks': 967,
    'CheckoutClicks': 1836,
    'Purchases': 24,
    'SearchToPdp': 0.02954330006133943,
    'PdpToCart': 0.3650434126085315,
    'CartToCheckout': 1.8986556359875906,
    'CheckoutPurchased': 0.013071895424836602
  }
};

const barEvents = [
  {
    // Event: Always show tooltip label onMouseOut
    target: 'data',
    eventHandlers: {
      onMouseOut: () => {
        return [{
          target: 'labels',
          mutation: (props) => {
            return props.label;
          }
        }];
      }
    }
  }
];

type Props = {
  jsonData: Object,
  debugMode?: ?boolean,
}

type ProductConversionChartBar = {
  key: string,
  value: number,
  delta: ?number,
  label: string,
  conversion: ?number,
}

class ProductConversionChart extends React.Component {

  props: Props;

  static defaultProps = {
    jsonData: {},
    debugMode: false,
  };

  get data(): Array<ProductConversionChartBar> {
    const { jsonData, debugMode } = this.props;
    
    const jsonDisplay = (debugMode) ? debugJsonData : jsonData;

    const deltaDisplay = (deltaStr: number): number => {
      return _.round(parseFloat(deltaStr) * 100, 2);
    };

    return [
      {
        key: dataTickValues[0],
        value: jsonDisplay.SearchViews,
        delta: null,
        label: jsonDisplay.SearchViews.toString(),
        conversion: null,
      },
      {
        key: dataTickValues[1],
        value: jsonDisplay.PdpViews,
        delta: deltaDisplay(jsonDisplay.SearchToPdp),
        label: jsonDisplay.PdpViews.toString(),
        conversion: percentDifferenceFromAvg(
          jsonDisplay.SearchToPdp, jsonDisplay.Average.SearchToPdp
        ),
      },
      {
        key: dataTickValues[2],
        value: jsonDisplay.CartClicks,
        delta: deltaDisplay(jsonDisplay.PdpToCart),
        label: jsonDisplay.CartClicks.toString(),
        conversion: percentDifferenceFromAvg(
          jsonDisplay.PdpToCart, jsonDisplay.Average.PdpToCart
        ),
      },
      {
        key: dataTickValues[3],
        value: jsonDisplay.CheckoutClicks,
        delta: deltaDisplay(jsonDisplay.CartToCheckout),
        label: jsonDisplay.CheckoutClicks.toString(),
        conversion: percentDifferenceFromAvg(
          jsonDisplay.CartToCheckout, jsonDisplay.Average.CartToCheckout
        ),
      },
      {
        key: dataTickValues[4],
        value: jsonDisplay.Purchases,
        delta: deltaDisplay(jsonDisplay.CheckoutPurchased),
        label: jsonDisplay.Purchases.toString(),
        conversion: percentDifferenceFromAvg(
          jsonDisplay.CheckoutPurchased, jsonDisplay.Average.CheckoutPurchased
        ),
      },
    ];
  }

  get chart() {
    return (
      <VictoryChart
        domainPadding={25}>
        <VictoryAxis
          standalone={false}
          style={xAxisStyle}
          orientation='bottom'
          tickValues={dataTickValues} />
        <VictoryAxis
          dependentAxis
          standalone={false}
          style={yAxisStyle}
          tickCount={3}
          orientation='left' />
        <VictoryBar
          labelComponent={
            <ProductConversionToolTip
              barWidth={barStyle.data.width}
              getDelta={(datum) => datum.delta}
              getConversion={(datum) => datum.conversion}
            />
          }
          style={barStyle}
          data={this.data}
          x='key'
          y='value'
          events={barEvents}
        />
      </VictoryChart>
    );
  }

  render() {
    return this.chart;
  }
}

export default ProductConversionChart;
