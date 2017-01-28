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
  data: { fill: colors.tealGreenish, opacity: 0 }
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
const dummyQueryKey = 'track.1.product.2.debug';
const dummyJsonData = {
  dummyQueryKey: [
    // y: Revenue in cents, x: UnixTimestamp
    { y: 750000, x: 1477983600 },
    { y: 1650000, x: 1478592000 },
    { y: 1425000, x: 1479196800 },
    { y: 1900000, x: 1479801600 },
  ],
};

export const ChartSegmentType = {
  Hour: 'h',
  Day: 'd',
  Week: 'w',
  Month: 'm',
};

type Props = {
  jsonData: Object,
  debugMode?: boolean,
  segmentType?: string,
  queryKey?: string,
}

class TotalRevenueChart extends React.Component {

  props: Props;

  static defaultProps = {
    jsonData: {},
    debugMode: false,
    segmentType: ChartSegmentType.Day,
    queryKey: '',
  };

  @autobind
  generateDataTickValues(fromData) {
    const { jsonData, debugMode, queryKey, segmentType } = this.props;

    const jsonDisplay = (debugMode) ? dummyJsonData[dummyQueryKey] : jsonData[queryKey];

    let tickValues = [];

    _.each(jsonDisplay, (d) => {
      const integerTime = parseInt(d.x);
      let timeFormat = null;

      switch(segmentType) {
        case ChartSegmentType.Hour:
          timeFormat = 'h A';
          break;
        case ChartSegmentType.Day:
        case ChartSegmentType.Week:
          timeFormat = 'MMM D';
          break;
        case ChartSegmentType.Month:
          timeFormat = 'MMM';
          break;
      }

      tickValues = _.concat(tickValues, moment.unix(integerTime).format(timeFormat));
    });

    return tickValues;
  }

  get chart() {
    const displayData = this.data;
    const dataTickValues = this.generateDataTickValues(displayData);

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
            tickValues={dataTickValues} />
          <VictoryAxis
            dependentAxis
            standalone={false}
            style={gridStyle}
            orientation="left"
            tickFormat={(rawRevenue) => (`${formatRevenue(rawRevenue)}`)} />
          <VictoryArea
            style={areaStyle}
            data={this.data}
            x="tick"
            y="y" />
          <VictoryScatter
            labelComponent={<TotalRevenueToolTip />}
            style={scatterStyle}
            data={displayData}
            x="tick"
            y="y" />
        </VictoryChart>
      </div>
    );
  }

  get data() {
    const { jsonData, debugMode, queryKey } = this.props;

    const jsonDisplay = (debugMode) ? dummyJsonData[dummyQueryKey] : jsonData[queryKey];

    // For each datum, set the x-axis tick value, mouseOver label and dateRange display
    for (let idx = 0; idx < jsonDisplay.length; idx++) {
      jsonDisplay[idx].tick = idx + 1;
      const revenueDisplay = formatRevenue(jsonDisplay[idx].y);

      if (idx + 1 < jsonDisplay.length) {
        const beginTime = unixTimeToDateFormat(jsonDisplay[idx].x);
        const endTime = unixTimeToDateFormat(jsonDisplay[idx + 1].x);
        const dateRange = `${beginTime} - ${endTime}`;

        jsonDisplay[idx].label = `${revenueDisplay}\n${dateRange}`;
      } else {
        const dateRange = unixTimeToDateFormat(jsonDisplay[idx].x);

        jsonDisplay[idx].label = `${revenueDisplay}\n${dateRange}`;
      }
    }

    return jsonDisplay;
  }

  render() {
    return this.chart;
  }
}

export default TotalRevenueChart;
