// @flow

// libs
import React from 'react';
import _ from 'lodash';
import moment from 'moment';
import { autobind } from 'core-decorators';

// components
import {
  VictoryScatter,
  VictoryArea,
  VictoryChart,
  VictoryAxis,
} from 'victory';
import TotalRevenueToolTip from './total-revenue-tooltip';

const colors = {
  tealGreenish: '#78CFD4',
  gray: '#808080',
  teal: '#008080',
  white: '#FFFFFF',
  darkBlue: '#3A434F',
};

const areaStyle = {
  data: { fill: colors.tealGreenish, opacity: 0.25, stroke: colors.teal }
};

const scatterStyle = {
  data: { fill: colors.tealGreenish, opacity: 1 }
};

const gridStyle = {
  axis: { stroke: colors.gray },
  grid: { stroke: colors.gray, strokeWidth: 0.25, strokeDasharray: 2.5, opacity: 0.6 },
  tickLabels: { fontSize: 8, fill: colors.gray },
};

const unixTimeToDateFormat = (unixTimeStr, dateFormat = 'MMM D, YYYY') => {
  return moment.unix(parseInt(unixTimeStr, 10)).format(dateFormat);
};

const formatRevenue = (moneyInCents, fractionDigits = 0) => {
  const formatter = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: fractionDigits,
  });

  const moneyInCentsDecimal = parseFloat(moneyInCents) / 100.0;

  return formatter.format(moneyInCentsDecimal);
};

// Dummy data for UI debugging
const dummyJsonData = [
  { revenue: 750000, timestamp: 1477983600 },
  { revenue: 1650000, timestamp: 1478592000 },
  { revenue: 1425000, timestamp: 1479196800 },
  { revenue: 1900000, timestamp: 1479801600 },
];
const dummyDataTickValues = [
  'Nov 1', 
  'Nov 8', 
  'Nov 15', 
  'Nov 22',
];

type Props = {
  jsonData: Object,
  debugMode?: ?boolean,
  dayWeekOrMonth?: ?string, // d = days, w = weeks, m = month
}

class TotalRevenueChart extends React.Component {

  props: Props;

  static defaultProps = {
    jsonData: {},
    debugMode: false,
    dayWeekOrMonth: 'd',
  };

  get data() {
    const { jsonData, debugMode } = this.props;

    const jsonDisplay = (debugMode) ? dummyJsonData : jsonData;

    // This is for dummyJsonData
    // TODO: Update when real JSON response format is known
    for (let idx = 0; idx < jsonDisplay.length; idx++) {
      jsonDisplay[idx].x = idx + 1;
      const revenueDisplay = formatRevenue(jsonDisplay[idx].revenue);

      if (idx + 1 < jsonDisplay.length) {
        const beginTime = unixTimeToDateFormat(jsonDisplay[idx].timestamp);
        const endTime = unixTimeToDateFormat(jsonDisplay[idx + 1].timestamp);
        const dateRange = `${beginTime} - ${endTime}`;
        jsonDisplay[idx].label = `${revenueDisplay}\n${dateRange}`;
      } else {
        const dateRange = unixTimeToDateFormat(jsonDisplay[idx].timestamp);
        jsonDisplay[idx].label = `${revenueDisplay}\n${dateRange}`;
      }
    }

    return jsonDisplay;
  }

  @autobind
  generateDataTickValues(fromData) {
    const { debugMode } = this.props;

    if (debugMode) {
      return dummyDataTickValues;
    }

    //TODO: Generate "MMM D" date formats to go along x-axis from json response data
    return [];
  }

  get chart() {
    const displayData = this.data;

    return (
      <div>
        <VictoryChart
          domainPadding={20}>
          <VictoryAxis
            standalone={false}
            style={{
              axis: { stroke: colors.gray },
              tickLabels: { fontSize: 8, fill: colors.gray },
            }}
            orientation="bottom"
            tickValues={this.generateDataTickValues(displayData)} />
          <VictoryAxis
            dependentAxis
            standalone={false}
            style={gridStyle}
            orientation="left"
            tickFormat={(rawRevenue) => (`${formatRevenue(rawRevenue)}`)} />
          <VictoryArea
            style={areaStyle}
            data={this.data}
            x="x"
            y="revenue" />
          <VictoryScatter
            labelComponent={<TotalRevenueToolTip />}
            style={scatterStyle}
            data={displayData}
            x="x"
            y="revenue" />
        </VictoryChart>
      </div>
    );
  }

  render() {
    return this.chart;
  }
}

export default TotalRevenueChart;
