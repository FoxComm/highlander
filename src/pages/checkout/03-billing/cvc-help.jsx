/* @flow */

// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';

// styles
import styles from './cvc-help.css';

type State = {
  popupVisible: boolean,
};

export default class CvcHelp extends Component {

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
        <div styleName="cvc-images">
          <img src="/images/CVV_Visa.png" />
          <img src="/images/CVV_American_Express.png" />
        </div>
      </div>
    );
  }

  render() {
    return (
      <div styleName="cvc-help" onMouseEnter={this.showPopup} onMouseLeave={this.hidePopup}>
        ?
        {this.popup}
      </div>
    );
  }
}
