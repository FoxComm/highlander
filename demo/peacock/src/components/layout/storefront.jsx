/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';
import { connect } from 'react-redux';
import classNames from 'classnames';

import Header from '../header/header';
import Footer from '../footer/footer';
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
      />
      <div className={bodyClass}>
        {props.children}
      </div>
      <Footer />
    </div>
  );
};

export default connect(mapState, actions)(StoreFront);
