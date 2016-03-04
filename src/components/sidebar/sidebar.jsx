/* @flow */

import React from 'react';
import type { HTMLElement } from '../../types';
import cssModules from 'react-css-modules';
import styles from './sidebar.css';

import Icon from '../common/icon';

// type SidebarProps = {

// };

const Sidebar = () : HTMLElement => {
  return (
    <div styleName="sidebar">
      <div styleName="overlay"></div>
      <div styleName="container">
        <div styleName="controls">
          <Icon name="fc-close" />
        </div>
      </div>
    </div>
  );
};

export default cssModules(Sidebar, styles);
