/* @flow */

import React, { Element } from 'react';
import classNames from 'classnames';

import Footer from '../footer/footer';

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

  const overlayClass = classNames(styles['content-container-overlay'], {
    [styles['_with-overlay']]: props.isContentOverlayVisible,
  });

  return (
    <div styleName="container">
      <div styleName="content-container">
        {childrenWithRoutes}
      </div>
      <Footer />
      <div className={overlayClass} />
    </div>
  );
};

export default StoreFront;
