/* @flow */

import React from 'react';
import classNames from 'classnames';

import Navigation from './navigation';

import { connect } from 'react-redux';
import * as SiteMenuActions from '../../modules/site-menu';

type Props = {
  routes: Array<Object>,
}

const Sidebar = ({ routes }: Props) => {
  const sidebarClass = classNames('fc-sidebar', '_open');

  return (
    <aside role="complimentary" className={sidebarClass}>
      <Navigation routes={routes} />
    </aside>
  );
};

const mapState = state => ({
  ...state.siteMenu,
});

export default connect(mapState, SiteMenuActions)(Sidebar);
