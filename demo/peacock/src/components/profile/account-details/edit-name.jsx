/* @flow */

import React, { Component } from 'react';

// libs
import _ from 'lodash';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// components
import { TextInput } from 'ui/text-input';
import { FormField } from 'ui/forms';
import CheckoutForm from 'pages/checkout/checkout-form';

// actions
import * as actions from 'modules/profile';

// types
import type { AsyncStatus } from 'types/async-actions';
import type { AccountDetailsProps } from 'types/profile';

import styles from '../profile.css';

type Props = AccountDetailsProps & {
  fetchAccount: () => Promise<*>,
  updateAccount: (payload: Object) => Promise<*>,
  updateState: AsyncStatus,
  clearErrorsFor: (...args: Array<string>) => void,
}

type State = {
  name: string,
}

class EditName extends Component {
  props: Props;

  state: State = {
    name: this.props.account.name || '',
  };

  componentWillMount() {
    this.props.fetchAccount();
  }

  componentWillUnmount() {
    this.props.clearAccountErrors();
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
      this.props.clearAccountErrors();
    });
  }

  @autobind
  handleCancel() {
    const name = this.props.account.name;
    this.setState({ name });
    this.props.toggleNameModal();
    this.props.clearAccountErrors();
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
};

export default connect(mapStateToProps, {
  ...actions,
})(EditName);
