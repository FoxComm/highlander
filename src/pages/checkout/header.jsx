/* @flow */

// libs
import React from 'react';

// styles
import styles from './header.css';

// components
import { Link } from 'react-router';
import Icon from 'ui/icon';

type Props = {
  isScrolled: boolean,
  setShippingStage: Function,
  setDeliveryStage: Function,
  setBillingState: Function,
  isGuestAuth: boolean,
};

const Header = (props: Props) => {
  const headerStyle = props.isScrolled ? 'header-scrolled' : 'header';

  const nav = (
    <nav styleName="navigation">
      <ol styleName="nav-list">
        <li styleName="nav-item"><a onClick={props.setShippingStage}>Shipping</a></li>
        <li styleName="nav-item"><a onClick={props.setDeliveryStage}>Delivery</a></li>
        <li styleName="nav-item"><a onClick={props.setBillingState}>Billing</a></li>
      </ol>
    </nav>
  );

  const checkout = (
    <div styleName="navigation-checkout">
      <span styleName="checkout-title">Checkout</span>
    </div>
  );

  return (
    <header styleName={headerStyle}>
      <div styleName="logo">
        <Link to="/">
          <Icon styleName="logo-icon" name="fc-logo"/>
        </Link>
      </div>
      <h1 styleName="title">Checkout</h1>
      {props.isGuestAuth ? checkout : nav}
    </header>
  );
};

export default Header;
