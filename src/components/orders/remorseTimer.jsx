import React, { PropTypes } from 'react';
import moment from 'moment';
import { AddButton } from '../common/buttons';
import Countdown from '../countdown/countdown';

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
      endDate: moment(this.state.endDate).add(number, key).format()
    });
  }

  onToggleOrderEdit() {
    this.setState({
      frozen: !this.state.frozen
    });
  }

  extendButton() {
    return (
      <AddButton className="fc-remorse-timer-extend" onClick={this.addTime.bind(this, 15, 'm')}>
        &nbsp;15 min
      </AddButton>
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
  endDate: PropTypes.string
};
