// @flow

import _ from 'lodash';
import React, { Component } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

import Counter from 'components/core/counter';
import BodyPortal from '../body-portal/body-portal';
import Overlay from '../overlay/overlay';

import styles from './css/adjust-quantity.css';

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
  counterId: string,
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

  _popup: HTMLElement;
  _block: HTMLElement;

  componentDidUpdate(prevProps: Props) {
    if (this._block && this.props.isPopupShown && !prevProps.isPopupShown) {
      this.setMenuPosition(this._block);
    }
  }

  setMenuPosition(node: HTMLElement) {
    const parentDim = node.getBoundingClientRect();

    this._popup.style.width = `${node.offsetWidth}px`;
    this._popup.style.top = `${parentDim.top + parentDim.height + window.scrollY}px`;
    this._popup.style.left = `${parentDim.left}px`;
  }

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
  handleChange({ target }: Object) {
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
    const { counterId } = this.props;

    return (
      <div styleName="block" ref={ref => this._block = ref}>
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
        <BodyPortal>
          <div styleName="popup" className={popupState} ref={p => this._popup = p}>
            <div styleName="title">Adjust Quantity</div>
            <Counter
              counterId={counterId}
              value={this.state.diff}
              onBlur={evt => evt.stopPropagation()}
              onChange={(quantity) => this.adjustValue(quantity)}
            />
          </div>
        </BodyPortal>
      </div>
    );
  }
}
