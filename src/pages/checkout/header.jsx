/* @flow */

// libs
import React from 'react';
import cx from 'classnames';

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
  currentStage: number,
};

const Header = (props: Props) => {
  const headerStyle = props.isScrolled ? 'header-scrolled' : 'header';

  const navItems = [
    ['Shipping', props.setShippingStage],
    ['Delivery', props.setDeliveryStage],
    ['Billing', props.setBillingState],
  ];

  const navList = navItems.map(([title, callback], i) => {
    const className = cx(styles['nav-item'], {
      [styles.active]: i === props.currentStage,
    });

    return (
      <li className={className} key={title}>
        <a onClick={callback}>{title}</a>
      </li>
    );
  });

  const nav = (
    <nav styleName="navigation">
      <ol styleName="nav-list">
        {navList}
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
