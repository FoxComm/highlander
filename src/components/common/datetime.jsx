import React, { PropTypes } from 'react';
import moment from 'moment';

const Moment = props => {
  return (
    <time dateTime={moment(props.value).format()}>
      {moment(props.value).format(props.format)}
    </time>
  );
};

Moment.propTypes = {
  value: PropTypes.string,
  format: PropTypes.string
};

Moment.defaultProps = {
  format: 'L LTS'
};

const DateTime = props => <Moment value={props.value} format={'L LT'}/>;

DateTime.propTypes = {
  value: PropTypes.string
};

const Date = props => <Moment value={props.value} format={'L'}/>;

Date.propTypes = {
  value: PropTypes.string
};

const Time = props => <Moment value={props.value} format={'LT'}/>;

Time.propTypes = {
  value: PropTypes.string
};

export {
  Moment,
  DateTime,
  Date,
  Time,
};
