'use strict';

import React from 'react';
import moment from 'moment';
import Countdown from '../countdown/countdown';
import { listenTo, stopListeningTo } from '../../lib/dispatcher';

const editEvent = 'toggle-order-edit';

export default class RemorseTimer extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      endDate: props.endDate,
      frozen: false
    };
  }

  addTime(number, key) {
    this.setState({
      endDate: moment(this.state.endDate).add(number, key)
    });
  }

  componentDidMount() {
    listenTo(editEvent, this);
  }

  componentWillUnmount() {
    stopListeningTo(editEvent, this);
  }

  onToggleOrderEdit() {
    this.setState({
      frozen: !this.state.frozen
    });
  }

  render() {
    let controls;

    if (this.state.frozen) {
      controls = 'Frozen while editing.';
    } else {
      controls = <button className="fc-btn" onClick={this.addTime.bind(this, 15, 'm')}><i className="icon-add"></i> 15 min</button>;
    }

    return (
      <div className="fc-remorse-timer">
        <Countdown endDate={this.state.endDate} frozen={this.state.frozen}/>
        <div className="fc-remorse-timer-controls">{controls}</div>
      </div>
    );
  }
}

RemorseTimer.propTypes = {
  endDate: React.PropTypes.string
};
