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

  addTime(time) {
    this.setState({millisecondsRemaining: this.state.millisecondsRemaining + time});
  }

  addFifteenMinutes() {
    this.addTime(15 * (1000 * 60));
  }

  tick() {
    this.setState({millisecondsRemaining: this.state.millisecondsRemaining - 1000});
    if (this.state.millisecondsRemaining <= 0) {
      this.setState({millisecondsRemaining: 0});
      clearInterval(this.interval);
    }
  }

  componentDidMount() {
    this.setState({millisecondsRemaining: moment(this.props.date).diff(moment())});
    this.interval = setInterval(this.tick.bind(this), 1000);
  }

  componentWillUnmount() {
    clearInterval(this.interval);
  }

  render() {
    let seconds = parseInt((this.state.millisecondsRemaining / 1000) % 60, 10);
    let minutes = parseInt((this.state.millisecondsRemaining / (1000 * 60)) % 60, 10);
    let hours = parseInt((this.state.millisecondsRemaining / (1000 * 60 * 60)) % 24, 10);
    return (
      <div>
        <div>{hours}:{minutes}:{seconds}</div>
        <a className='btn' onClick={this.addFifteenMinutes.bind(this)}>+15</a>
      </div>
    );
  }
}

Countdown.propTypes = {
  date: React.PropTypes.string
};

Countdown.defaultProps = {
  date: moment('2016-10-20').toISOString()
};

export default Countdown;
