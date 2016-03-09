
// libs
import React, {PropTypes} from 'react';
import { inflect } from 'fleck';
import { connect } from 'react-redux';

// components
import NotificationBlock from '../activity-notifications/notification-block';
import DetailedInitials from '../users/detailed-initials';
import Breadcrumb from './breadcrumb';

const Header = props => {
  const currentUser = props.user;

  const name = currentUser ? currentUser.name.split(' ')[0] : '';
  return (
    <header role='banner' className="fc-header">
      <Breadcrumb routes={props.routes} params={props.params}/>
      <div className="sub-nav">
        <NotificationBlock />
        <div className="fc-header__initials"><DetailedInitials {...currentUser} /></div>
          <div className="fc-header__name">{name}</div>
          <div className="sort"><i className="icon-chevron-down"/></div>
      </div>
    </header>
  );
};

Header.propTypes = {
  routes: PropTypes.array,
  params: PropTypes.object
};

export default connect((state) => ({user: state.user}))(Header);
