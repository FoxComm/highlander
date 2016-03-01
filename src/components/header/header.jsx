
// libs
import React, {PropTypes} from 'react';
import { inflect } from 'fleck';

// components
import NotificationBlock from '../activity-notifications/notification-block';
import Initials from '../users/initials';
import Breadcrumb from './breadcrumb';

export const currentUser = {id: 1, name: 'Frankly Admin', email: 'admin@admin.com'};

const Header = props => {
  const name = currentUser.name.split(' ')[0];
  return (
    <header role='banner' className="fc-header">
      <Breadcrumb routes={props.routes} params={props.params}/>
      <div className="sub-nav">
        <NotificationBlock />
        <div className="fc-header__initials"><Initials {...currentUser} /></div>
          <div className="fc-header__name">{name}</div>
          <div className="sort"><i className="icon-chevron-down"></i></div>
      </div>
    </header>
  );
};

Header.propTypes = {
  routes: PropTypes.array,
  params: PropTypes.object
};

export default Header;
