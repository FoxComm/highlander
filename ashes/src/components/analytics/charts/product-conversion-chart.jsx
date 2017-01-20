// @flow

// libs
import React from 'react';
import _ from 'lodash';

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

// Dummy response payload for UI debugging
const dummyJsonData = {
  'SearchViews': 876,
  'PdpViews': 500,
  'CartClicks': 379,
  'CheckoutClicks': 263,
  'Purchases': 68,
  'SearchToPdp': 57.1,
  'PdpToCart': 75.8,
  'CartToCheckout': 69.4,
  'CheckoutToPurchase': 25.6,
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

class ProductConversionChart extends React.Component {

  props: Props;

  static defaultProps = {
    jsonData: {},
    debugMode: false,
  };

  get data() {
    const { jsonData, debugMode } = this.props;
    
    const jsonDisplay = (debugMode) ? dummyJsonData : jsonData;

    const deltaDisplay = (deltaStr) => {
      return _.round(parseFloat(deltaStr) * 100, 2);
    };

    return [
      {
        key: dataTickValues[0],
        value: jsonDisplay.SearchViews,
        delta: null,
        label: jsonDisplay.SearchViews.toString(),
      },
      {
        key: dataTickValues[1],
        value: jsonDisplay.PdpViews,
        delta: deltaDisplay(jsonDisplay.SearchToPdp),
        label: jsonDisplay.PdpViews.toString(),
      },
      {
        key: dataTickValues[2],
        value: jsonDisplay.CartClicks,
        delta: deltaDisplay(jsonDisplay.PdpToCart),
        label: jsonDisplay.CartClicks.toString(),
      },
      {
        key: dataTickValues[3],
        value: jsonDisplay.CheckoutClicks,
        delta: deltaDisplay(jsonDisplay.CartToCheckout),
        label: jsonDisplay.CheckoutClicks.toString(),
      },
      {
        key: dataTickValues[4],
        value: jsonDisplay.Purchases,
        delta: deltaDisplay(jsonDisplay.CheckoutToPurchase),
        label: jsonDisplay.Purchases.toString(),
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
          orientation="bottom"
          tickValues={dataTickValues} />
        <VictoryAxis
          dependentAxis
          standalone={false}
          style={yAxisStyle}
          tickCount={3}
          orientation="left" />
        <VictoryBar
          labelComponent={
            <ProductConversionToolTip
              barWidth={barStyle.data.width}
              getDelta={(datum) => datum.delta}
            />
          }
          style={barStyle}
          data={this.data}
          x="key"
          y="value"
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
