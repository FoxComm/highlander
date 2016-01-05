import React, { PropTypes } from 'react';

import _ from 'lodash';
import classNames from 'classnames';

const DatePicker = props => {
  const { month, year } = props;

  const firstMonth = new Date(year, month - 1, 1);
  const secondMonth = new Date(year, month, 1);
  const weeks = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];

  const daysToDisplay = date => {
    const startDay = 1 - date.getDay(); // Day is 1-based in JS Date.

    return _.range(startDay, startDay + 35).map(day => {
      const dt = new Date(date.getFullYear(), date.getMonth(), day);
      const klass = classNames('fc-datepicker__day', {
        '_last-month': date.getMonth() > dt.getMonth(),
        '_next-month': date.getMonth() < dt.getMonth()
      });

      return <div className={klass}>{dt.getDate()}</div>;
    });
  };

  const render = date => {
    const monthName = date.toLocaleString('en-us', { month: 'long', year: 'numeric' });

    return (
      <div className="fc-datepicker__month">
        <div className="fc-datepicker__month-name">
          <span>{monthName}</span>
        </div>
        <div className="fc-datepicker__weeks-header">
          {weeks.map(w => <div className="fc-datepicker__week">{w}</div>)}
        </div>
        <div className="fc-datepicker__days">
          {daysToDisplay(date)}
        </div>
      </div>
    );
  };

  return (
    <div className="fc-datepicker">
      <input type="text" />
      <div className="fc-datepicker__dropdown-container">
        {render(firstMonth)}
        {render(secondMonth)}
      </div>
    </div>
  );
};

DatePicker.propTypes = {
  month: PropTypes.number,
  year: PropTypes.number
};

DatePicker.defaultProps = {
  month: new Date().getMonth() + 1,
  year: new Date().getFullYear()
};

export default DatePicker;
