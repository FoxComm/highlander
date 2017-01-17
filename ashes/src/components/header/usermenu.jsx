/* @flow */

import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { toggleUserMenu } from '../../modules/usermenu';
import { logout, authMessage, INFO_MESSAGES } from '../../modules/user';
import { transitionTo } from 'browserHistory';

import styles from './usermenu.css';

import type { TUser } from 'modules/user';

type Props = {
  toggleUserMenu: Function,
  logout: Function,
  authMessage: Function,
  user?: TUser,
};

export class UserMenu extends Component {
  props: Props;

  componentDidMount() {
    window.addEventListener('click', this.props.toggleUserMenu, false);
  }

  componentWillUnmount() {
    window.removeEventListener('click', this.props.toggleUserMenu, false);
  }

  @autobind
  handleLogout(e: SyntheticDragEvent) {
    e.stopPropagation();
    e.preventDefault();
    this.props.toggleUserMenu();
    this.props.logout().then(() => {
      transitionTo('login');
      this.props.authMessage(INFO_MESSAGES.LOGGED_OUT);
    });
  }

  @autobind
  goToSettings() {
    const { user } = this.props;
    if (user) {
      transitionTo('user', {userId: user.id});
    }
  }

  get settingsLink(): ?Element {
    const { user } = this.props;
    if (user && user.id != null) {
      return <li><a onClick={this.goToSettings}>Settings</a></li>;
    }
  }

  render() {
    const rev = process.env.GIT_REVISION;

    return (
      <ul styleName="usermenu">
        {this.settingsLink}
        <li><a id="log-out-btn" onClick={this.handleLogout}>Log out</a></li>
        <li styleName="copyright">
          &copy; FoxCommerce. All rights reserved.{' '}
          <span>Version {rev}</span>
        </li>
      </ul>
    );
  }
}

export default connect(null, { logout, authMessage, toggleUserMenu })(UserMenu);
