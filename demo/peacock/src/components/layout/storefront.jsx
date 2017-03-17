/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';
import { connect } from 'react-redux';
import classNames from 'classnames';

import Header from '../header/header';
import Footer from '../footer/footer';
import * as actions from 'modules/banner';

import styles from './storefront.css';

import type { RoutesParams } from 'types';

type Props = RoutesParams & {
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

  const childrenWithRoutes = React.Children.map(props.children,
    (child) => React.cloneElement(child, {
      routes: props.routes,
      routerParams: props.params,
    })
  );

  return (
    <div styleName="container">
      <Header
        path={props.location.pathname}
        query={props.location.query}
      />
      <div className={bodyClass}>
        {childrenWithRoutes}
      </div>
      <Footer />
    </div>
  );
};

export default connect(mapState, actions)(StoreFront);
