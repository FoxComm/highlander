/* @flow */

import React, { Element } from 'react';

import Header from '../header/header';
import Footer from '../footer/footer';

import styles from './storefront.css';

import type { RoutesParams } from 'types';

type Props = RoutesParams & {
  children: Element<*>,
  location: any,
};

const StoreFront = (props: Props) => {
  const childrenWithRoutes = React.Children.map(props.children,
    child => React.cloneElement(child, {
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
      <div styleName="content-container">
        {childrenWithRoutes}
      </div>
      <Footer />
    </div>
  );
};

export default StoreFront;
