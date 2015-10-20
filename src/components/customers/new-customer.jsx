'use strict';

import React, { PropTypes } from 'react';
import SectionTitle from '../section-title/section-title';
import FormField from '../forms/formfield.jsx';
import Form from '../forms/form.jsx';
import { Link } from '../link';
import { transitionTo } from '../../route-helpers';
import { Map } from 'immutable';

export default class NewCustomer extends React.Component {

  static contextTypes = {
    history: PropTypes.object.isRequired
  };

  constructor(props, context) {
    super(props, context);

    this.state = {
      email: null,
      name: null,
      password: null,
      isGuest: false
    };
  }

  submitForm() {
    console.log("submit");
  }

  render () {
    let formData = this.state.formData;
    return (
      <div className="fc-customer-create">
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
                    <FormField label="Name" validator="ascii">
                      <input id="nameCustomerFormField"
                             className="fc-customer-form-input"
                             name="name"
                             maxLength="255"
                             type="text"
                             value={this.state.name}
                             required />
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
