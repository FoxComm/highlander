import React, { PropTypes } from 'react';
import moment from 'moment';

const Moment = props => {
  const timeValue = props.utc ? moment.utc(props.value) : moment(props.value);

  return (
    <time dateTime={timeValue.local().format()}>
      {timeValue.local().format(props.format)}
    </time>
  );
};

Moment.propTypes = {
  utc: PropTypes.bool,
  value: PropTypes.string,
  format: PropTypes.string
};

Moment.defaultProps = {
  format: 'L LTS',
  utc: true,
};

const DateTime = props => <Moment value={props.value} format={'L LT'}/>;

DateTime.propTypes = {
  utc: PropTypes.bool,
  value: PropTypes.string
};

const Date = props => <Moment value={props.value} format={'L'}/>;

Date.propTypes = {
  utc: PropTypes.bool,
  value: PropTypes.string
};

const Time = props => <Moment value={props.value} format={'LT'}/>;

Time.propTypes = {
  utc: PropTypes.bool,
  value: PropTypes.string
};

DateTime.defaultProps = Date.defaultProps = Time.defaultProps = {
  utc: true,
};

export {
  Moment,
  DateTime,
  Date,
  Time,
};
