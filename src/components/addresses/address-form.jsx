'use strict';

import _ from 'lodash';
import React from 'react';
import FormField from '../forms/formfield.jsx';
import InputMask from 'react-input-mask';
import Form from '../forms/form.jsx';
import { listenTo, stopListeningTo, dispatch } from '../../lib/dispatcher';
import CountryStore from '../../stores/countries';
import AddressStore from '../../stores/addresses';
import OrderStore from '../../stores/orders';
import * as validators from '../../lib/validators';

const DEFAULT_COUNTRY = 'US';

export default class AddressForm extends React.Component {

  constructor(props, context) {
    super(props, context);

    let address = props.address;
    let formData = address ? _.omit(address, 'region') : {};
    formData.regionId = address ? address.region.id : null;

    this.state = {
      formData,
      countryId: address ? address.region.countryId : null
    };
  }

  get isAddingForm() {
    return !this.props.address;
  }

  componentDidMount() {
    CountryStore.listenToEvent('change', this);
    CountryStore.fetch();
  }

  componentWillUnmount() {
    CountryStore.stopListeningToEvent('change', this);
  }

  onChangeCountryStore(countries) {
    this.setState({countries});

    if (!this.state.country && !this.countryWillFetch) {
      this.updateRegions();
    }
  }

  updateRegions(countryId=this.state.countryId) {
    if (countryId === null) {
      let country = CountryStore.findWhere({alpha2: DEFAULT_COUNTRY});
      countryId = country.id;
    }

    this.countryWillFetch = true;
    CountryStore.fetch(countryId)
      .then(country => {
        this.setState({
          country,
          countryId,
          formData: _.extend(this.state.formData, {regionId: country.regions[0].id})
        });
      })
      .catch(({errors}) => {
        this.setState({errors});
      });
  }

  close() {
    dispatch('toggleModal', null);
  }

  componentWillUpdate(nextProps, nextState) {
    if (!this.state.errors && nextState.errors) {
      this.shouldScrollToErrors = true;
    }
  }

  componentDidUpdate() {
    if (this.shouldScrollToErrors && this.state.errors) {
      this.refs.errorMessages.scrollIntoView();
      this.shouldScrollToErrors = false;
    }
  }

  /**
   * Prepare value before submitting to server
   * @param name
   * @param value
   */
  prepareValue(name, value) {
    switch (name) {
      case 'phoneNumber':
        return value.replace(/[^\d]/g, '');
        break;
      default:
        return value;
    }
  }

  onSubmitForm(event) {
    event.preventDefault();

    const customerId = this.props.customerId;
    const formData = _.transform(this.state.formData, (result, value, name) => {
      result[name] = this.prepareValue(name, value);
    });

    let willSaved;

    if (this.isAddingForm) {
      willSaved = AddressStore.create(customerId, formData);
    } else {
      willSaved = AddressStore.patch(customerId, this.props.address.id, formData);
    }

    willSaved
      .then((address) => {
        this.setState({errors: null});

        if (this.props.onSaved) {
          this.props.onSaved(address.id);
        }
      })
      .then(() => {
        this.close();
      })
      .catch(({errors}) => {
        this.setState({errors});
      });
  }

  onChangeValue({target}) {
    if (target.name === 'country') {
      let countryId = Number(target.value);
      this.setState({countryId});
      this.updateRegions(countryId);
    } else {
      let value = target.dataset.type === 'int' ? Number(target.value) : target.value;

      let formData = _.extend({}, this.state.formData, {
        [target.name]: value
      });

      this.setState({formData});
    }
  }

  get countryCode() {
    const state = this.state;

    return state.country && state.country.alpha2;
  }

  validateZipCode() {
    const state = this.state;
    const countryCode = this.countryCode;
    const formData = state.formData;

    if (validators.zipCode(formData.zip, countryCode)) {
      return null;
    } else {
      return `${CountryStore.zipName(countryCode)} is invalid for selected country.`;
    }
  }

  get phoneInput() {
    const formData = this.state.formData;

    if (this.countryCode === 'US') {
      return <InputMask type="tel" name="phoneNumber" mask="(999)999-9999"
                        onChange={this.onChangeValue.bind(this)}
                        value={formData.phoneNumber} placeholder={CountryStore.phoneExample(this.countryCode)}/>;
    }
    return (
      <input type="tel" name="phoneNumber" value={formData.phoneNumber}
             maxLength="15" placeholder={CountryStore.phoneExample(this.countryCode)} />
    );
  }

  get errorMessages() {
    if (this.state.errors) {
      return (
        <div className="messages" ref="errorMessages">
          {this.state.errors.map((error, index) => {
            return <div className="fc-alert is-error"><i className="icon-error"></i>{error}</div>;
            })}
        </div>
      );
    }
    return null;
  }

  render() {
    const state = this.state;
    const formData = state.formData;
    const countries = state.countries || [];

    const countryCode = this.countryCode;
    const regions = state.country && state.country.regions || [];
    const title = this.isAddingForm ? 'New Address' : 'Edit Address';

    return (
      <div className="fc-content-box fc-address-form">
        <header className="header">
          <div className="fc-address-form-header">Address Book</div>
          <i onClick={this.close.bind(this)} className="icon-close" title="Close"></i>
        </header>
        {this.errorMessages}
        <article>
          <Form action={AddressStore.uri(this.props.customerId)}
                onSubmit={this.onSubmitForm.bind(this)}
                onChange={this.onChangeValue.bind(this)}>
            <ul className="fc-address-form-fields">
              <li>
                <div className="fc-address-form-field-title">{title}</div>
              </li>
              <li>
                <FormField label="Name" validator="ascii">
                  <input name="name" maxLength="255" type="text" value={formData.name} required />
                </FormField>
              </li>
              <li>
                <FormField label="Country">
                  <select name="country" value={this.state.countryId}>
                    {countries.map((country, index) => {
                      return <option value={country.id} key={`${index}-${country.id}`}>{country.name}</option>;
                      })}
                  </select>
                </FormField>
              </li>
              <li>
                <FormField label="Street Address" validator="ascii">
                  <input name="address1" maxLength="255" type="text" value={formData.address1} required />
                </FormField>
              </li>
              <li>
                <FormField label="Street Address 2" validator="ascii" optional>
                  <input name="address2" maxLength="255" type="text" value={formData.address2} />
                </FormField>
              </li>
              <li>
                <FormField label="City" validator="ascii">
                  <input name="city" maxLength="255" type="text" value={formData.city} required />
                </FormField>
              </li>
              <li>
                <FormField label={CountryStore.regionName(countryCode)}>
                  <select name="regionId" value={formData.regionId} data-type="int" required>
                    {regions.map((state, index) => {
                      return <option value={state.id} key={`${index}-${state.id}`}>{state.name}</option>;
                      })}
                  </select>
                </FormField>
              </li>
              <li>
                <FormField label={CountryStore.zipName(countryCode)} validator={this.validateZipCode.bind(this)}>
                  <input type="text" name="zip"
                         placeholder={CountryStore.zipExample(countryCode)}
                         value={formData.zip} className='control' required />
                </FormField>
              </li>
              <li>
                <FormField label="Phone Number" validator="phoneNumber">
                  {this.phoneInput}
                </FormField>
              </li>
              <li className="fc-address-form-controls">
                <a onClick={this.close.bind(this)} className="fc-btn-link" href="javascript:void(0)">Cancel</a>
                <button className="fc-btn fc-btn-primary" type="submit">Save and choose</button>
              </li>
            </ul>
            </Form>
        </article>
      </div>
    );
  }
}

AddressForm.propTypes = {
  address: React.PropTypes.object,
  customerId: React.PropTypes.number,
  onSaved: React.PropTypes.func
};
