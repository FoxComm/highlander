/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import classNames from 'classnames';
import { logout } from 'modules/auth';
import { fetch as fetchCart } from 'modules/cart';
import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

import { isAuthorizedUser } from 'paragons/auth';

import styles from './sidebar.css';

import Icon from 'ui/icon';
import Categories from '../navigation/navigation';
import Search from '../search/search';

import * as actions from 'modules/sidebar';
import { resetTerm } from 'modules/search';

type SidebarProps = Localized & {
  isVisible: boolean,
  toggleSidebar: Function,
  resetTerm: Function,
  path: string,
};

const Sidebar = (props: SidebarProps): HTMLElement => {
  const sidebarClass = classNames({
    'sidebar-hidden': !props.isVisible,
    'sidebar-shown': props.isVisible,
  });

  const changeCategoryCallback = () => {
    props.resetTerm();
  };

  const { t } = props;

  const handleLogout = e => {
    e.preventDefault();
    props.logout().then(() => {
      props.fetchCart();
    });
  };

  const onLinkClick = e => {
    if (e.target.tagName === 'A') {
      props.toggleSidebar();
    }
  };

  const userAuthorized = isAuthorizedUser(props.user);

  const renderSessionLink = userAuthorized ? (
    <a styleName="session-link" onClick={handleLogout}>
      {t('LOG OUT')}
    </a>
  ) : (
    <Link
      styleName="session-link"
      to={{pathname: props.path, query: {auth: 'LOGIN'}}}
    >
      {t('LOG IN')}
    </Link>
  );

  const myProfileLink = userAuthorized ? (
    <Link
      to="/profile"
      styleName="session-link"
      activeClassName={styles['active-link']}
    >
      PROFILE
    </Link>
  ) : null;

  return (
    <div styleName={sidebarClass}>
      <div styleName="overlay" onClick={props.toggleSidebar}></div>
      <div styleName="container">
        <div styleName="controls">
          <div styleName="controls-close">
            <a styleName="close-button" onClick={props.toggleSidebar}>
              <Icon name="fc-close" className="close-icon"/>
            </a>
          </div>
          <div styleName="controls-search">
            <Search onSearch={props.toggleSidebar} isActive/>
          </div>
          <div styleName="links-group" onClick={onLinkClick}>
            <div styleName="controls-categories">
              <Categories
                onClick={changeCategoryCallback}
                path={props.path}
              />
            </div>
            <div styleName="controls-session">
              {myProfileLink}
            </div>
            <div styleName="controls-session">
              {renderSessionLink}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

const mapStates = state => ({
  ...state.sidebar,
  ...state.auth,
});

export default connect(mapStates, {
  ...actions,
  resetTerm,
  logout,
  fetchCart,
})(localized(Sidebar));
