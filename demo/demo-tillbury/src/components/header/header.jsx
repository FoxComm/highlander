/* @flow */

import get from 'lodash/get';
import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router';

// actions
import { toggleSidebar } from 'modules/sidebar';

// components
import ActionLink from 'ui/action-link/action-link';
import UserTools from 'components/usertools/usertools';
import Navigation from 'components/navigation/navigation';
import Sidebar from 'components/sidebar/sidebar';
import Logo from 'components/logo/logo';

import styles from './header.css';

type Props = {
  toggleSidebar: () => any,
  path: string,
  query: ?Object,
};

const Header = (props: Props) => {
  const menuIcon = {
    name: !props.sidebarVisible ? 'fc-hamburger' : 'fc-close',
    size: 'm',
  };

  return (
    <div styleName="header">
      <div styleName="header-wrap">
        <div styleName="nav-search-logo-wrapper">
          <Link to="/" styleName="logo-link">
            <Logo styleName="logo" />
          </Link>

          <div styleName="wrap">
            {/* <ActionLink
            action={props.toggleSidebar}
            icon={menuIcon}
            styleName="action-link-menu"
            />
          <Link to="/" styleName="logo-link-mobile">
            <Logo styleName="logo" />
          </Link> */}
            <div styleName="tools">
              <UserTools path={props.path} query={props.query} />
            </div>
          </div>

          <div styleName="flourish" />
          <div styleName="flourish-underline" />
        </div>
      </div>

      <div styleName="navigation-wrap">
        <div styleName="navigation">
          <Navigation path={props.path} />
        </div>
      </div>

      <div styleName="search-container" />

      <div styleName="mobile-sidebar">
        <Sidebar path={props.path} />
      </div>
    </div>
  );
};

const mapState = state => ({
  sidebarVisible: get(state.sidebar, 'isVisible', false),
});

export default connect(mapState, {
  toggleSidebar,
})(Header);
