import React from 'react';
import _ from 'lodash';

import { Checkbox } from '../checkbox/checkbox';
import Form from '../forms/form';
import FormField from '../forms/formfield';
import Typeahead from '../typeahead/typeahead';

const NewOrder = props => {
  return (
    <div className="fc-order-create">
      <div className="fc-grid">
        <header className="fc-order-create__header fc-col-md-1-1">
          <h1 className="fc-title">
            New Order
          </h1>
        </header>
        <article className="fc-col-md-1-1">
          <div className="fc-grid fc-order-create__customer-form-panel">
            <div className="fc-order-create__customer-form-subtitle fc-col-md-1-1">
              <h2>Customer</h2>
            </div>
            <div className="fc-order-create__customer-form fc-col-md-1-1">
              <Form className="fc-grid fc-grid-no-gutter" onSubmit={_.noop}>
                <FormField 
                  className="fc-order-create__customer-search fc-col-md-5-8"
                  label="Search All Customers">
                  <Typeahead
                    placeholder="Customer name or email..."
                    items={[]} />
                </FormField>
                <FormField className="fc-col-md-2-8" label="Checkout as Guest">
                  <Checkbox name="guestCheckout" inline={true} />
                </FormField>
                <FormField className="fc-col-md-1-8">
                  <input className="fc-btn fc-btn-primary" type="submit" value="Next" />
                </FormField>
              </Form>
            </div>
          </div>
        </article>
      </div>
    </div>
  );
};

export default NewOrder;
