'use strict';

import React, { PropTypes } from 'react';
import classNames from 'classnames';
import moment from 'moment';

export default class Countdown extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {};
  }

  startInterval() {
    this.interval = this.interval || setInterval(this.tick.bind(this), 1000);
  }

  stopInterval() {
    if (this.interval) {
      clearInterval(this.interval);
      this.interval = null;
    }
  }

  tick() {
    let timeLeft = Math.max(0, moment(this.props.endDate).utc().diff(moment.utc()));
    this.setState({
      ending: timeLeft < moment.duration(3, 'm'),
      difference: moment.utc(timeLeft).format('HH:mm:ss')
    });
    if (!timeLeft || this.props.frozen) {
      this.stopInterval();
    } else {
      this.startInterval();
    }
  }

  componentDidMount() {
    this.tick();
    this.startInterval();
  }

  componentWillUnmount() {
    this.stopInterval();
  }

  render() {
    let classnames = classNames({
      'fc-countdown': true,
      'fc-countdown_ending': this.state.ending,
      'fc-countdown_frozen': this.props.frozen
    });

    return (
      <div className={classnames}>{this.state.difference}</div>
    );
  }
}

Countdown.propTypes = {
  endDate: PropTypes.string,
  frozen: PropTypes.bool
};

Countdown.defaultProps = {
  endDate: moment.utc().format()
};
