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
  format: 'MM/DD/YYYY HH:mm:ss'
};

const DateTime = (props) => <Moment value={props.value} format={'MM/DD/YYYY HH:mm'}/>;
const Date = (props) => <Moment value={props.value} format={'MM/DD/YYYY'}/>;
const Time = (props) => <Moment value={props.value} format={'HH:mm'}/>;

export {
  Moment,
  DateTime,
  Date,
  Time,
};
