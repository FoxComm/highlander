// @flow

// libs
import React from 'react';
import _ from 'lodash';
import { autobind } from 'core-decorators';

// funcs
import { percentDifferenceFromAvg } from '../analytics';

// funcs
import { percentDifferenceFromAvg } from '../analytics';

// components
import {
  VictoryBar,
  VictoryChart,
  VictoryGroup,
  VictoryAxis,
} from 'victory';
import ProductConversionToolTip from './product-conversion-tooltip';

// styles
const axisTickColor = '#9BA3A7';
const gridLineColor = '#ABB2B6';

const yAxisStyle = {
  axis: { stroke: axisTickColor },
  grid: { stroke: gridLineColor, strokeWidth: 0.25, strokeDasharray: 2.5 },
  tickLabels: { fontSize: 8, fill: axisTickColor, padding: 5 },
};
const xAxisStyle = {
  axis: { stroke: axisTickColor },
  tickLabels: { fontSize: 8, fill: axisTickColor, padding: 5 },
};

const singleBarStyle = {
  data: { fill: '#4FC2C9', width: 28 },
  labels: { fontSize: 12 },
  parent: { border: '1px solid #ccc' },
};
const comparisonBarStyles = {
  dataBarStyle:  {
    data: { fill: '#4FC2C9', width: 28 },
    labels: { fontSize: 12 },
    parent: { border: '1px solid #ccc' },
  },
  comparisonDataBarStyle: {
    data: { fill: '#CED2D5', width: 28 },
    labels: { fontSize: 12 },
    parent: { border: '1px solid #ccc' },
  },
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
const debugComparisonJsonData = {
  'SearchViews': 4446,
  'PdpViews': 136,
  'CartClicks': 64,
  'CheckoutClicks': 34,
  'Purchases': 12,
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

// types
type Props = {
  jsonData: Object,
  debugMode?: ?boolean,
  comparisonJsonData?: Object,
}

type ProductConversionChartBar = {
  key: string,
  value: number,
  delta: ?number,
  label: string,
  conversion: ?number,
}

class ProductConversionChart extends React.Component {

  static defaultProps = {
    jsonData: {},
    debugMode: false,
    comparisonJsonData: {},
  };

  props: Props;

  @autobind
  jsonDataToChartData(jsonDisplay: Object): Array<ProductConversionChartBar> {

    const deltaDisplay = (deltaStr: number): number => {
      return _.round(parseFloat(deltaStr) * 100, 2);
    };

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

  @autobind
  renderDataChart(jsonData: Object) {
    return (
      <VictoryChart
        width={700}
        height={350}
        domainPadding={55}>
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
              barWidth={singleBarStyle.data.width}
              deltaToolTipCoordinates={{
                x: singleBarStyle.data.width * 1.2,
                offsetX: -85,
                y: 180,
                offsetY: 4,
                deltaTextDx: -12,
                deltaTextDy: 3.25,
              }}
              deltaToolTipStyles={{
                borderColor: '#363636',
                fill: '#3A4350',
                textColor: '#FFFFFF',
                fontSize: 6,
              }}
              conversionToolTipOffsetCoordinates={{
                dx: -100,
                y: 26,
              }}
              conversionToolTipStyles={{
                textColor: '#3A4350',
                fontSize: 6,
              }}
              toolTipColor="#363636"
              getDelta={(datum) => datum.delta}
              getConversion={(datum) => datum.conversion} />
          }
          style={singleBarStyle}
          data={this.jsonDataToChartData(jsonData)}
          x="key"
          y="value"
          events={barEvents} />
      </VictoryChart>
    );
  }

  @autobind
  renderDataVsComparisonChart(jsonData: Object, comparisonJsonData: Object = {}) {
    const { dataBarStyle, comparisonDataBarStyle } = comparisonBarStyles;

    return (
      <VictoryChart
        width={700}
        height={350}
        domainPadding={45}>
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
        <VictoryGroup offset={40}>
          <VictoryBar
            labelComponent={
              <ProductConversionToolTip
                barWidth={singleBarStyle.data.width}
                deltaToolTipCoordinates={{
                  x: singleBarStyle.data.width * 0.4,
                  offsetX: -67.5,
                  y: 170,
                  offsetY: 4,
                  deltaTextDx: -17,
                  deltaTextDy: 3.25,
                }}
                deltaToolTipStyles={{
                  borderColor: '#363636',
                  fill: '#3A4350',
                  textColor: 'white',
                  fontSize: 6,
                }}
                conversionToolTipOffsetCoordinates={{
                  dx: -60,
                  y: 26,
                }}
                conversionToolTipStyles={{
                  textColor: '#3A4350',
                  fontSize: 6,
                }}
                toolTipColor="#363636"
                getDelta={(datum) => datum.delta}
                getConversion={(datum) => datum.conversion} />
            }
            style={dataBarStyle}
            data={this.jsonDataToChartData(jsonData)}
            x="key"
            y="value"
            events={barEvents} />
          <VictoryBar
            labelComponent={
              <ProductConversionToolTip
                barWidth={singleBarStyle.data.width}
                deltaToolTipCoordinates={{
                  x: singleBarStyle.data.width * 1.3,
                  offsetX: -102,
                  y: 195,
                  offsetY: 4,
                  deltaTextDx: -27,
                  deltaTextDy: 3.25,
                }}
                deltaToolTipStyles={{
                  borderColor: '#CED2D5',
                  fill: '#CED2D5',
                  textColor: '3A4350',
                  fontSize: 6,
                }}
                conversionToolTipOffsetCoordinates={{
                  dx: -120,
                  y: 26,
                }}
                conversionToolTipStyles={{
                  textColor: '#3A4350',
                  fontSize: 6,
                }}
                toolTipColor="#9B9B9B"
                getDelta={(datum) => datum.delta}
                getConversion={(datum) => datum.conversion} />
            }
            style={comparisonDataBarStyle}
            data={this.jsonDataToChartData(comparisonJsonData)}
            x="key"
            y="value"
            events={barEvents} />
        </VictoryGroup>
      </VictoryChart>
    );
  }

  get chart() {
    let { debugMode, jsonData, comparisonJsonData } = this.props;

    if (debugMode) {
      jsonData = debugJsonData;
      comparisonJsonData = debugComparisonJsonData;

      return this.renderDataVsComparisonChart(jsonData, comparisonJsonData);
      //return this.renderDataChart(jsonData);
    }

    if (_.isNil(comparisonJsonData) || _.isEmpty(comparisonJsonData)) {
      return this.renderDataChart(jsonData);
    }

    return this.renderDataVsComparisonChart(jsonData, comparisonJsonData);
  }

  render() {
    return this.chart;
  }
}

export default ProductConversionChart;
