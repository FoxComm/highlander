
import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { toggleUserMenu } from '../../modules/usermenu';
import { logout, authMessage, INFO_MESSAGES } from '../../modules/user';
import { transitionTo } from 'browserHistory';

import styles from './usermenu.css';

@connect(null, { logout, authMessage, toggleUserMenu })
export default class UserMenu extends Component {

  static propTypes = {
    toggleUserMenu: PropTypes.func.isRequired,
    logout: PropTypes.func.isRequired,
    authMessage: PropTypes.func.isRequired,
  };

  componentDidMount() {
    window.addEventListener('click', this.props.toggleUserMenu, false);
  }

  componentWillUnmount() {
    window.removeEventListener('click', this.props.toggleUserMenu, false);
  }

  @autobind
  handleLogout(e) {
    e.stopPropagation();
    e.preventDefault();
    this.props.toggleUserMenu();
    this.props.logout().then(() => {
      transitionTo('login');
      this.props.authMessage(INFO_MESSAGES.LOGGED_OUT);
    });
  }

  render() {
    return (
      <ul styleName="usermenu">
        <li><a onClick={this.handleLogout}>Log out</a></li>
      </ul>
    );
  }
}
