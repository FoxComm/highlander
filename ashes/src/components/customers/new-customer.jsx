// libs
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// redux
import * as CustomersActions from '../../modules/customers/new';

// components
import FormField from '../forms/formfield.jsx';
import Form from '../forms/form.jsx';
import { Link } from '../link';
import SaveCancel from '../common/save-cancel';

@connect((state, ownProps) => ({
  ...state.customers.adding,
  ...ownProps.location.query,
}), CustomersActions)
export default class NewCustomer extends React.Component {

  static propTypes = {
    createCustomer: PropTypes.func.isRequired,
    changeFormData: PropTypes.func.isRequired,
    resetForm: PropTypes.func.isRequired,
    name: PropTypes.string,
    email: PropTypes.string,
    id: PropTypes.number
  };

  @autobind
  submitForm(event) {
    event.preventDefault();
    this.props.createCustomer();
  }

  @autobind
  onChangeValue({ target }) {
    this.props.changeFormData(target.name, target.value);
  }

  componentDidMount() {
    this.props.resetForm();
  }

  render() {
    const { name, email } = this.props;

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
                      <input id="nameCustomerFormField"
                             className="fc-customer-form-input"
                             name="name"
                             maxLength="255"
                             type="text"
                             value={name}
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
                             value={email}
                             required />
                    </FormField>
                  </li>
                  <li className="fc-customer-form-controls">
                    <SaveCancel cancelTo="customers"
                                saveText="Save Customer" />
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
