/* @flow */

import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import type { HTMLElement } from 'types';
import { toggleUserMenu } from 'modules/usermenu';
import { logout } from 'modules/auth';
import localized from 'lib/i18n';

import styles from './usertools.css';

import { fetch as fetchCart } from 'modules/cart';

type Props = {
  toggleUserMenu: Function,
  logout: Function,
  fetchCart: Function,
  t: Function,
};

class UserMenu extends Component {
  props: Props;

  componentDidMount() {
    window.addEventListener('click', this.props.toggleUserMenu, false);
  }

  componentWillUnmount() {
    window.removeEventListener('click', this.props.toggleUserMenu, false);
  }

  @autobind
  handleLogout(e: Object) {
    e.stopPropagation();
    e.preventDefault();
    this.props.toggleUserMenu();
    this.props.logout().then(() => {
      this.props.fetchCart();
    });
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
  fetchCart,
})(localized(UserMenu));
