/* @flow */

import React, { Component, Element } from 'react';

// libs
import _ from 'lodash';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { isAuthorizedUser } from 'paragons/auth';
import localized from 'lib/i18n';

// actions
import { showCart, hideCart } from 'modules/cart';
import { toggleUserMenu } from 'modules/ui';
import { toggleAuthMenu } from 'modules/auth';

// components
import ActionLink from 'ui/action-link/action-link';
import UserMenu from './usermenu';
import Cart from 'components/cart/cart';
import Icon from 'ui/icon';

import styles from './usertools.css';

type Props = {
  auth: {
    isVisible: boolean,
    user: {
      name: string,
      email: string,
    },
  },
  showCart: Function,
  hideCart: Function,
  toggleUserMenu: Function,
  toggleAuthMenu: Function,
  path: string,
  t: any,
  quantity: number,
  auth: Object,
};

class UserTools extends Component {
  props: Props;

  @autobind
  handleUserClick(e) {
    e.stopPropagation();
    this.props.toggleUserMenu();
  }

  renderUserInfo() {
    const { auth, t } = this.props;
    const user = _.get(auth, 'user', null);

    return !isAuthorizedUser(user) ? (
      <ActionLink
        action={this.props.toggleAuthMenu}
        title={t('Sign in')}
        styleName="login-link"
      />
    ) : (
      <div styleName="user-info">
        <span styleName="username" onClick={this.handleUserClick}>{t('Hi')}, {user.name}</span>
        {this.props.isMenuVisible && <UserMenu />}
      </div>
    );
  }

  get cart(): Array<Element<any>> {
    return [
      <span styleName="cart-quantity" key="cart-quantity">{this.props.quantity}</span>,
      <Cart key="cart" />,
    ];
  }

  render() {
    return (
      <div styleName="tools">
        <div styleName="login" className={{ [styles.active]: this.props.auth.isVisible }}>
          {this.renderUserInfo()}
        </div>
        <div
          styleName="action-link-cart"
          onMouseEnter={this.props.showCart}
          onMouseLeave={this.props.hideCart}
        >
          <div>
            <div styleName="cart-icon" />
            {this.cart}
          </div>
        </div>
      </div>
    );
  }
}

const mapState = state => ({
  auth: state.auth,
  isMenuVisible: state.ui.usermenuVisible,
  quantity: state.cart.quantity,
});

export default connect(mapState, {
  showCart,
  hideCart,
  toggleUserMenu,
  toggleAuthMenu,
})(localized(UserTools));
