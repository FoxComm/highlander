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
import { browserHistory } from 'lib/history';

// actions
import { toggleCart, fetch as fetchCart } from 'modules/cart';
import { logout } from 'modules/auth';

// components
import ActionLink from 'ui/action-link/action-link';

import styles from './usertools.css';

type Props = {
  toggleCart: () => void,
  logout: () => Promise<*>,
  fetchCart: () => void,
  path: string,
  t: any,
  query: string,
  quantity: number,
  auth: Object,
};

class UserTools extends Component {
  props: Props;

  @autobind
  handleLogout() {
    this.props.logout().then(() => {
      this.props.fetchCart();
      browserHistory.push('/');
    });
  }

  get userInfo() {
    const { t } = this.props;
    const user = _.get(this.props, ['auth', 'user'], null);
    const query = merge(this.props.query, {auth: authBlockTypes.LOGIN});

    if (!isAuthorizedUser(user)) {
      return (
        <Link styleName="login" to={{pathname: this.props.path, query}}>
          {t('Log in')}
        </Link>
      );
    }

    return (
      <div>
        <span styleName="name">{t('Hi')}, {user.name}</span>
        <Link to="/logout" onClick={this.handleLogout} styleName="logout">
          {t('Log out')}
        </Link>
        <Link to="/profile" styleName="profile">
          {t('Profile')}
        </Link>
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
        <div styleName="user-info">
          {this.userInfo}
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

const mapState = (state) => {
  return {
    auth: _.get(state, 'auth', {}),
    quantity: _.get(state.cart, 'quantity', 0),
  };
};

export default connect(mapState, {
  toggleCart,
  logout,
  fetchCart,
})(localized(UserTools));
