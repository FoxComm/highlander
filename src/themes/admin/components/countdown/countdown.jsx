'use strict';

import React from 'react';
import moment from 'moment';

class Countdown extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      millisecondsRemaining: 0
    };
  }

  addMinutes(minutes) {
    this.addTime(minutes * (1000 * 60));
  }

  addHours(hours) {
    this.addTime(hours * (1000 * 60 * 60));
  }

  addTime(time) {
    this.setState({millisecondsRemaining: this.state.millisecondsRemaining + time});
    if (!this.interval) {
      this.startInterval();
    }
  }

  startInterval() {
    this.interval = setInterval(this.tick.bind(this), 1000);
  }

  stopInterval() {
    clearInterval(this.interval);
  }

  tick() {
    this.setState({millisecondsRemaining: this.state.millisecondsRemaining - 1000});
    if (this.state.millisecondsRemaining <= 0) {
      this.setState({millisecondsRemaining: 0});
      this.stopInterval();
    }
  }

  componentDidMount() {
    this.setState({millisecondsRemaining: moment(this.props.date).diff(moment())});
    if (this.state.millisecondsRemaining > 0) {
      this.startInterval();
    }
  }

  componentWillUnmount() {
    this.stopInterval();
  }

  render() {
    let seconds = parseInt((this.state.millisecondsRemaining / 1000) % 60, 10);
    let minutes = parseInt((this.state.millisecondsRemaining / (1000 * 60)) % 60, 10);
    let hours = parseInt((this.state.millisecondsRemaining / (1000 * 60 * 60)) % 24, 10);
    return (
      <div>
        <div>{hours}:{minutes}:{seconds}</div>
        <a className='btn' onClick={this.addMinutes.bind(this, 15)}>+15</a>
      </div>
    );
  }
}

Countdown.propTypes = {
  date: React.PropTypes.string
};

Countdown.defaultProps = {
  date: moment('2015-10-20').toISOString()
};

export default Countdown;
