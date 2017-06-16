import React from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';

import styles from './datetime.css';

export const Moment = ({utc, value, format, emptyValue}) => {
  if (!value) {
    return <span styleName="time">{emptyValue}</span>;
  }

  const timeValue = utc ? moment.utc(value) : moment(value);

  return (
    <time styleName="time" dateTime={timeValue.local().format()}>
      {timeValue.local().format(format)}
    </time>
  );
};

Moment.propTypes = {
  utc: PropTypes.bool,
  value: PropTypes.string,
  format: PropTypes.string,
  emptyValue: PropTypes.string,
};

Moment.defaultProps = {
  format: 'L LTS',
  utc: true,
  emptyValue: 'not set',
};


export const DateTime = props => <Moment {...props} format={'L LT'} />;

DateTime.propTypes = {
  utc: PropTypes.bool,
  value: PropTypes.string
};

DateTime.defaultProps = {
  utc: true,
};


export const Date = props => <Moment {...props} format={'L'} />;

Date.propTypes = {
  utc: PropTypes.bool,
  value: PropTypes.string
};

Date.defaultProps = {
  utc: true,
};


export const Time = props => <Moment {...props} format={'LT'} />;

Time.propTypes = {
  utc: PropTypes.bool,
  value: PropTypes.string
};

Time.defaultProps = {
  utc: true,
};
