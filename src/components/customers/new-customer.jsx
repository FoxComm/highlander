'use strict';

import React, { PropTypes } from 'react';
import SectionTitle from '../section-title/section-title';
import FormField from '../forms/formfield.jsx';
import Form from '../forms/form.jsx';
import { Link } from '../link';

import { transitionTo } from '../../route-helpers';

import { Map } from 'immutable';

import CustomerStore from '../../stores/customers';
import CustomerActions from '../../actions/customers';

export default class NewCustomer extends React.Component {

  static contextTypes = {
    history: PropTypes.object.isRequired
  };

  constructor(props, context) {
    super(props, context);

    this.state = Map({
      email: null,
      name: null,
      password: null,
      isGuest: false
    });
    this.onChange = this.onChange.bind(this);
  }

  componentDidMount() {
    CustomerStore.listen(this.onChange);
  }

  componentWillUnmount() {
    CustomerStore.unlisten(this.onChange);
  }

  onChange() {
    let state = CustomerStore.getState();
    let customer = state.first();
    transitionTo(this.context.history, 'customer', {customer: customer.id});
  }

  submitForm(event) {
    event.preventDefault();

    CustomerActions.createCustomer(event.target);
  }

  render () {
    let formData = this.state.formData;
    return (
      <div className="customer-create">
        <div className="gutter">
          <header className="fc-customer-form-header">
            <h1 className="fc-title">
              New Customer
            </h1>
          </header>
          <article>
            <div className="fc-grid">
              <Form className="fc-customer-form fc-form-vertical fc-col-md-2-5"
                    action="/customers"
                    method="POST"
                    onSubmit={this.submitForm.bind(this)}>
                <ul className="fc-customer-form-fields">
                  <li>
                    <FormField label="Name" validator="ascii" optional>
                      <input id="nameCustomerFormField"
                             className="fc-customer-form-input"
                             name="name"
                             maxLength="255"
                             type="text"
                             value={this.state.name} />
                    </FormField>
                  </li>
                  <li>
                    <FormField label="Email Address" validator="ascii">
                      <input id="emailCustomerFormField"
                             className="fc-customer-form-input"
                             name="email"
                             maxLength="255"
                             type="text"
                             value={this.state.email}
                             required />
                    </FormField>
                  </li>
                  <li>
                    <FormField label="Create Password" validator="ascii" optional>
                      <input id="passwordCustomerFormField"
                             className="fc-customer-form-input"
                             name="password"
                             maxLength="255"
                             type="password"
                             value={this.state.password} />
                    </FormField>
                  </li>
                  <li>
                    <FormField label="Guest Account">
                      <input id="isGuestCustomerFormField"
                             className="fc-customer-form-checkbox"
                             name="isGuest"
                             type="checkbox"
                             value={this.state.isGuest} />
                    </FormField>
                  </li>
                  <li className="fc-customer-form-controls">
                    <Link to='customers' className="fc-btn-link">Cancel</Link>
                    <input type="submit" value="Save Customer" className="fc-btn fc-btn-primary" />
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
