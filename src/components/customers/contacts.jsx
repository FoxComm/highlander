import React, { PropTypes } from 'react';
import _ from 'lodash';
import ContentBox from '../content-box/content-box';
import FormField from '../forms/formfield';
import Form from '../forms/form';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import * as CustomerContactActions from '../../modules/customers/contacts';
import { PrimaryButton } from '../common/buttons';

function mapDispatchToProps(dispatch, props) {
  return _.transform(CustomerContactActions, (result, action, key) => {
    result[key] = (...args) => {
      return dispatch(action(props.customerId, ...args));
    };
  });
}

@connect((state, props) => ({
  ...state.customers.details[props.customerId]
}), mapDispatchToProps)

export default class CustomerContacts extends React.Component {

  static propTypes = {
    customerId: PropTypes.number.isRequired,
    toggleEditCustomer: PropTypes.func.isRequired,
    updateCustomerContacts: PropTypes.func,
    details: PropTypes.object,
    isContactsEditing: PropTypes.bool
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      name: props.details.name,
      email: props.details.email,
      phoneNumber: props.details.phoneNumber
    };
  }

  static validateName(newName) {
    if (_.includes(newName, '@')) {
      return '@ symbol disallowed';
    }
  }

  static validateEmail(newEmail) {
    if (!_.includes(newEmail, '@')) {
      return '@ symbol is ommited';
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
    if (this.props.isContactsEditing) {
      return (
        <FormField validator={ CustomerContacts.validateName }>
          <input id='nameField'
                 className='fc-customer-form-input'
                 name='Name'
                 maxLength='255'
                 type='text'
                 required
                 onChange={ ({target}) => this.setState({name: target.value}) }
                 value={ this.state.name } />
        </FormField>
      );
    } else {
      return <dd>{ this.props.details.name }</dd>;
    }
  }

  get emailField() {
    if (this.props.isContactsEditing) {
      return (
        <FormField validator={ CustomerContacts.validateEmail }>
          <input id='emailField'
                 className='fc-customer-form-input'
                 name='Email'
                 maxLength='255'
                 type='text'
                 required
                 onChange={ ({target}) => this.setState({email: target.value}) }
                 value={ this.state.email } />
        </FormField>
      );
    } else {
      return <dd>{ this.props.details.email }</dd>;
    }
  }

  get phoneField() {
    if (this.props.isContactsEditing) {
      return (
        <FormField validator='ascii'>
          <input id='phoneField'
                 className='fc-customer-form-input'
                 name='Phone'
                 maxLength='255'
                 type='text'
                 required
                 onChange={ ({target}) => this.setState({phoneNumber: target.value}) }
                 value={ this.state.phoneNumber } />
        </FormField>
      );
    } else {
      return <dd>{ this.props.details.phoneNumber }</dd>;
    }
  }

  get formActions() {
    if (this.props.isContactsEditing) {
      return (
        <div>
          <a className='fc-customer-cancel-edit' onClick={ this.props.toggleEditCustomer }>
            Cancel
          </a>
          <PrimaryButton>
            Save
          </PrimaryButton>
        </div>
      );
    }
  }

  get actionBlock() {
    if (!this.props.isContactsEditing) {
      return (
        <button onClick={ this.onEditClick } className='fc-btn'>
          <i className='icon-edit'></i>
        </button>
      );
    }
  }

  render() {
    return (
        <ContentBox title='Contact Information'
                    className='fc-customer-contacts'
                    actionBlock={ this.actionBlock }>
          <Form className='fc-customer-contacts-form fc-form-vertical'
                onChange={ this.onChange }
                onSubmit={ this.onSubmit }>
          <dl>
            <dt>Name</dt>
            { this.nameField }
          </dl>
          <dl>
            <dt>Email Address</dt>
            { this.emailField }
          </dl>
          <dl>
            <dt>Phone Number</dt>
            { this.phoneField }
          </dl>
            { this.formActions }
          </Form>
        </ContentBox>
    );
  }
}
