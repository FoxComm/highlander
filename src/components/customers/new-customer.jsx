'use strict';

import React from 'react';
import SectionTitle from '../section-title/section-title';
import FormField from '../forms/formfield.jsx';
import Form from '../forms/form.jsx';
import { Link } from '../link';

import CustomerStore from '../../stores/customers';
import CustomerActions from '../../actions/customers';

export default class NewCustomer extends React.Component {

  constructor(props, context) {
    super(props, context);

    this.state = {
      formData: {}
    };
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
            <Form className="fc-form-vertical"
                  action="/customers"
                  method="POST"
                  onSubmit={this.submitForm.bind(this)}>
              <ul className="fc-customer-form-fields">
                <li>
                  <FormField label="Email Address" validator="ascii">
                    <input name="email" maxLength="255" type="text" value={formData.email} required />
                  </FormField>
                </li>
                <li>
                  <FormField label="First Name" validator="ascii" optional>
                    <input name="first_name" maxLength="255" type="text" value={formData.firstName} />
                  </FormField>
                </li>
                <li>
                  <FormField label="Last Name" validator="ascii" optional>
                    <input name="last_name" maxLength="255" type="text" value={formData.lastName} />
                  </FormField>
                </li>
                <li>
                  <FormField label="Phone Number" validator="ascii" optional>
                    <input name="phone_number" maxLength="255" type="text" value={formData.phoneNumber} />
                  </FormField>
                </li>
                <li className="fc-customer-form-controls">
                  <Link to='customers'><i className="icon-close"></i></Link>
                  <input type="submit" value="Save Customer" className="fc-btn" />
                </li>
              </ul>
            </Form>
          </article>
        </div>
      </div>
    );
  }
}
