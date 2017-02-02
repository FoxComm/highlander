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

// styles
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

// helpers
const unixTimeToDateFormat = (unixTimeStr, dateFormat = 'MMM D, YYYY') => {
  return moment.unix(parseInt(unixTimeStr, 10)).format(dateFormat);
};

const formatCurrency = (value, currencyCode = 'USD', fractionDigits = 0) => {
  // $FlowFixMe: Can't figure out how to fix 'identifier `Intl`. Could not resolve name'
  const formatter = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currencyCode,
    minimumFractionDigits: fractionDigits,
  });

  const moneyInCentsDecimal = parseFloat(value) / 100.0;

  return formatter.format(moneyInCentsDecimal);
};

const formatValue = (value, currencyCode = 'USD', fractionDigits = 0) => {
  if (!_.isNil(currencyCode)) {
    return formatCurrency(value, currencyCode, fractionDigits);
  } else {
    return parseInt(value);
  }
};

const formatTickValue = (datum: any, totalDataSize: number, groupSize: number = 16) => {
  if (_.inRange(totalDataSize, 0, groupSize)) {
    return datum.tickValue;
  }

  if (_.inRange(totalDataSize, groupSize, groupSize * 2)) {
    return (datum.tick % 2)
      ? ''
      : datum.tickValue;
  }

  if (_.inRange(totalDataSize, groupSize * 2, groupSize * 6)) {
    return (datum.tick % 2 || datum.tick % 3)
      ? ''
      : datum.tickValue;
  }
};

// Dummy data for UI debugging
const dummyQueryKey: string = 'track.1.product.2.debug';
const dummyJsonData: { dummyQueryKey: [ {x: number, y: number } ] } = {
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

// types
type Props = {
  jsonData: Object,
  debugMode?: boolean,
  segmentType?: string,
  queryKey?: string,
  currencyCode?: string,
}

class TotalRevenueChart extends React.Component {

  props: Props;

  static defaultProps = {
    jsonData: {},
    debugMode: false,
    segmentType: ChartSegmentType.Day,
    queryKey: '',
    currencyCode: 'USD',
  };

  @autobind
  generateDataTickValues(jsonData: any) {
    const { debugMode, queryKey, segmentType } = this.props;

    const jsonDisplay = (debugMode) ? dummyJsonData[dummyQueryKey] : jsonData;

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

      d.tickValue = moment.unix(integerTime).format(timeFormat);
    });

    return jsonDisplay;
  }

  get chart() {
    const { currencyCode } = this.props;

    const displayData = this.generateDataTickValues(this.data);
    const dataSize = _.size(displayData);

    return (
      <div>
        <VictoryChart
          domainPadding={20}>
          <VictoryAxis
            standalone={false}
            style={{
              axis: { stroke: colors.gray },
              tickLabels: { fontSize: (dataSize < 90) ? 8 : 6, fill: colors.gray },
            }}
            orientation="bottom"
            tickFormat={(x) => (x) => { return formatTickValue(displayData[x - 1], dataSize); }}
            tickValues={_.map(displayData, (datum) => { return datum.tickValue; })} />
          <VictoryAxis
            dependentAxis
            standalone={false}
            style={gridStyle}
            orientation="left"
            tickFormat={(rawValue) => (`${formatValue(rawValue, currencyCode)}`)} />
          <VictoryArea
            style={areaStyle}
            data={displayData}
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

  get data(): any {
    const { jsonData, debugMode, queryKey, currencyCode } = this.props;

    const jsonDisplay = (debugMode) ? dummyJsonData[dummyQueryKey] : jsonData[queryKey];

    // For each datum, set the x-axis tick value, mouseOver label and dateRange display
    for (let idx = 0; idx < jsonDisplay.length; idx++) {
      jsonDisplay[idx].tick = idx + 1;
      const valueDisplay = formatValue(jsonDisplay[idx].y, currencyCode);

      if (idx + 1 < jsonDisplay.length) {
        const beginTime = unixTimeToDateFormat(jsonDisplay[idx].x);
        const endTime = unixTimeToDateFormat(jsonDisplay[idx + 1].x);
        const dateRange = `${beginTime} - ${endTime}`;

        jsonDisplay[idx].label = `${valueDisplay}\n${dateRange}`;
      } else {
        const dateRange = unixTimeToDateFormat(jsonDisplay[idx].x);

        jsonDisplay[idx].label = `${valueDisplay}\n${dateRange}`;
      }
    }

    return jsonDisplay;
  }

  render() {
    return this.chart;
  }
}

export default TotalRevenueChart;
