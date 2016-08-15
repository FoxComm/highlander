/* @flow */

import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { toggleUserMenu } from '../../modules/usermenu';
import { logout, authMessage, INFO_MESSAGES } from '../../modules/user';
import { transitionTo } from 'browserHistory';

import styles from './usermenu.css';

type User = {
  id: string|number,
}

type Props = {
  toggleUserMenu: Function,
  logout: Function,
  authMessage: Function,
  user?: User,
};

/* ::`*/
@connect(null, { logout, authMessage, toggleUserMenu })
/* ::`*/
export default class UserMenu extends Component {
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
    return (
      <ul styleName="usermenu">
        {this.settingsLink}
        <li><a onClick={this.handleLogout}>Log out</a></li>
        <li styleName="copyright">&copy; FoxCommerce. All rights reserved.</li>
      </ul>
    );
  }
}
