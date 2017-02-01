// @flow

// libs
import React, { Component } from 'react';
import {
  VictoryBar,
  VictoryChart,
  VictoryAxis,
  VictoryTooltip,
  VictoryLabel,
} from 'victory';
import _ from 'lodash';

const axisTickColor = "#9BA3A7";
const yAxisStyle = {
  axis: { stroke: axisTickColor },
  grid: { stroke: axisTickColor, strokeWidth: 0.25, strokeDasharray: 2.5, opacity: 0.6 },
  tickLabels: { fontSize: 8, fill: axisTickColor },
};

const xAxisStyle = {
  axis: { stroke: axisTickColor },
  tickLabels: { fontSize: 6, fill: axisTickColor },
};

const dataTickValues = [
  "Category/Search View",
  "PDP View",
  "Add to Cart",
  "Enter Checkout",
  "Purchased",
];

const barStyle = {
  data: { fill: "#4FC2C9", width: 15 },
  labels: { fontSize: 12 },
  parent: { border: "1px solid #ccc" },
};

// Dummy response payload for UI debugging
const dummyJsonData = {
  "SearchViews": 876,
  "PdpViews": 500,
  "CartClicks": 379,
  "CheckoutClicks": 263,
  "Purchases": 68,
  "SearchToPdp": 57.1,
  "PdpToCart": 75.8,
  "CartToCheckout": 69.4,
  "CheckoutToPurchase": 25.6,
};

const barEvents = [
  {
    // Event: Always show tooltip label onMouseOut
    target: "data",
    eventHandlers: {
      onMouseOut: () => {
        return [{
          target: "labels",
          mutation: (props) => {
            return props.label;
          }
        }];
      }
    }
  }
];

class CustomToolTip extends React.Component {

  static defaultEvents = VictoryTooltip.defaultEvents;

  get tooltips() {
    const { barWidth, getDelta, datum } = this.props;

    const toolTipFlyoutStyle = {
      opacity: 0,
    };
    const toolTipStyle = {
      fontSize: 6,
    };

    const deltaToolTipFlyoutStyle = {
      stroke: "#363636",
      fill: "#3A4350",
    };
    const deltaToolTipStyle = {
      fill: "white",
      fontSize: 6,
    };
    const deltaValue = getDelta(datum);

    const deltaToolTip = (deltaValue > 0) ?
      <VictoryTooltip
        {...this.props}
        cornerRadius={0}
        flyoutStyle={deltaToolTipFlyoutStyle}
        style={deltaToolTipStyle}
        pointerLength={7}
        pointerWidth={19}
        dx={barWidth * 1.5}
        y={180}
        text={`${deltaValue} %`}
        orientation="left"
        labelComponent = {
          <VictoryLabel />
        }
        active={true}
        renderInPortal={false}
      />
      : false;

    return (
      <g>
        <VictoryTooltip
          {...this.props}
          flyoutStyle={toolTipFlyoutStyle}
          style={toolTipStyle}
          pointerLength={0}
          dy={-10}
          active={true}
          renderInPortal={false}
        />
        {deltaToolTip}
      </g>
    );
  }

  render() {
    return this.tooltips;
  }
}

type Props = {
  jsonData: Object,
  debugMode?: ?boolean,
}

class ProductConversionChart extends Component {

  props: Props;

  static defaultProps = {
    jsonData: {},
    debugMode: false,
  };

  get data(): Array<Object> {
    const { jsonData, debugMode } = this.props;

    const jsonDisplay = (debugMode) ? dummyJsonData : jsonData;

    return [
      {
        key: dataTickValues[0],
        value: jsonDisplay.SearchViews,
        label: jsonDisplay.SearchViews.toString(),
      },
      {
        key: dataTickValues[1],
        value: jsonDisplay.PdpViews,
        delta: _.round(jsonDisplay.SearchToPdp, 2),
        label: jsonDisplay.PdpViews.toString(),
      },
      {
        key: dataTickValues[2],
        value: jsonDisplay.CartClicks,
        delta: _.round(jsonDisplay.PdpToCart, 2),
        label: jsonDisplay.CartClicks.toString(),
      },
      {
        key: dataTickValues[3],
        value: jsonDisplay.CheckoutClicks,
        delta: _.round(jsonDisplay.CartToCheckout, 2),
        label: jsonDisplay.CheckoutClicks.toString(),
      },
      {
        key: dataTickValues[4],
        value: jsonDisplay.Purchases,
        delta: _.round(jsonDisplay.CheckoutToPurchase, 2),
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
            <CustomToolTip
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
