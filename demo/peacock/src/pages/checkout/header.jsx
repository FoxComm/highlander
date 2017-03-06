/* @flow */

// libs
import React from 'react';
import classNames from 'classnames';

// styles
import styles from './header.css';

// components
import { Link } from 'react-router';
import Icon from 'ui/icon';

import { EditStages } from 'modules/checkout';

type Props = {
  isScrolled: boolean,
  setShippingStage: Function,
  setDeliveryStage: Function,
  setBillingStage: Function,
  isGuestAuth: boolean,
  currentStage: number,
};

const Header = (props: Props) => {
  const headerStyle = props.isScrolled ? 'header-scrolled' : 'header';

  const navItems = [
    ['Shipping', props.setShippingStage, EditStages.SHIPPING],
    ['Delivery', props.setDeliveryStage, EditStages.DELIVERY],
    ['Billing', props.setBillingStage, EditStages.BILLING],
  ];

  const navList = navItems.map(([title, callback, stage], i) => {
    const className = classNames(styles['nav-item'], {
      [styles.active]: i === props.currentStage,
    });

    const checkedCallback = () => {
      if (props.currentStage >= stage) {
        callback();
      }
    };

    return (
      <li className={className} key={title}>
        <a onClick={checkedCallback}>{title}</a>
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
