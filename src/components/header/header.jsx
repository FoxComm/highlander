
// libs
import React, {PropTypes} from 'react';
import { inflect } from 'fleck';
import { connect } from 'react-redux';
import _ from 'lodash';

// components
import NotificationBlock from '../activity-notifications/notification-block';
import DetailedInitials from '../users/detailed-initials';
import Breadcrumb from './breadcrumb';
import * as userActions from '../../modules/user';

import type { TUser } from '../../modules/user';

@connect(state => ({user: state.user.current}), userActions)
export default class Header extends React.Component {

  static propTypes = {
    routes: PropTypes.array,
    params: PropTypes.object,
    fetchUserInfo: PropTypes.func.isRequired,
  };

  componentDidMount() {
    this.props.fetchUserInfo();
  }

  render() {
    const props = this.props;
    const user:TUser = props.user;

    const name = (_.isEmpty(user) || user.name == null) ? '' : user.name.split(' ')[0];
    return (
      <header role='banner' className="fc-header">
        <Breadcrumb routes={props.routes} params={props.params}/>
        <div className="sub-nav">
          <NotificationBlock />
          <div className="fc-header__initials"><DetailedInitials {...user} /></div>
          <div className="fc-header__name">{name}</div>
          <div className="sort"><i className="icon-chevron-down"/></div>
        </div>
      </header>
    );
  };
}

