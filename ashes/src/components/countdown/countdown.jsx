
/* @flow */

import React, { Component, Element } from 'react';
import classNames from 'classnames';
import moment from 'moment';

type Props = {
  endDate: string,
  frozen: boolean,
  onCountdownFinished?: Function,
};

type State = {
  ending?: boolean,
  difference?: string,
};

export default class Countdown extends Component {
  props: Props;
  state: State = {};
  interval: ?number = null;

  static defaultProps = {
    endDate: moment.utc().format(),
  };

  startInterval(): void {
    this.interval = this.interval || setInterval(this.tick.bind(this), 1000);
  }

  stopInterval(): void {
    if (this.interval) {
      clearInterval(this.interval);
      this.interval = null;
    }
  }

  tick(end: ?string): void {
    const endDate = end || this.props.endDate;
    const timeLeft = Math.max(0, moment(endDate).utc().diff(moment.utc()));
    this.setState({
      ending: timeLeft < moment.duration(3, 'm'),
      difference: moment.utc(timeLeft).format('HH:mm:ss')
    });
    if (!timeLeft || this.props.frozen) {
      this.stopInterval();
    } else {
      this.startInterval();
    }
    if (!timeLeft && this.props.onCountdownFinished) {
      this.props.onCountdownFinished();
    }
  }

  componentDidMount(): void {
    this.tick();
    this.startInterval();
  }

  componentWillReceiveProps(nextProps: Props): void {
    // force recalc of difference value
    this.tick(nextProps.endDate);
  }

  componentWillUnmount(): void {
    this.stopInterval();
  }

  render() {
    const classnames = classNames({
      'fc-countdown': true,
      'fc-countdown_ending': this.state.ending,
      'fc-countdown_frozen': this.props.frozen
    });

    return (
      <div className={classnames}>{this.state.difference}</div>
    );
  }
}
