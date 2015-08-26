  'use strict';

import React from 'react';
import moment from 'moment';
import { listenTo, stopListeningTo } from '../../lib/dispatcher';

const zeroTime = '00:00:00';
const editEvent = 'toggle-order-edit';

export default class Countdown extends React.Component {
  constructor(props) {
    super(props);
    this.onToggleOrderEdit = this.onToggleOrderEdit.bind(this);
    this.state = {
      difference: zeroTime,
      endDate: props.endDate,
      frozen: false
    };
  }

  addTime(number, key) {
    this.setState({
      endDate: moment(this.state.endDate).add(number, key)
    });
    this.startInterval();
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
    let difference = moment(this.state.endDate).diff(moment.utc());
    if (difference <= 0) {
      this.setState({
        difference: zeroTime
      });
      this.stopInterval();
    } else {
      this.setState({
        difference: moment.utc(difference).format('HH:mm:ss')
      });
    }
  }

  componentDidMount() {
    listenTo(editEvent, this);
    this.tick();
    this.startInterval();
  }

  componentWillUnmount() {
    stopListeningTo(editEvent, this);
    this.stopInterval();
  }

  onToggleOrderEdit() {
    this.setState({
      frozen: !this.state.frozen
    });
  }

  render() {
    let
      rightContent = null;

    if (this.state.frozen) {
      this.stopInterval();
      rightContent = <span>Frozen while editing.</span>;
    } else {
      this.startInterval();
      rightContent = <button className='btn' onClick={this.addTime.bind(this, 15, 'm')}>+15</button>;
    }
    return (
      <div className='countdown'>
        <span>{this.state.difference}</span>
        {rightContent}
      </div>
    );
  }
}

Countdown.propTypes = {
  endDate: React.PropTypes.string
};

Countdown.defaultProps = {
  endDate: moment.utc().add(3, 'h').format()
};
