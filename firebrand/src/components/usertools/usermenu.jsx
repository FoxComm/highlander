/* @flow */

import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import type { HTMLElement } from 'types';
import { toggleUserMenu } from 'modules/usermenu';
import { logout } from 'modules/auth';
import localized from 'lib/i18n';

import styles from './usertools.css';

class UserMenu extends Component {
  static PropTypes = {
    toggleUserMenu: PropTypes.func,
    logout: PropTypes.func,
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
    this.props.logout();
  }

  render(): HTMLElement {
    const { t } = this.props;
    return (
      <ul styleName="menu">
        <li>
          <a
            styleName="menu-link"
            href="/logout"
            onClick={this.handleLogout}
          >{t('LOG OUT')}</a>
        </li>
      </ul>
    );
  }
}

export default connect(null, {
  logout,
  toggleUserMenu,
})(localized(UserMenu));
