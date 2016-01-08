
// libs
import React, {PropTypes} from 'react';
import { inflect } from 'fleck';

// components
import NotificationBlock from '../activity-notifications/notification-block';
import Initials from '../users/initials';
import Breadcrumb from './breadcrumb';

const currentUser = {name: 'Frankly Admin', email: 'admin@admin.com'};

export default class Header extends React.Component {

  render() {
    const name = currentUser.name.split(' ')[0];
    return (
      <header role='banner' className="fc-header">
        <Breadcrumb routes={this.props.routes} params={this.props.params}/>
        <div className="sub-nav">
          <NotificationBlock />
          <div className="fc-header__initials"><Initials {...currentUser} /></div>
          <div className="fc-header__name">{name}</div>
          <div className="sort"><i className="icon-chevron-down"></i></div>
        </div>
      </header>
    );
  }
}
