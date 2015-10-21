'use strict';

import React, { PropTypes } from 'react';
import SectionTitle from '../section-title/section-title';
import FormField from '../forms/formfield.jsx';
import Form from '../forms/form.jsx';
import { Link } from '../link';
import { transitionTo } from '../../route-helpers';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import * as CustomersActions from '../../modules/customers/new';

@connect(state => ({
  ...state.customers.adding
}), {
  ...CustomersActions
})
export default class NewCustomer extends React.Component {

  static contextTypes = {
    history: PropTypes.object.isRequired
  };

  @autobind
  submitForm(event) {
    event.preventDefault();
    console.log("submit");
  }

  render () {
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
                             value={this.props.name}
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
                             value={this.props.email}
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
