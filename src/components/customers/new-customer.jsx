'use strict';

import React from 'react';
import SectionTitle from '../section-title/section-title';
import FormField from '../forms/formfield.jsx';
import Form from '../forms/form.jsx';

import CustomerStore from '../../stores/customers';
import CustomerActions from '../../actions/customers';

export default class NewCustomer extends React.Component {

  constructor(props, context) {
    super(props, context);

    this.state = {
      formData: {}
    };
  }

  render () {
    let formData = this.state.formData;
    return (
      <div className="customer-create">
        <SectionTitle title="New Customer" />
        <article>
          <Form>
            <ul className="fc-customer-form-fields">
              <li>
                <FormField label="Email Address" validator="ascii">
                  <input name="name" maxLength="255" type="text" value={formData.email} required />
                </FormField>
              </li>
              <li>
                <FormField label="First Name" validator="ascii" optional>
                  <input name="name" maxLength="255" type="text" value={formData.firstName} />
                </FormField>
              </li>
              <li>
                <FormField label="Last Name" validator="ascii" optional>
                  <input name="name" maxLength="255" type="text" value={formData.lastName} />
                </FormField>
              </li>
              <li>
                <FormField label="Phone Number" validator="ascii" optional>
                  <input name="name" maxLength="255" type="text" value={formData.phoneNumber} />
                </FormField>
              </li>
            </ul>
          </Form>
        </article>
      </div>
    );
  }
}
