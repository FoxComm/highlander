// @flow

import _ from 'lodash';
import React, { Component } from 'react';
import classNames from 'classnames';
import styles from './css/adjust-quantity.css';
import { autobind } from 'core-decorators';

import Counter from './counter';
import Overlay from '../overlay/overlay';

type State = {
  diff: number,
  value: number,
}

type Props = {
  value: number,
  onChange: (diff: number, quantity: number) => void,
  isPopupShown: boolean,
  togglePopup: (visible: boolean) => void,
  min: number,
}

export default class AdjustQuantity extends Component {
  props: Props;
  state: State = {
    diff: 0,
    value: this.props.value,
  };

  static defaultProps = {
    min: 0,
  };

  adjustValue(newValue: number) {
    if (newValue < this.props.min) {
      newValue = this.props.min;
    }
    const diff = newValue - this.props.value;

    this.setState({
      value: newValue,
      diff,
    }, () => {
      this.props.onChange(diff, newValue);
    });
  }

  @autobind
  handleChange({target}: Object) {
    const quantity = Number(target.value);
    if (!_.isNaN(quantity)) {
      this.adjustValue(quantity);
    }
  }

  @autobind
  handleInputFocus() {
    this.props.togglePopup(true);
  }

  hide() {
    this.props.togglePopup(false);
  }

  render() {
    const popupState = classNames({
      '_open': this.props.isPopupShown,
    });

    return (
      <div styleName="block">
        <Overlay shown={this.props.isPopupShown} onClick={() => this.hide()} />
        <input
          className="fc-text-input _no-counters"
          styleName="input"
          type="number"
          value={this.state.value}
          onChange={this.handleChange}
          onFocus={this.handleInputFocus}
          min={this.props.min}
        />
        <div styleName="popup" className={popupState}>
          <div styleName="title">Adjust Quantity</div>
          <Counter
            value={this.state.diff}
            increaseAction={() => this.adjustValue(this.state.value + 1)}
            decreaseAction={() => this.adjustValue(this.state.value - 1)}
            min={null}
            onBlur={evt => evt.stopPropagation()}
          />
        </div>
      </div>
    );
  }
}
