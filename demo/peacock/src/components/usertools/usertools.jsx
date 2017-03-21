/* @flow */

import _ from 'lodash';
import React, { Component, PropTypes, Element } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { toggleCart } from 'modules/cart';
import { toggleUserMenu } from 'modules/usermenu';
import { authBlockTypes, isAuthorizedUser } from 'paragons/auth';
import { merge } from 'sprout-data';

import localized from 'lib/i18n';

import styles from './usertools.css';

import { Link } from 'react-router';
import UserMenu from './usermenu';

class UserTools extends Component {
  static PropTypes = {
    toggleCart: PropTypes.func,
    toggleUserMenu: PropTypes.func,
    path: PropTypes.string,
  };

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

  render(): Element<*> {
    return (
      <div styleName="tools">
        <div styleName="login">
          {this.renderUserInfo()}
        </div>
        <span styleName="cart" onClick={this.props.toggleCart}>
          My Cart
          <div styleName="cart-quantity-wrapper">
            <sup styleName="cart-quantity">{this.props.quantity}</sup>
          </div>
        </span>
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
