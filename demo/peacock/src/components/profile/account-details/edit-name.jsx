// @flow
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { browserHistory } from 'lib/history';
import { clearErrorsFor } from '@foxcomm/wings';

import { Link } from 'react-router';
import Button from 'ui/buttons';
import { TextInput } from 'ui/text-input';
import { FormField, Form } from 'ui/forms';
import ErrorAlerts from '@foxcomm/wings/lib/ui/alerts/error-alerts';
import CheckoutForm from 'pages/checkout/checkout-form';

import * as actions from 'modules/profile';

import type { AsyncStatus } from 'types/async-actions';

import styles from './account-details.css';

type Account = {
  name: string,
  email: string,
  isGuest: boolean,
  id: number,
}

type EmptyAccount = {
  email: void,
  name: void,
}

type EditNameProps = {
  account: Account|EmptyAccount,
  fetchAccount: () => Promise<*>,
  updateAccount: (payload: Object) => Promise<*>,
  updateState: AsyncStatus,
  clearErrorsFor: (...args: Array<string>) => void,
}

type State = {
  name: string,
}

class EditName extends Component {
  static title = 'Edit first & last name';

  props: EditNameProps;
  state: State = {
    name: this.props.account.name || '',
  };

  componentWillMount() {
    this.props.fetchAccount();
  }

  componentWillUnmount() {
    this.props.clearErrorsFor('updateAccount');
  }

  @autobind
  handleNameChange(event) {
    this.setState({
      name: event.target.value,
    });
  }

  @autobind
  handleSave() {
    this.props.updateAccount({
      name: this.state.name,
    }).then(() => {
      this.props.toggleNameModal();
    });
  }

  @autobind
  handleCancel() {
    const name = this.props.account.name;
    this.setState({ name });
    this.props.toggleNameModal();
  }

  render() {
    const action = {
      title: 'Cancel',
      handler: this.handleCancel,
    };
    return (
      <CheckoutForm
        submit={this.handleSave}
        buttonLabel="Apply"
        title="Edit first and last name"
        action={action}
        error={this.props.updateState.err}
        inProgress={this.props.updateState.inProgress}
      >
        <FormField
          error={!!this.props.updateState.err}
          styleName="name-field"
        >
          <TextInput
            required
            value={this.state.name}
            onChange={this.handleNameChange}
            placeholder="First and last name"
            name="firstLastName"
          />
        </FormField>
      </CheckoutForm>
    );
  }
}


const mapStateToProps = (state) => {
  return {
    account: state.profile.account,
    updateState: _.get(state.asyncActions, 'updateAccount', {}),
  };
}

export default connect(mapStateToProps, {...actions, clearErrorsFor})(EditName);
