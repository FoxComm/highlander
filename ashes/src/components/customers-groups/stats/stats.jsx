/* @flow */

import { capitalize, get, isNumber, isNaN, identity } from 'lodash';
import classNames from 'classnames';
import React, { Component } from 'react';

import { PanelList, PanelListItem } from 'components/panel/panel-list';
import Currency from 'components/common/currency';
import RadioButton from 'components/forms/radio-button';

import styles from './stats.css';

type Props = {
  stats: TCustomerGroupStats,
  isLoading: boolean,
};

type State = {
  period: string;
}

function getPercent(stats, period, fieldName) {
  const groupValue = get(stats, [period, 'group', fieldName]);

  if (!isNumber(groupValue)) {
    return void 0;
  }

  return groupValue / get(stats, [period, 'overall', fieldName]);
}

const StatsValue = ({ value, currency, preprocess = identity, className = '' }) => {
  if (!isNumber(value) || isNaN(value)) {
    return <span className={className}>—</span>;
  }

  const v = preprocess(Number(value)); // flow...

  return <span className={className}>{currency ? <Currency value={v} /> : v}</span>;
};

const StatsUnit = ({ title, stats, period, fieldName, currency = false, preprocess = identity }) => (
  <PanelListItem title={title}>
    <StatsValue
      value={get(stats, [period, 'group', fieldName])}
      preprocess={preprocess}
      currency={currency}
    />
    <StatsValue
      className={styles.percent}
      value={getPercent(stats, period, fieldName)}
      preprocess={(v: number) => `${(v * 100).toFixed(2)}%`}
    />
  </PanelListItem>
);

const getStatsUnitElement = (stats, period) => rest => (
  <StatsUnit stats={stats} period={period} {...rest} />
);

class CustomerGroupStats extends Component {
  props: Props;

  state: State = {
    period: 'month',
  };

  get timeframes() {
    const { isLoading, stats } = this.props;

    return (
      <div className={styles.periods}>
        {Object.keys(stats).map((period: string) => (
          <RadioButton
            id={period}
            className={styles.period}
            checked={this.state.period === period}
            onChange={() => this.setState({ period })}
            disabled={isLoading}
            key={period}
          >
            <label htmlFor={period}>{capitalize(period)}</label>
          </RadioButton>
        ))}
      </div>
    );
  }

  render() {
    const { isLoading, stats } = this.props;

    if (stats == null) {
      return null;
    }

    const Stats = getStatsUnitElement(stats, this.state.period);

    return (
      <div>
        {this.timeframes}
        <PanelList className={classNames(styles.stats, { [styles._loading]: isLoading })}>
          <Stats title="Total Orders" fieldName="ordersCount" />
          <Stats title="Total Sales" fieldName="totalSales" currency />
          <Stats title="Avg. Order Size" fieldName="averageOrderSize" preprocess={Math.round} />
          <Stats title="Avg. Order Value" fieldName="averageOrderSum" currency />
        </PanelList>
      </div>
    );
  }
}

export default CustomerGroupStats;
