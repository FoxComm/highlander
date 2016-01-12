
import React from 'react';
import static_url from '../../lib/s3';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

import Navigation from './navigation';

export default class Sidebar extends React.Component {

  constructor(...args) {
    super(...args);
    this.state = {
      open: false
    };
  }

  @autobind
  toggleSidebar() {
    const status = !this.state.open;
    this.setState({open: status});
  }

  render() {
    const indicatorClass = classNames({
      'icon-chevron-left': this.state.open,
      'icon-chevron-right': !this.state.open
    });
    const sidebarClass = classNames('fc-sidebar', {
      '_open': this.state.open
    });
    const controlClass = classNames('fc-sidebar__control', {
      '_open': this.state.open
    });
    return (
      <aside role="complimentary" className={sidebarClass}>
        <div className="logo">
          <img src={static_url('images/fc-logo-nav.svg')}></img>
        </div>
        <div className={controlClass} onClick={this.toggleSidebar}>
          <i className={indicatorClass}></i>
        </div>
        <Navigation open={this.state.open}/>
      </aside>
    );
  }
}
