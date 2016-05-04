/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';

import Header from '../header/header';
import Sidebar from '../sidebar/sidebar';
import Footer from '../footer/footer';
import Cart from '../cart/cart';

import styles from './storefront.css';

type StoreFrontProps = {
  children: HTMLElement;
  location: any;
}

const StoreFront = (props : StoreFrontProps) : HTMLElement => {
  return (
    <div styleName="container">
      <Header path={props.location.pathname} />
      <div styleName="content-container">
        <div styleName="storefront">
          {props.children}
        </div>
      </div>
      <div styleName="footer">
        <Footer />
      </div>
      <div styleName="mobile-sidebar">
        <Sidebar path={props.location.pathname} />
      </div>
      <div>
        <Cart />
      </div>
    </div>
  );
};

export default StoreFront;
