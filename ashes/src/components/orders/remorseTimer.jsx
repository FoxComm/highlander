
/* @flow */

import React, { Component, Element } from 'react';
import moment from 'moment';
import { AddButton } from '../common/buttons';
import Countdown from '../countdown/countdown';

type Props = {
  initialEndDate: string,
  onIncreaseClick: Function,
  onCountdownFinished?: Function,
};

type State = {
  frozen: boolean,
};

export default class RemorseTimer extends Component {
  props: Props;
  state: State = {
    frozen: false
  };

  onToggleOrderEdit(): void {
    this.setState({
      frozen: !this.state.frozen
    });
  }

  extendButton(): Element {
    return (
      <AddButton id="remorse-timer-extend-btn" className="fc-remorse-timer-extend" onClick={ this.props.onIncreaseClick }>
        15 min
      </AddButton>
    );
  }

  controls(): Element|string {
    if (this.state.frozen) {
      return 'Frozen while editing.';
    } else {
      return this.extendButton();
    }
  }

  render(): Element {
    return (
      <div className="fc-remorse-timer">
        <Countdown
          endDate={this.props.initialEndDate}
          frozen={this.state.frozen}
          onCountdownFinished={this.props.onCountdownFinished}
        />
        <div className="fc-remorse-timer-controls">{this.controls()}</div>
      </div>
    );
  }
}
