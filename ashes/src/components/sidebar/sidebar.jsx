/* @flow */

import React from 'react';
import classNames from 'classnames';

import Navigation from './navigation';

import { connect } from 'react-redux';
import * as SiteMenuActions from '../../modules/site-menu';

type Params = { [key: string]: string };

type Props = {
  routes: Array<Object>,
  params: Params,
  className?: string,
};

const Sidebar = ({ routes, params, className }: Props) => {
  const sidebarClass = classNames('fc-sidebar', '_open', className);

  return (
    <aside role="complimentary" className={sidebarClass}>
      <Navigation
        routes={routes}
        params={params}
      />
    </aside>
  );
};

const mapState = state => ({
  ...state.siteMenu,
});

export default connect(mapState, SiteMenuActions)(Sidebar);
