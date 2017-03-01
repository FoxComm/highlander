
import _ from 'lodash';
import React, { PropTypes } from 'react';
import static_url from '../../lib/s3';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

import Navigation from './navigation';

import { connect } from 'react-redux';
import * as SiteMenuActions from '../../modules/site-menu';

import styles from './navigation.css';

@connect((state, props) => ({
  ...state.siteMenu
}), SiteMenuActions)
export default class Sidebar extends React.Component {

  static propTypes = {
    isMenuExpanded: PropTypes.bool,
    menuItems: PropTypes.object,
    routes: PropTypes.array.isRequired,
    toggleMenuItem: PropTypes.func.isRequired,
    toggleSiteMenu: PropTypes.func.isRequired,
    resetManuallyChanged: PropTypes.func.isRequired
  };

  static defaultProps = {
    isMenuExpanded: true
  };

  componentWillReceiveProps(newProps) {
    if (!_.isEqual(this.props.routes, newProps.routes)) {
      this.props.resetManuallyChanged();
    }
  }

  @autobind
  toggleSidebar() {
    this.props.toggleSiteMenu();
  }

  render() {
    const indicatorClass = classNames({
      'icon-chevron-left': this.props.isMenuExpanded,
      'icon-chevron-right': !this.props.isMenuExpanded
    });
    const sidebarClass = classNames('fc-sidebar', styles.root, {
      '_open': this.props.isMenuExpanded
    });
    const controlClass = classNames('fc-sidebar__control', {
      '_open': this.props.isMenuExpanded
    });
    return (
      <aside role="complimentary" className={sidebarClass}>
        <div className="logo">
          <img src={static_url('images/fc-logo-nav.svg')} />
        </div>
        <div className={controlClass} onClick={this.toggleSidebar}>
          <i className={indicatorClass} />
        </div>
        <Navigation routes={this.props.routes}
                    collapsed={!this.props.isMenuExpanded}
                    menuItems={this.props.menuItems}
                    toggleMenuItem={this.props.toggleMenuItem} />
      </aside>
    );
  }
}
