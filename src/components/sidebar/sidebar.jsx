/* @flow */

import React from 'react';
import type { HTMLElement } from '../../types';
import { connect } from 'react-redux';
import classNames from 'classnames';
import cssModules from 'react-css-modules';
import styles from './sidebar.css';

// import Icon from '../common/icon';
import Categories from '../categories/categories';

import * as actions from '../../modules/sidebar';

type SidebarProps = {
  isVisible: boolean;
  toggleSidebar: Function;
};

const getState = state => ({ ...state.sidebar });

const Sidebar = (props : SidebarProps) : HTMLElement => {
  const sidebarClass = classNames({
    'sidebar-hidden': !props.isVisible,
    'sidebar-shown': props.isVisible,
  });

  const changeCategoryCallback = () => {
    props.toggleSidebar();
  };

  return (
    <div styleName={sidebarClass}>
      <div styleName="overlay" onClick={props.toggleSidebar}></div>
      <div styleName="container">
        <div styleName="controls">
          <div styleName="controls-close">
            <a styleName="close-icon" onClick={props.toggleSidebar}>
              X {/* <Icon name="fc-close"/> */}
            </a>
          </div>
          <div styleName="controls-categories">
            <Categories onClick={changeCategoryCallback} />
          </div>
        </div>
      </div>
    </div>
  );
};

export default connect(getState, actions)(cssModules(Sidebar, styles));
