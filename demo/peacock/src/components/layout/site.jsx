/* @flow */

import React, { Component } from 'react';
import { connect } from 'react-redux';
import { dissoc } from 'sprout-data';

import * as actions from 'modules/auth';

import styles from './site.css';

import Overlay from '../overlay/overlay';
import Auth from '../auth/auth';

import type { RoutesParams } from 'types';

const mapState = state => ({
  isAuthBlockVisible: state.auth.isAuthBlockVisible,
});

type Props = RoutesParams & {
  children: Array<any>,
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
      })
    );

    return (
      <div styleName="site">
        {isAuthBlockVisible && this.renderAuthBlock()}
        {childrenWithRoutes}
      </div>
    );
  }
}

export default connect(mapState, actions)(Site);
