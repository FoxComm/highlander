/* @flow */

import _ from 'lodash';
import React, { Component, PropTypes } from 'react';
import type { HTMLElement } from 'types';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { toggleCart } from 'modules/cart';
import { toggleUserMenu } from 'modules/usermenu';
import { authBlockTypes } from 'paragons/auth';
import { merge } from 'sprout-data';

import localized from 'lib/i18n';

import styles from './usertools.css';

import Icon from 'ui/icon';
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
    return _.isEmpty(user) ? (
      <Link styleName="login-link" to={{pathname: this.props.path, query}}>
        {t('LOG IN')}
      </Link>
    ) : (
      <div styleName="user-info">
        <span styleName="username" onClick={this.handleUserClick}>{t('HI')}, {user.name.toUpperCase()}</span>
        {this.props.isMenuVisible && <UserMenu />}
      </div>
    );
  }

  render(): HTMLElement {
    return (
      <div styleName="tools">
        <div styleName="login">
          {this.renderUserInfo()}
        </div>
        <button styleName="cart" onClick={this.props.toggleCart}>
          <Icon name="fc-cart" styleName="head-icon"/>
          <sup styleName="cart-quantity">{this.props.quantity}</sup>
        </button>
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
