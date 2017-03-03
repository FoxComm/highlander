/* @flow */

import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

import Button from 'ui/buttons';
import { TextInput } from 'ui/inputs';
import { FormField } from 'ui/forms';

import { email as validateEmail } from 'ui/forms/validators';

import styles from './form.css';

type State = {
  email: string,
  error: ?string,
};

export default class SubscriptionForm extends Component {

  state: State = {
    email: '',
    error: null,
  };

  @autobind
  onEmailChange(value: string) {
    this.setState({email: value, error: null});
  }

  @autobind
  validateAndSubmit(e: Object) {
    const { email } = this.state;
    const validationResult = validateEmail(email);
    if (_.isString(validationResult) && !_.isEmpty(validationResult)) {
      e.preventDefault();
      e.stopPropagation();
      this.setState({ error: 'Email format is invalid.' });
    } else {
      setTimeout(() => this.setState({ email: '', error: null }), 1000);
    }
  }

  render() {
    return (
      <form
        styleName="email"
        action="//theperfectgourmet.us13.list-manage.com/subscribe/post?u=cb7c9e34080be32c3c8afbba6&id=3aff43b62c"
        method="post"
        id="mc-embedded-subscribe-form"
        name="mc-embedded-subscribe-form"
        target="_blank"
      >
        <FormField key="EMAIL" styleName="form-field" error={this.state.error}>
          <TextInput
            placeholder="Email"
            name="EMAIL"
            value={this.state.email}
            onChange={({target}) => this.onEmailChange(target.value)}
          />
        </FormField>
        <Button
          styleName="button"
          type="submit"
          onClick={this.validateAndSubmit}
        >
          Join
        </Button>
      </form>
    );
  }
}
