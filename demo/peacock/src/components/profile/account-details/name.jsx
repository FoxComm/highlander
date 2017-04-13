/* @flow */

import React, { Component } from 'react';

import ActionLink from 'ui/action-link/action-link';

import styles from './account-details.css';

type Props = {
  name: string,
};

class Name extends Component {
  props: Props;

  get action() {
    return (
      <ActionLink
        action={() => null}
        title="Edit"
        styleName="action-link"
      />
    );
  }

  get content() {
    const { name } = this.props;
    return (
      <div styleName="content">
        {name}
      </div>
    );
  }

  render() {
    return (
      <div styleName="name-block">
        <div styleName="header">
          <div styleName="title">First and last name</div>
          {this.action}
        </div>
        {this.content}
      </div>
    );
  }
}

export default Name;
