/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';
import { connect } from 'react-redux';
import classNames from 'classnames';
import styles from './sidebar.css';

import Icon from 'ui/icon';
import Categories from '../categories/categories';
import Search from '../search/search';

import * as actions from 'modules/sidebar';
import { resetTerm } from 'modules/search';
import { authBlockToggle } from 'modules/auth';

type SidebarProps = {
  isVisible: boolean;
  toggleSidebar: Function;
  resetTerm: Function;
  authBlockToggle: Function;
};

const getState = state => ({ ...state.sidebar });

const Sidebar = (props: SidebarProps): HTMLElement => {
  const sidebarClass = classNames({
    'sidebar-hidden': !props.isVisible,
    'sidebar-shown': props.isVisible,
  });

  const changeCategoryCallback = () => {
    props.toggleSidebar();
    props.resetTerm();
  };

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
          <div styleName="controls-login">
            <a styleName="login-link" onClick={props.authBlockToggle}>LOG IN</a>
          </div>
        </div>
      </div>
    </div>
  );
};

export default connect(getState, {...actions, resetTerm, authBlockToggle})(Sidebar);
