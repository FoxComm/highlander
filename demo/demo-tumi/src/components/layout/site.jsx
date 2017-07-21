/* @flow */

// libs
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { dissoc } from 'sprout-data';
import { autobind } from 'core-decorators';

// components
import Auth from '../auth/auth';
// import Footer from 'components/footer/footer';

import * as actions from 'modules/auth';

import type { RoutesParams } from 'types';

import styles from './site.css';

const mapState = state => ({
  isAuthBlockVisible: state.auth.isVisible, // || props.location.query && props.location.query.auth,
});

type Props = RoutesParams & {
  children: Array<any>,
  isAuthBlockVisible: boolean,
};

class Site extends Component {
  props: Props;
  scrollListener: Function;
  node: Object;

  state = {};

  // @autobind
  updateScrollPosition(node) {
    const { scrollTop } = node;

    this.setState({ scrollTop });
  }

  @autobind
  addScrollListener(node) {
    this.node = node;
    this.scrollListener = this.updateScrollPosition.bind(this, node);
    node.addEventListener('scroll', this.scrollListener);
    this.updateScrollPosition(node);
  }

  componentWillUnmount() {
    this.node.removeEventListener('scroll', this.scrollListener);
  }

  get auth() {
    const auth = this.props.location.query.auth;
    const pathname = this.props.location.pathname;
    const query = dissoc(this.props.location.query, 'auth');
    const path = { pathname, query };

    return (
      <Auth
        toggleMenu={this.props.toggleAuthMenu}
        isVisible={this.props.isAuthBlockVisible}
        authBlockType={auth}
        path={path}
      />
    );
  }

  render() {
    const childrenWithRoutes = React.Children.map(this.props.children,
      child => React.cloneElement(child, {
        routes: this.props.routes,
        routerParams: this.props.params,
        scrollTop: this.state.scrollTop,
      })
    );

    return (
      <div styleName="site" id="site" ref={this.addScrollListener}>
        {this.auth}
        {childrenWithRoutes}
      </div>
    );
  }
}

export default connect(mapState, actions)(Site);
