/* @flow */

// libs
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { dissoc } from 'sprout-data';
import ScrollContainer from 'react-router-scroll/lib/ScrollContainer';

// components
import Overlay from '../overlay/overlay';
import Auth from '../auth/auth';
import Header from 'components/header/header';

import * as actions from 'modules/auth';

import type { RoutesParams } from 'types';

import styles from './site.css';

function scrollHandler(prevRouterProps, { params, location }) {
  // do not scroll page to top if product type was selected in dropdown menu
  if (params.categoryName && params.productType) {
    return false;
  }

  // Do not scroll the page if query string has been changed
  if (prevRouterProps && prevRouterProps.location.pathname === location.pathname) {
    return false;
  }

  return true;
}

const mapState = state => ({
  isAuthBlockVisible: state.auth.isAuthBlockVisible,
  isContentOverlayVisible: state.contentOverlay.isVisible,
});

type Props = RoutesParams & {
  children: Array<any>,
  isContentOverlayVisible: boolean,
};

class Site extends Component {
  props: Props;

  renderAuthBlock() {
    const auth = this.props.location.query.auth;
    const pathname = this.props.location.pathname;
    const query = dissoc(this.props.location.query, 'auth');
    const path = {pathname, query};
    return (
      <Overlay path={path}>
        <Auth authBlockType={auth} path={path} />
      </Overlay>
    );
  }

  render() {
    const isAuthBlockVisible = this.props.location.query && this.props.location.query.auth;

    const childrenWithRoutes = React.Children.map(this.props.children,
      child => React.cloneElement(child, {
        routes: this.props.routes,
        routerParams: this.props.params,
        isContentOverlayVisible: this.props.isContentOverlayVisible,
      })
    );

    const { location } = this.props;

    return (
      <ScrollContainer scrollKey="site" shouldUpdateScroll={scrollHandler}>
        <div styleName="site" id="site">
          <Header
            path={location.pathname}
            query={location.query}
          />
          {isAuthBlockVisible && this.renderAuthBlock()}
          {childrenWithRoutes}
        </div>
      </ScrollContainer>
    );
  }
}

export default connect(mapState, actions)(Site);
