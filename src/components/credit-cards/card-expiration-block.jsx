/* @flow */

// libs
import React, { Component, Element } from 'react';

// components
import Dropdown from '../dropdown/dropdown';

// utils
import * as CardUtils from '../../lib/credit-card-utils';

type Props = {
  month: string;
  year: string;
  onMonthChange: Function;
  onYearChange: Function;
}

export default class ExpirationBlock extends Component {
  props: Props;

  static defaultProps = {
    month: 1,
    year: 2016,
  };

  shouldComponentUpdate(nextProps: Props): boolean {
    return nextProps.month !== this.props.month || nextProps.year !== this.props.year;
  }

  render(): Element {
    console.log('rendering ExpirationBlock');
    const { month, year, onMonthChange, onYearChange } = this.props;

    return (
      <li className="fc-credit-card-form__line">
        <label className="fc-credit-card-form__label">Expiration Date</label>
        <div className="fc-grid">
          <div className="fc-col-md-1-2">
            <Dropdown name="expMonth"
                      items={CardUtils.monthList()}
                      placeholder="Month"
                      value={month}
                      onChange={onMonthChange} />
          </div>
          <div className="fc-col-md-1-2">
            <Dropdown name="expYear"
                      items={CardUtils.expirationYears()}
                      placeholder="Year"
                      value={year}
                      onChange={onYearChange} />
          </div>
        </div>
      </li>
    );
  }
}
