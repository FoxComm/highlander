'use strict';

import React, { PropTypes } from 'react';
import moment from 'moment';

const Moment = (props) => {
  return (
    <time dateTime={moment(props.value).format()}>
      {moment(props.value).format(props.format)}
    </time>
  );
};

Moment.propTypes = {
  value: PropTypes.string.isRequired,
  format: PropTypes.string
};

Moment.defaultProps = {
  format: 'L LTS'
};

const DateTime = (props) => <Moment value={props.value} format={'L LT'}/>;
const Date = (props) => <Moment value={props.value} format={'L'}/>;
const Time = (props) => <Moment value={props.value} format={'LT'}/>;

export {
  Moment,
  DateTime,
  Date,
  Time,
};
