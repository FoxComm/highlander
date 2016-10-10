/* @flow */

// libs
import React from 'react';

// styles
import styles from './header.css';

// components
import { Link } from 'react-router';
import Icon from 'ui/icon';

const Header = () => {
  return (
    <header styleName="header">
      <div styleName="logo">
        <Link to="/">
          <Icon styleName="logo" name="fc-logo"/>
        </Link>
      </div>
      <h1 styleName="title">Checkout</h1>
      <nav styleName="navigation">
        <ol styleName="nav-list">
          <li styleName="nav-item"><a href="">Shipping</a></li>
          <li styleName="nav-item"><a href="">Delivery</a></li>
          <li styleName="nav-item"><a href="">Billing</a></li>
        </ol>
      </nav>
    </header>
  );
};

export default Header;
