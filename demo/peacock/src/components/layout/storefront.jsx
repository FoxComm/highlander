/* @flow */

import React, { Element } from 'react';
import classNames from 'classnames';

import Footer from 'components/footer/footer';
import Overlay from 'ui/overlay/overlay';

import styles from './storefront.css';

import type { RoutesParams } from 'types';

type Props = RoutesParams & {
  children: Element<*>,
  location: any,
  isContentOverlayVisible: boolean,
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
      <div styleName="content-container">
        {childrenWithRoutes}
      </div>
      <Footer />
      <Overlay shown={props.isContentOverlayVisible} />
    </div>
  );
};

export default StoreFront;
