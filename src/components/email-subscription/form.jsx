/* @flow */

import React, { Component } from 'react';

import Button from 'ui/buttons';
import { TextInput } from 'ui/inputs';

import styles from './form.css';

export default class SubscriptionForm extends Component {

  render() {
    return (
      <form styleName="email">
        <TextInput placeholder="Email" />
        <Button styleName="button" type="button">Join</Button>
      </form>
    );
  }
}
