/* @flow */

// libs
import { capitalize, get, identity } from 'lodash';
import classNames from 'classnames';
import React, { Component, Element } from 'react';

// components
import { PanelList, PanelListItem } from 'components/panel/panel-list';
import Currency from 'components/common/currency';
import RadioButton from 'components/core/radio-button';

// styles
import s from './stats.css';

type Props = {
  stats: TCustomerGroupStats,
  isLoading: boolean,
};

type State = {
  period: string,
}

function getPercent(stats, period, fieldName) {
  const groupValue = get(stats, [period, 'group', fieldName]);

  if (!groupValue) {
    return void 0;
  }

  return groupValue / get(stats, [period, 'overall', fieldName]);
}

const StatsValue = ({ value, currency, preprocess = identity, className = '' }) => {
  if (!value) {
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
      className={s.percent}
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

  get timeframes(): Array<Element<*>> {
    return Object.keys(this.props.stats).map((period: string) => (
      <RadioButton
        id={period}
        label={capitalize(period)}
        className={s.period}
        checked={this.state.period === period}
        onChange={() => this.setState({ period })}
        disabled={this.props.isLoading}
        key={period}
      />
    ));
  }

  render() {
    const { isLoading, stats } = this.props;

    if (stats == null) {
      return null;
    }

    const Stats = getStatsUnitElement(stats, this.state.period);

    return (
      <div>
        <div className={s.periodsContainer}>
          {this.timeframes}
        </div>
        <PanelList className={classNames(s.stats, { [s.loading]: isLoading })}>
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
