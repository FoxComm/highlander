/* @flow */

import _ from 'lodash';
import React from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

import Navigation from './navigation';

import { connect } from 'react-redux';
import * as SiteMenuActions from '../../modules/site-menu';

type Props = {
  routes: Array<Object>
}

@connect((state, props) => ({
  ...state.siteMenu
}), SiteMenuActions)

class Sidebar extends React.Component {
  props: Props;
  
  render() {
    const sidebarClass = classNames('fc-sidebar', '_open');

    return (
      <aside role="complimentary" className={sidebarClass}>
        <Navigation routes={this.props.routes} />
      </aside>
    );
  }
}

export default Sidebar;
