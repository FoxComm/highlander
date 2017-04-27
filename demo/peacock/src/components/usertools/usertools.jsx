/* @flow */

import React, { Component, Element } from 'react';

// libs
import _ from 'lodash';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { authBlockTypes } from 'paragons/auth';
import { merge } from 'sprout-data';
import { isAuthorizedUser } from 'paragons/auth';
import localized from 'lib/i18n';
import { Link } from 'react-router';

// actions
import { toggleCart } from 'modules/cart';
import { toggleUserMenu } from 'modules/usermenu';

// components
import UserMenu from './usermenu';
import ActionLink from 'ui/action-link/action-link';

import styles from './usertools.css';

type Props = {
  toggleCart: Function,
  toggleUserMenu: Function,
  path: string,
  t: any,
  query: string,
  quantity: number,
};

class UserTools extends Component {
  props: Props;

  @autobind
  handleUserClick(e) {
    e.stopPropagation();
    this.props.toggleUserMenu();
  }

  renderUserInfo() {
    const { t } = this.props;
    const user = _.get(this.props, ['auth', 'user'], null);
    const query = merge(this.props.query, {auth: authBlockTypes.LOGIN});
    return !isAuthorizedUser(user) ? (
      <Link styleName="login-link" to={{pathname: this.props.path, query}}>
        {t('Log in')}
      </Link>
    ) : (
      <div styleName="user-info">
        <span styleName="username" onClick={this.handleUserClick}>{t('Hi')}, {user.name}</span>
        {this.props.isMenuVisible && <UserMenu />}
      </div>
    );
  }

  get cart(): ?Element<*> {
    if (this.props.quantity === 0) return null;

    return (
      <div styleName="cart-quantity-wrapper">
        <sup styleName="cart-quantity">{this.props.quantity}</sup>
      </div>
    );
  }

  render() {
    return (
      <div styleName="tools">
        <div styleName="login">
          {this.renderUserInfo()}
        </div>
        <ActionLink
          action={this.props.toggleCart}
          title="My Cart"
          styleName="action-link-cart"
        >
          {this.cart}
        </ActionLink>
      </div>
    );
  }
}

const mapState = state => ({
  auth: state.auth,
  isMenuVisible: state.usermenu.isVisible,
  quantity: state.cart.quantity,
});

export default connect(mapState, {
  toggleCart,
  toggleUserMenu,
})(localized(UserTools));
