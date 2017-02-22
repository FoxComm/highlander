/* @flow weak */

// libs
import React, { PropTypes, Component, Element } from 'react';
import { inflect } from 'fleck';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import NotificationBlock from '../activity-notifications/notification-block';
import DetailedInitials from '../user-initials/detailed-initials';
import Breadcrumb from './breadcrumb';
import UserMenu from './usermenu';
import * as userActions from 'modules/user';
import { toggleUserMenu } from 'modules/usermenu';

import type { TUser } from 'modules/user';

import styles from './header.css';

type Props = {
  routes: Array<any>,
  params: Object,
  fetchUserInfo: Function,
  toggleUserMenu: Function,
  isMenuVisible?: boolean,
  user: ?TUser,
};

const mapStateToProps = state => ({
  user: state.user.current,
  isMenuVisible: state.usermenu.isVisible,
});

@connect(mapStateToProps, { ...userActions, toggleUserMenu })
export default class Header extends React.Component {
  props: Props;

  componentDidMount(): void {
    this.props.fetchUserInfo();
  }

  @autobind
  handleUserClick(e): void {
    e.stopPropagation();
    this.props.toggleUserMenu();
  }

  get initials(): ?Element<*> {
    const { user } = this.props;
    if (user) {
      return <DetailedInitials {...user} />;
    }
  }

  render() {
    const props = this.props;
    const user: ?TUser = props.user;

    const name = (user == null || _.isEmpty(user) || user.name == null) ? '' : user.name.split(' ')[0];
    return (
      <header role='banner' styleName="header">
        <Breadcrumb routes={props.routes} params={props.params}/>
        <div styleName="sub-nav">
          <div styleName="notifications">
            <NotificationBlock />
          </div>
          <div styleName="user" onClick={this.handleUserClick}>
            <div styleName="initials">{this.initials}</div>
            <div styleName="name">{name}</div>
            <div id="user-menu-btn" styleName="arrow">
              {props.isMenuVisible ? <i className="icon-chevron-up"/> : <i className="icon-chevron-down"/>}
            </div>
            {props.isMenuVisible && <UserMenu user={props.user}/>}
          </div>
        </div>
      </header>
    );
  };
}

