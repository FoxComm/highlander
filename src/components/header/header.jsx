
// libs
import React, { PropTypes, Component } from 'react';
import { inflect } from 'fleck';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import NotificationBlock from '../activity-notifications/notification-block';
import DetailedInitials from '../user-initials/detailed-initials';
import Breadcrumb from './breadcrumb';
import UserMenu from './usermenu';
import * as userActions from '../../modules/user';
import { toggleUserMenu } from '../../modules/usermenu';

import type { TUser } from '../../modules/user';

import styles from './header.css';

const mapState = state => ({
  user: state.user.current,
  isMenuVisible: state.usermenu.isVisible,
});

@connect(mapState, { ...userActions, toggleUserMenu })
export default class Header extends React.Component {

  static propTypes = {
    routes: PropTypes.array,
    params: PropTypes.object,
    fetchUserInfo: PropTypes.func.isRequired,
    toggleUserMenu: PropTypes.func.isRequired,
    isMenuVisible: PropTypes.bool,
  };

  componentDidMount() {
    this.props.fetchUserInfo();
  }

  @autobind
  handleUserClick(e) {
    e.stopPropagation();
    this.props.toggleUserMenu();
  }

  render() {
    const props = this.props;
    const user:TUser = props.user;

    const name = (_.isEmpty(user) || user.name == null) ? '' : user.name.split(' ')[0];
    return (
      <header role='banner' styleName="header">
        <Breadcrumb routes={props.routes} params={props.params}/>
        <div styleName="sub-nav">
          <div styleName="notifications">
            <NotificationBlock />
          </div>
          <div styleName="user" onClick={this.handleUserClick}>
            <div styleName="initials"><DetailedInitials {...user} /></div>
            <div styleName="name">{name}</div>
            <div styleName="arrow">
              {props.isMenuVisible ? <i className="icon-chevron-up"/> : <i className="icon-chevron-down"/>}
            </div>
            {props.isMenuVisible && <UserMenu/>}
          </div>
        </div>
      </header>
    );
  };
}

