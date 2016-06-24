// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

// redux
import * as CustomerContactActions from '../../modules/customers/contacts';

// components
import ContentBox from '../content-box/content-box';
import FormField from '../forms/formfield';
import Form from '../forms/form';
import ErrorAlerts from '../alerts/error-alerts';
import { EditButton } from '../common/buttons';
import SaveCancel from '../common/save-cancel';

// function mapDispatchToProps(dispatch, props) {
//   return _.transform(CustomerContactActions, (result, action, key) => {
//     result[key] = (...args) => {
//       return dispatch(action(props.userId, ...args));
//     };
//   });
// }

@connect((state, props) => ({
  ...state.users.details[props.userId]
}), null)
export default class UserContacts extends React.Component {
  constructor(props, ...args) {
    super(props, ...args);
    this.state = {
      name: props.details.name,
      email: props.details.email,
      phoneNumber: '',
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
      return <dd>{ this.props.details.name }</dd>;
    }

    return (
      <FormField validator={ UserContacts.validateName }>
        <input id='nameField'
               className='fc-user-form-input'
               name='Name'
               maxLength='255'
               type='text'
               required
               onChange={ ({target}) => this.setState({name: target.value}) }
               value={ this.state.name } />
      </FormField>
    );
  }

  get emailField() {
    if (!this.props.isContactsEditing) {
      return <dd>{ this.props.details.email }</dd>;
    }

    return (
      <FormField validator={ UserContacts.validateEmail }>
        <input id='emailField'
               className='fc-user-form-input'
               name='Email'
               maxLength='255'
               type='text'
               required
               onChange={ ({target}) => this.setState({email: target.value}) }
               value={ this.state.email } />
      </FormField>
    );
  }

  get phoneField() {
    if (!this.props.isContactsEditing) {
      return <dd>{ this.props.details.phoneNumber }</dd>;
    }

    return (
      <FormField validator='ascii'>
        <input id='phoneField'
               className='fc-user-form-input'
               name='Phone'
               maxLength='255'
               type='text'
               required
               onChange={ ({target}) => this.setState({phoneNumber: target.value}) }
               value={ this.state.phoneNumber } />
      </FormField>
    );
  }

  get formActions() {
    if (this.props.isContactsEditing) {
      return <SaveCancel onCancel={ this.props.toggleEditUser } />;
    }
  }

  get actionBlock() {
    if (!this.props.isContactsEditing) {
      return (
        <EditButton onClick={ this.onEditClick }/>
      );
    }
  }

  render() {
    return (
      <ContentBox title='Contact Information'
                  className='fc-user-contacts'
                  actionBlock={ this.actionBlock }>
        <ErrorAlerts error={this.props.err} closeAction={this.props.cleanErrors} />
        <Form className='fc-user-contacts-form fc-form-vertical'
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
