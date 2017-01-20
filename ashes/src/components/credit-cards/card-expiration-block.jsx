/* @flow */

// libs
import React, { Component, Element } from 'react';

// components
import Dropdown from '../dropdown/dropdown';
import FormField from '../forms/formfield';

// utils
import * as CardUtils from 'lib/credit-card-utils';

type Props = {
  month: string;
  year: string;
  onMonthChange: Function;
  onYearChange: Function;
}

export default class ExpirationBlock extends Component {
  props: Props;

  shouldComponentUpdate(nextProps: Props): boolean {
    return nextProps.month !== this.props.month || nextProps.year !== this.props.year;
  }

  render(): Element {
    const { month, year, onMonthChange, onYearChange } = this.props;

    return (
      <li className="fc-credit-card-form__line">
        <label className="fc-credit-card-form__label">Expiration Date</label>
        <div className="fc-grid">
          <div className="fc-col-md-1-2">
            <FormField getTargetValue={() => month} validationLabel="Month" required>
              <Dropdown name="expMonth"
                        id="expMonth"
                        items={CardUtils.monthList()}
                        placeholder="Month"
                        value={month}
                        onChange={onMonthChange} />
            </FormField>

          </div>
          <div className="fc-col-md-1-2">
            <FormField getTargetValue={() => year} validationLabel="Year" required>
              <Dropdown name="expYear"
                        id="expYear"
                        items={CardUtils.expirationYears()}
                        placeholder="Year"
                        value={year}
                        onChange={onYearChange} />
            </FormField>
          </div>
        </div>
      </li>
    );
  }
}
