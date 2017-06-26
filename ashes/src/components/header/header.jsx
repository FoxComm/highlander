/* @flow weak */

// libs
import React, { Element } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import NotificationBlock from 'components/activity-notifications';
import DetailedInitials from 'components/user-initials/detailed-initials';
import Breadcrumb from './breadcrumb';
import UserMenu from './usermenu';
import * as userActions from 'modules/user';
import { toggleUserMenu } from 'modules/usermenu';
import SvgIcon from 'components/core/svg-icon';
import Icon from 'components/core/icon';

import type { TUser } from 'modules/user';

import styles from './header.css';

type Props = {
  routes: Array<any>,
  params: Object,
  fetchUserInfo: Function,
  toggleUserMenu: Function,
  isMenuVisible?: boolean,
  user: ?TUser,
  className?: string,
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

  render(): Element<*> {
    const { routes, params, isMenuVisible, user, className } = this.props;
    const name = user == null || _.isEmpty(user) || user.name == null ? '' : user.name.split(' ')[0];

    return (
      <header role="banner" styleName="header" className={className} name="">
        <div styleName="logo">
          <SvgIcon name="logo" className={styles['logo-icon']} />
        </div>
        <div styleName="top-nav-menu">
          <Breadcrumb routes={routes} params={params} />
          <div styleName="sub-nav">
            <NotificationBlock />
            <div styleName="user" onClick={this.handleUserClick}>
              <div styleName="initials">{this.initials}</div>
              <div styleName="name">{name}</div>
              <div id="fct-user-menu-btn" styleName="arrow">
                {isMenuVisible ? <Icon name="chevron-up" /> : <Icon name="chevron-down" />}
              </div>
              {isMenuVisible && <UserMenu user={user} />}
            </div>
          </div>
        </div>
      </header>
    );
  }
}
