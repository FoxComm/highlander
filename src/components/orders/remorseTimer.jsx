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
      controls = <button onClick={this.addTime.bind(this, 15, 'm')}>+15</button>;
    }

    return (
      <div className="remorsetimer">
        <Countdown endDate={this.state.endDate} frozen={this.state.frozen}/>
        <div className="remorsetimer__controls">{controls}</div>
      </div>
    );
  }
}

RemorseTimer.propTypes = {
  endDate: React.PropTypes.string
};
