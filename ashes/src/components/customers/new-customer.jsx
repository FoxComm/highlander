// @flow weak
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { transitionTo, transitionToLazy } from 'browserHistory';

// redux
import * as CustomersActions from 'modules/customers/new';

// components
import FormField from '../forms/formfield';
import Form from '../forms/form';
import SaveCancel from 'components/core/save-cancel';
import { ApiErrors } from 'components/utils/errors';
import TextInput from 'components/core/text-input';

import type { NewCustomerPayload } from 'modules/customers/new';

type Props = {
  submitStatus: AsyncState,
  createCustomer: (payload: NewCustomerPayload) => Promise<*>,
  clearErrors: () => void,
}

function mapStateToProps(state) {
  return {
    submitStatus: _.get(state, 'asyncActions.createCustomer', {}),
  };
}

type State = {
  name: string,
  email: string,
}


class NewCustomer extends Component {
  props: Props;
  state: State = {
    name: '',
    email: '',
  };

  componentWillMount() {
    this.props.clearErrors();
  }

  @autobind
  submitForm() {
    const payload = {
      name: this.state.name,
      email: this.state.email,
    };
    this.props.createCustomer(payload).then(({ id }) => {
      transitionTo('customer', { customerId: id });
    });
  }

  @autobind
  onChangeValue({ target }) {
    this.setState({
      [target.name]: target.value,
    });
  }

  get errors() {
    const { submitStatus } = this.props;
    if (submitStatus.err) {
      return (
        <li>
          <ApiErrors response={submitStatus.err} />
        </li>
      );
    }
  }

  render() {
    const { name, email } = this.state;
    const { submitStatus } = this.props;

    return (
      <div className="fc-customer-create">
        <div className="fc-grid">
          <header className="fc-customer-form-header fc-col-md-1-1">
            <h1 className="fc-title">
              New Customer
            </h1>
          </header>
          <article className="fc-col-md-1-1">
            <div className="fc-grid fc-grid-no-gutter">
              <Form className="fc-customer-form fc-form-vertical fc-col-md-2-5"
                    onSubmit={this.submitForm}
                    onChange={this.onChangeValue}>
                <ul className="fc-customer-form-fields">
                  <li>
                    <FormField label="Name" validator="ascii">
                      <TextInput id="nameCustomerFormField"
                             className="fc-customer-form-input"
                             name="name"
                             maxLength="255"
                             value={name}
                             required />
                    </FormField>
                  </li>
                  <li>
                    <FormField label="Email Address" validator="ascii">
                      <TextInput id="emailCustomerFormField"
                             className="fc-customer-form-input"
                             name="email"
                             maxLength="255"
                             value={email}
                             required />
                    </FormField>
                  </li>
                  {this.errors}
                  <li className="fc-customer-form-controls">
                    <SaveCancel
                      onCancel={transitionToLazy('customers')}
                      saveLabel="Save Customer"
                      isLoading={submitStatus.inProgress}
                    />
                  </li>
                </ul>
              </Form>
            </div>
          </article>
        </div>
      </div>
    );
  }
}

export default connect(mapStateToProps, CustomersActions)(NewCustomer);
