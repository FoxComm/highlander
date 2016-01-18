
import React from 'react';
import static_url from '../../lib/s3';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

import Navigation from './navigation';

import { connect } from 'react-redux';
import * as SiteMenuActions from '../../modules/site-menu';

@connect((state, props) => ({
  ...state.siteMenu
}), SiteMenuActions)
export default class Sidebar extends React.Component {

  @autobind
  toggleSidebar() {
    this.props.toggleSiteMenu();
  }

  render() {
    const indicatorClass = classNames({
      'icon-chevron-left': this.props.isMenuExpanded,
      'icon-chevron-right': !this.props.isMenuExpanded
    });
    const sidebarClass = classNames('fc-sidebar', {
      '_open': this.props.isMenuExpanded
    });
    const controlClass = classNames('fc-sidebar__control', {
      '_open': this.props.isMenuExpanded
    });
    return (
      <aside role="complimentary" className={sidebarClass}>
        <div className="logo">
          <img src={static_url('images/fc-logo-nav.svg')}></img>
        </div>
        <div className={controlClass} onClick={this.toggleSidebar}>
          <i className={indicatorClass}></i>
        </div>
        <Navigation routes={this.props.routes} collapsed={!this.props.isMenuExpanded}/>
      </aside>
    );
  }
}
