/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';

import Header from '../header/header';
import Sidebar from '../sidebar/sidebar';
import Footer from '../footer/footer';
import Cart from '../cart/cart';

import styles from './storefront.css';

type Props = {
  children: HTMLElement;
  location: any;
}

const StoreFront = (props: Props) => {
  return (
    <div styleName="container">
      <Header path={props.location.pathname} query={props.location.query}/>
      <div styleName="content-container">
        {props.children}
      </div>
      <Footer />
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
