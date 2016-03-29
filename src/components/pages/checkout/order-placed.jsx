
import React, { Component } from 'react';
import styles from './order-placed.css';
import { browserHistory } from 'react-router';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

import Button from 'ui/buttons';
import { fetch as fetchCart } from 'modules/cart';
import { resetCheckout } from 'modules/checkout';

@connect(state => state.cart, {fetchCart, resetCheckout})
class OrderPlaced extends Component {
  @autobind
  toHome() {
    browserHistory.push('/');
  }

  componentWillUnmount() {
    this.props.fetchCart();
    this.props.resetCheckout();
  }

  render() {
    const props = this.props;

    return (
      <div styleName="order-placed">
        <div styleName="header">Thanks for your order!</div>
        <div styleName="order-number">Order Number: {props.referenceNumber}</div>
        <div styleName="desc">
          Looks like you'll be getting some cool stuff soon. In the meantime, we're sending you an
          email with your order information.
        </div>
        <Button styleName="to-home" onClick={this.toHome}>Take me home</Button>
      </div>
    );
  }
}

export default OrderPlaced;
