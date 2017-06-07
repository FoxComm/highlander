/* @flow */

// libs
import moment from 'moment';
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';

// styles
import s from './countdown.css';

type Props = {
  /** Date to start countdown from */
  endDate: string,
  /** Threshold when countdown is in ending state (e.g. 5 minutes till end). moment.js object */
  endingThreshold: Object,
  /** Weather countdown is stopped */
  frozen?: boolean,
  /** Time format */
  format?: string,
  /** How often component's time should be updated in milliseconds */
  tickInterval?: number,
  /** Callback to be called on countdown finished */
  onCountdownFinished?: () => any,
};

type State = {
  ending: boolean,
  difference: string,
};

export default class Countdown extends Component {
  props: Props;
  state: State = {
    ending: false,
    difference: '',
  };

  static defaultProps: $Shape<Props> = {
    endingThreshold: moment.duration(3, 'm'),
    frozen: false,
    format: 'HH:mm:ss',
    tickInterval: 1000,
  };

  interval: ?number = null;

  startInterval(): void {
    this.interval = this.interval || setInterval(this.tick, this.props.tickInterval);
  }

  stopInterval(): void {
    if (this.interval) {
      this.interval = clearInterval(this.interval);
    }
  }

  @autobind
  tick(end: ?string): void {
    const { endingThreshold, format, frozen, onCountdownFinished } = this.props;

    const endDate = end || this.props.endDate;
    const timeLeft = Math.max(0, moment(endDate).utc().diff(moment.utc()));

    this.setState({
      ending: timeLeft < endingThreshold.asMilliseconds(),
      difference: moment.utc(timeLeft).format(format)
    });

    if (!timeLeft || frozen) {
      this.stopInterval();
    } else {
      this.startInterval();
    }

    if (!timeLeft && onCountdownFinished) {
      onCountdownFinished();
    }
  }

  componentDidMount(): void {
    this.tick();
  }

  componentWillReceiveProps(nextProps: Props): void {
    // force recalc of difference value
    this.tick(nextProps.endDate);
  }

  componentWillUnmount(): void {
    this.stopInterval();
  }

  render() {
    const cls = classNames(s.countdown, {
      [s.ending]: this.state.ending,
      [s.frozen]: this.props.frozen
    });

    return (
      <div className={cls}>{this.state.difference}</div>
    );
  }
}
