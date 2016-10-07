/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import classNames from 'classnames';
import { logout } from 'modules/auth';
import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

import styles from './sidebar.css';

import Icon from 'ui/icon';
import Categories from '../navigation/navigation';
import Search from '../search/search';

import * as actions from 'modules/sidebar';
import { resetTerm } from 'modules/search';

type SidebarProps = Localized & {
  isVisible: boolean;
  toggleSidebar: Function;
  resetTerm: Function;
  path: string;
};

const Sidebar = (props: SidebarProps): HTMLElement => {
  const sidebarClass = classNames({
    'sidebar-hidden': !props.isVisible,
    'sidebar-shown': props.isVisible,
  });

  const changeCategoryCallback = () => {
    props.toggleSidebar();
    props.resetTerm();
  };

  const { t } = props;

  const handleLogout = e => {
    e.stopPropagation();
    e.preventDefault();
    props.logout();
  };

  const renderSessionLink = props.user ? (
    <a styleName="session-link" onClick={handleLogout}>
      {t('LOG OUT')}
    </a>
  ) : (
    <Link styleName="session-link" to={{pathname: props.path, query: {auth: 'LOGIN'}}}>
      {t('LOG IN')}
    </Link>
  );

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
          <div styleName="controls-categories">
            <Categories onClick={changeCategoryCallback} />
          </div>
          <div styleName="controls-session">
            {renderSessionLink}
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
})(localized(Sidebar));
