// libs
import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

// redux
import * as CustomerContactActions from '../../modules/customers/contacts';

// components
import ContentBox from '../content-box/content-box';
import FormField from '../forms/formfield';
import Form from '../forms/form';
import { ApiErrors } from 'components/utils/errors';
import { EditButton } from 'components/core/button';
import SaveCancel from 'components/core/save-cancel';
import TextInput from 'components/core/text-input';

function mapDispatchToProps(dispatch, props) {
  return _.transform(CustomerContactActions, (result, action, key) => {
    result[key] = (...args) => {
      return dispatch(action(props.customerId, ...args));
    };
  });
}

@connect(
  (state, props) => ({
    ...state.customers.details[props.customerId],
  }),
  mapDispatchToProps
)
export default class CustomerContacts extends React.Component {
  static propTypes = {
    customerId: PropTypes.number.isRequired,
    toggleEditCustomer: PropTypes.func.isRequired,
    cleanErrors: PropTypes.func.isRequired,
    updateCustomerContacts: PropTypes.func.isRequired,
    details: PropTypes.object,
    isContactsEditing: PropTypes.bool,
    err: PropTypes.any,
  };

  constructor(props, ...args) {
    super(props, ...args);
    this.state = {
      name: props.details.name,
      email: props.details.email,
      phoneNumber: props.details.phoneNumber,
    };
  }

  static validateName(newName) {
    if (_.includes(newName, '@')) {
      return '@ symbol disallowed';
    }
  }

  static validateEmail(newEmail) {
    if (!_.includes(newEmail, '@')) {
      return '@ symbol is omitted';
    }
  }

  @autobind
  onSubmit() {
    this.props.toggleEditCustomer();
    this.props.updateCustomerContacts(this.state);
  }

  @autobind
  onEditClick(event) {
    this.props.toggleEditCustomer();
    event.preventDefault();
  }

  get nameField() {
    if (!this.props.isContactsEditing) {
      return <dd id="customer-contacts-name">{this.props.details.name}</dd>;
    }

    return (
      <FormField validator={CustomerContacts.validateName}>
        <TextInput
          id="nameField"
          className="fc-customer-form-input"
          name="Name"
          maxLength="255"
          required
          onChange={value => this.setState({ name: value })}
          value={this.state.name}
        />
      </FormField>
    );
  }

  get emailField() {
    if (!this.props.isContactsEditing) {
      return <dd id="customer-contacts-email">{this.props.details.email}</dd>;
    }

    return (
      <FormField validator={CustomerContacts.validateEmail}>
        <TextInput
          id="emailField"
          className="fc-customer-form-input"
          name="Email"
          maxLength="255"
          required
          onChange={value => this.setState({ email: value })}
          value={this.state.email}
        />
      </FormField>
    );
  }

  get phoneField() {
    if (!this.props.isContactsEditing) {
      return <dd id="customer-contacts-phone">{this.props.details.phoneNumber}</dd>;
    }

    return (
      <FormField validator="ascii">
        <TextInput
          id="phoneField"
          className="fc-customer-form-input"
          name="Phone"
          maxLength="255"
          required
          onChange={value => this.setState({ phoneNumber: value })}
          value={this.state.phoneNumber}
        />
      </FormField>
    );
  }

  get formActions() {
    if (this.props.isContactsEditing) {
      return <SaveCancel onCancel={this.props.toggleEditCustomer} />;
    }
  }

  get actionBlock() {
    if (!this.props.isContactsEditing) {
      return <EditButton id="fct-edit-btn__customer-contacts" onClick={this.onEditClick} />;
    }
  }

  render() {
    return (
      <ContentBox title="Contact Information" className="fc-customer-contacts" actionBlock={this.actionBlock}>
        <ApiErrors response={this.props.err} closeAction={this.props.cleanErrors} />
        <Form className="fc-customer-contacts-form fc-form-vertical" onChange={this.onChange} onSubmit={this.onSubmit}>
          <dl>
            <dt>First & Last Name</dt>
            {this.nameField}
          </dl>
          <dl>
            <dt>Email Address</dt>
            {this.emailField}
          </dl>
          <dl>
            <dt>Phone Number</dt>
            {this.phoneField}
          </dl>
          {this.formActions}
        </Form>
      </ContentBox>
    );
  }
}
