/* @flow */

// libs
import React, { Component, Element } from 'react';

// components
import { TextDropdown } from 'components/core/dropdown';
import FormField from '../forms/formfield';

// utils
import * as CardUtils from 'lib/credit-card-utils';

type Props = {
  month: string,
  year: string,
  onMonthChange: Function,
  onYearChange: Function,
};

export default class ExpirationBlock extends Component {
  props: Props;

  shouldComponentUpdate(nextProps: Props): boolean {
    return nextProps.month !== this.props.month || nextProps.year !== this.props.year;
  }

  render() {
    const { month, year, onMonthChange, onYearChange } = this.props;

    return (
      <li className="fc-credit-card-form__line">
        <label className="fc-credit-card-form__label">Expiration Date</label>
        <div className="fc-grid">
          <div className="fc-col-md-1-2">
            <FormField getTargetValue={() => month} validationLabel="Month" required>
              <TextDropdown
                className="at-expMonth"
                name="expMonth"
                items={CardUtils.monthList()}
                placeholder="Month"
                value={month}
                onChange={onMonthChange}
              />
            </FormField>

          </div>
          <div className="fc-col-md-1-2">
            <FormField getTargetValue={() => year} validationLabel="Year" required>
              <TextDropdown
                className="at-expYear"
                name="expYear"
                items={CardUtils.expirationYears()}
                placeholder="Year"
                value={year}
                onChange={onYearChange}
              />
            </FormField>
          </div>
        </div>
      </li>
    );
  }
}
