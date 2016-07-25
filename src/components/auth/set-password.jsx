// @flow

import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import styles from './css/auth.css';
import { autobind } from 'core-decorators';

import Form from '../forms/form';
import FormField from '../forms/formfield';
import { PrimaryButton, Button } from '../common/buttons';

type State = {
  email: string,
  password: string,
}




class SetPassword extends Component {

  state: State = {
    email: '',
    password: '',
  };

  get username() {
    return this.props.location.query.username;
  }

  @autobind
  handleSubmit() {

  }

  @autobind
  handleInputChange(event) {
    const { target } = event;
    this.setState({
      [target.name]: target.value,
    });
  }

  render() {
    return (
      <div styleName="main">
        <div className="fc-auth__title">Create Account</div>
        <div styleName="message">
          Hey, {this.username}! You’ve been invited to create an account with
          FoxCommerce. All you need to do is choose your method
          to sign up.
        </div>
        <Form styleName="form" onSubmit={this.handleSubmit}>
          <FormField styleName="email" label="Email">
            <input
              name="email"
              onChange={this.handleInputChange}
              value={this.state.email}
              type="text"
              className="fc-input"
            />
          </FormField>
          <FormField styleName="password" label="Create Password">
            <input
              name="password"
              onChange={this.handleInputChange}
              value={this.state.password}
              type="password"
              className="fc-input"
            />
          </FormField>
          <FormField styleName="password" label="Confirm Password">
            <input
              name="password"
              onChange={this.handleInputChange}
              value={this.state.password}
              type="password"
              className="fc-input"
            />
          </FormField>
          <PrimaryButton
            styleName="submit-button"
            type="submit"
          >
            Sign Up
          </PrimaryButton>
        </Form>
      </div>
    )
  }
}

export default SetPassword;
