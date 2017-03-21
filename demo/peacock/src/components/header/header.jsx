/* @flow */

import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router';

import { toggleSidebar } from 'modules/sidebar';

import styles from './header.css';

import Icon from 'ui/icon';
import Search from '../search/search';
import UserTools from '../usertools/usertools';
import Navigation from '../navigation/navigation';
import Cart from 'components/cart/cart';
import Sidebar from 'components/sidebar/sidebar';

type Props = {
  toggleSidebar: Function,
  path: string,
  query: ?Object,
};

class Header extends React.Component {
  props: Props;

  render() {
    return (
      <div>
        <div styleName="header">
          <div styleName="wrap">
            <div styleName="hamburger" onClick={this.props.toggleSidebar}>
              Menu
            </div>
            <div styleName="nav-search-logo-wrapper">
              <Link to="/" styleName="logo-link">
                <Icon styleName="logo" name="fc-logo" />
              </Link>
              <div styleName="navigation">
                <Navigation path={this.props.path} />
              </div>
              <div styleName="search">
                <Search />
              </div>
            </div>
            <div styleName="tools">
              <UserTools path={this.props.path} query={this.props.query} />
            </div>
          </div>
        </div>

        <Cart />

        <div styleName="mobile-sidebar">
          <Sidebar path={this.props.path} />
        </div>

      </div>
    );
  }
}


export default connect(void 0, {
  toggleSidebar,
})(Header);
