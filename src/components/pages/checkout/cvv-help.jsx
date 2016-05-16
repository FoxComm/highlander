/* @flow */

import React, { Component } from 'react';
import { autobind } from 'core-decorators';

import styles from './cvv-help.css';

import Icon from 'ui/icon';

type State = {
  popupVisible: boolean;
}

export default class CvvHelp extends Component {

  state: State = {
    popupVisible: false,
  };

  @autobind
  showPopup() {
    this.setState({
      popupVisible: true,
    });
  }

  @autobind
  hidePopup() {
    this.setState({
      popupVisible: false,
    });
  }

  get popup() {
    if (! this.state.popupVisible) return null;

    return (
      <div styleName="popup">
        <p>
          For Visa, Master Card and Discover (left), the 3 digits on the back of the card.
        </p>
        <p>
          For American Express (right), the 4 digits on the front of the card.
        </p>
        <div styleName="cvv-images">
          <Icon name="fc-cvv-visa" />
          <Icon name="fc-cvv-amex" />
        </div>
      </div>
    );
  }

  render() {
    return (
      <div styleName="cvv-help" onMouseEnter={this.showPopup} onMouseLeave={this.hidePopup}>
        ?
        {this.popup}
      </div>
    );
  }
}
