/* @flow */

import { capitalize, get, isNumber, identity } from 'lodash';
import classNames from 'classnames';
import React, { Component } from 'react';

import { PanelList, PanelListItem } from 'components/panel/panel-list';
import Currency from 'components/common/currency';
import RadioButton from 'components/forms/radio-button';

import styles from './stats.css';

type Props = {
  stats: Object,
  isLoading: boolean,
};

type State = {
  period: string;
}

const StatsValue = ({ value, currency, preprocess = identity }) => {
  if (!isNumber(value)) {
    return <span>—</span>;
  }

  const v = preprocess(value);

  return currency ? <Currency value={v} /> : <span>{v}</span>;
};

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
            className={styles.period}
            name="stats"
            id={period}
            checked={this.state.period === period}
            onChange={() => this.setState({ period })}
            disabled={isLoading}
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

    console.log(this.state.period);

    return (
      <div>
        {this.timeframes}
        <PanelList className={classNames(styles.stats, { [styles._loading]: isLoading })}>
          <PanelListItem title="Total Orders">
            <StatsValue value={get(stats, [this.state.period, 'ordersCount'])} />
          </PanelListItem>
          <PanelListItem title="Total Sales">
            <StatsValue value={get(stats, [this.state.period, 'totalSales'])} currency />
          </PanelListItem>
          <PanelListItem title="Avg. Order Size">
            <StatsValue value={get(stats, [this.state.period, 'averageOrderSize'])} preprocess={Math.round} />
          </PanelListItem>
          <PanelListItem title="Avg. Order Value">
            <StatsValue value={get(stats, [this.state.period, 'averageOrderSum'])} currency />
          </PanelListItem>
        </PanelList>
      </div>
    );
  }
}

export default CustomerGroupStats;
