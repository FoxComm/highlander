
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { PrimaryButton } from '../common/buttons';
import { Checkbox } from '../checkbox/checkbox';
import FormField from '../forms/formfield.jsx';
import Form from '../forms/form.jsx';
import Dropdown from '../dropdown/dropdown';
import DropdownItem from '../dropdown/dropdownItem';
import * as CountriesActions from '../../modules/countries';
import { connect } from 'react-redux';


@connect((state, props) => ({
  countries: state.countries
}), CountriesActions)
export default class EditAddressBox extends React.Component {

  componentDidMount() {
    this.props.fetchCountries();
  }

  render() {
    const form = this.props.form;
    return (
      <li className="fc-card-container fc-addresses fc-addresses-new">
        <Form className="fc-customer-address-form fc-form-vertical"
              onChange={ this.props.onChange }
              onSubmit={ this.props.onSubmit }>
          <header>
            New Address
          </header>
          <div>
            <ul className="fc-address-form-fields">
              <li className="fc-address-form-line">
                <label className="fc-address-default-checkbox">
                  <Checkbox defaultChecked={ form.isDefaultShipping } name="isDefaultShipping" />
                  <span>Default shipping address</span>
                </label>
              </li>
              <li className="fc-address-form-line">
                <FormField label="First Name">
                  <input id="firstName"
                         className="fc-customer-form-input"
                         name="firstName"
                         maxLength="255"
                         type="text"
                         required
                         value={ form.firstName } />
                </FormField>
              </li>
              <li className="fc-address-form-line">
                    <FormField label="Last Name">
                      <input id="numberCardFormField"
                             className="fc-customer-form-input"
                             name="lastName"
                             maxLength="255"
                             type="text"
                             required
                             value={ form.lastName } />
                    </FormField>
              </li>
            </ul>
          </div>
          <div className="fc-address-form-controls">
            <a className="fc-btn-link" onClick={ this.props.onCancel }>Cancel</a>
            <PrimaryButton type="submit">Save</PrimaryButton>
          </div>
        </Form>
      </li>
    );
  }
}
