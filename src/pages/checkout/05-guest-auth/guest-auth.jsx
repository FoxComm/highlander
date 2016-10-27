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
        Guest Auth
      </article>
    );
  }
}

export default localized(GuestAuth);
