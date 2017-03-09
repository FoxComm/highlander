/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';
import { connect } from 'react-redux';
import classNames from 'classnames';

import Header from '../header/header';
import Sidebar from '../sidebar/sidebar';
import Footer from '../footer/footer';
import Cart from '../cart/cart';

import * as actions from 'modules/banner';

import styles from './storefront.css';

type Props = {
  children: HTMLElement,
  location: any,
  banner: {
    isVisible: boolean,
  },
  closeBanner: Function,
};

const mapState = state => ({
  banner: state.banner,
});

const StoreFront = (props: Props) => {
  const bodyClass = classNames(styles['content-container'], {
    [styles['_without-banner']]: !props.banner.isVisible,
  });

  return (
    <div styleName="container">
      <Header
        path={props.location.pathname}
        query={props.location.query}
        isBannerVisible={props.banner.isVisible}
        closeBanner={props.closeBanner}
      />
      <div className={bodyClass}>
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

export default connect(mapState, actions)(StoreFront);
