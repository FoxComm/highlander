'use strict';

import React from 'react';
import moment from 'moment';

class Countdown extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      seconds: '0',
      minutes: '0',
      hours: '0',
      endDate: props.endDate
    };
  }

  addTime(number, key) {
    this.setState({
      endDate: moment(this.state.endDate).add(number, key).toISOString()
    });
    this.startInterval();
  }

  startInterval() {
    this.interval = this.interval || setInterval(this.tick.bind(this), 1000);
  }

  stopInterval() {
    if (this.interval) {
      clearInterval(this.interval);
    }
  }

  tick() {
    let difference = moment(this.state.endDate).diff(moment());
    if (difference <= 0) {
      this.setState({
        seconds: '0',
        minutes: '0',
        hours: '0'
      });
      this.stopInterval();
      return;
    }
    let duration = moment.duration(difference);
    this.setState({
      seconds: duration.seconds(),
      minutes: duration.minutes()
    });
    this.setState({
      hours: parseInt(
        duration
          .subtract(this.state.seconds, 'seconds')
          .subtract(this.state.minutes, 'minutes')
          .asHours(),
        10
      )
    });
  }

  componentDidMount() {
    this.startInterval();
  }

  componentWillUnmount() {
    this.stopInterval();
  }

  render() {
    return (
      <div>
        <div>{this.state.hours}:{this.state.minutes}:{this.state.seconds}</div>
        <a className='btn' onClick={this.addTime.bind(this, 15, 'm')}>+15</a>
      </div>
    );
  }
}

Countdown.propTypes = {
  endDate: React.PropTypes.string
};

Countdown.defaultProps = {
  endDate: moment().add(24, 'h').toISOString()
};

export default Countdown;
