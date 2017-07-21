/* @flow */

import React, { Children, Element } from 'react';
import classNames from 'classnames';

import Header from 'components/header/header';
import Footer from 'components/footer/footer';

import styles from './storefront.css';

import type { RoutesParams } from 'types';

type Props = RoutesParams & {
  children: Element<*>,
  location: any,
  params: Object,
};

const StoreFront = (props: Props) => {
  const childrenWithRoutes = Children.map(props.children,
    child => React.cloneElement(child, {
      routes: props.routes,
      routerParams: props.params,
    })
  );

  const containerClass = classNames(styles['content-container'], {
    [styles['full-width']]: Children.only(props.children).props.route.fullWidth,
  });

  const { location } = props;

  return (
    <div styleName="container">
      <Header
        path={location.pathname}
        query={location.query}
      />
      <div className={containerClass}>
        {childrenWithRoutes}
      </div>
      <Footer />
    </div>
  );
};

export default StoreFront;
