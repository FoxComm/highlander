/* @flow */

import React, { Component } from 'react';

import localized from 'lib/i18n';

import styles from './guest-auth.css';

@localized
class GuestAuth extends Component {

  render() {
    if (!this.props.isEditing) {
      return null;
    }

    return (
      <article styleName="guest-auth">
        <div styleName="auth-block">
          <header styleName="header">SIGN IN & CHECKOUT</header>
          <div styleName="form">
            <div styleName="form-content">
            </div>
          </div>
        </div>
        <div styleName="auth-block">
          <header styleName="header">CHECKOUT AS GUEST</header>
          <div styleName="form">
            <div styleName="form-content">
            </div>
          </div>
        </div>
      </article>
    );
  }
}

export default localized(GuestAuth);
