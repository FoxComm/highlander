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

  extendButton() {
    return (
      <button className="fc-btn fc-remorse-timer-extend" onClick={this.addTime.bind(this, 15, 'm')}>
        <i className="icon-add"></i> 15 min
      </button>
    );
  }

  controls() {
    if (this.state.frozen) {
      return 'Frozen while editing.';
    } else {
      return this.extendButton();
    }
  }

  render() {
    return (
      <div className="fc-remorse-timer">
        <Countdown endDate={this.state.endDate} frozen={this.state.frozen}/>
        <div className="fc-remorse-timer-controls">{this.controls()}</div>
      </div>
    );
  }
}

RemorseTimer.propTypes = {
  endDate: React.PropTypes.string
};
