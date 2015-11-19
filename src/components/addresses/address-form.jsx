
import _ from 'lodash';
import { autobind } from 'core-decorators';
import React, { PropTypes } from 'react';
import FormField from '../forms/formfield';
import InputMask from 'react-input-mask';
import Form from '../forms/form';
import * as validators from '../../lib/validators';
import ErrorAlerts from '../alerts/error-alerts';
import ContentBox from '../content-box/content-box';
import modalWrapper from '../modal/wrapper';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import * as CountriesActions from '../../modules/countries';
import * as AddressFormActions from '../../modules/address-form';
import { createSelector } from 'reselect';
import {regionName, zipName, zipExample, phoneExample, phoneMask} from '../../i18n';

const formNamespace = props => props.address && props.address.id || 'new';

const selectCurrentCountry = createSelector(
  state => state.countries,
  (state, props) => {
    const addressForm = state.addressForm[formNamespace(props)];
    return addressForm && addressForm.countryId;
  },
  (countries={}, countryId) => countries[countryId]

);

function mapStateToProps(state, props) {
  return {
    countries: _.values(state.countries),
    country: selectCurrentCountry(state, props),
    formData: {},
    ...state.addressForm[formNamespace(props)]
  };
}

function mapDispatchToProps(dispatch, props) {
  const boundAddressFormActions = _.transform(AddressFormActions, (result, action, key) => {
    result[key] = (...args) => {
      return dispatch(action(formNamespace(props), ...args));
    };
  });

  return {
    ...boundAddressFormActions,
    ...bindActionCreators(CountriesActions, dispatch)
  };
}

@modalWrapper
@connect(mapStateToProps, mapDispatchToProps)
export default class AddressForm extends React.Component {

  static propTypes = {
    address: PropTypes.object,
    customerId: PropTypes.number,
    onSaved: PropTypes.func,
    closeAction: PropTypes.func.isRequired
  };

  componentDidMount() {
    this.props.fetchCountries();
    this.props.init(this.props.address);
  }

  /*componentWillUpdate(nextProps, nextState) {
    if (!this.props.error && nextState.error) {
      this.shouldScrollToErrors = true;
    }
  }

  componentDidUpdate() {
    if (this.shouldScrollToErrors && this.props.error) {
      this.refs.errorMessages.scrollIntoView();
      this.shouldScrollToErrors = false;
    }
  }*/

  @autobind
  handleFormSubmit(event) {
    event.preventDefault();

    const customerId = this.props.customerId;

    this.props.submitForm(customerId)
      .then(address => {
        if (this.props.onSaved) {
          this.props.onSaved(address.id);
        }

        this.props.closeAction();
      });
  }

  @autobind
  handleFormChange({target}) {
    const value = target.dataset.type === 'int' ? Number(target.value) : target.value;

    this.props.changeValue(target.name, value);
  }

  get countryCode() {
    const props = this.props;

    return props.country && props.country.alpha2;
  }

  validateZipCode() {
    const countryCode = this.countryCode;
    const formData = this.props.formData;

    if (validators.zipCode(formData.zip, countryCode)) {
      return null;
    } else {
      return `${zipName(countryCode)} is invalid for selected country`;
    }
  }

  get phoneInput() {
    const formData = this.props.formData;

    if (this.countryCode === 'US') {
      return (
        <InputMask type="tel" name="phoneNumber" mask={phoneMask(this.countryCode)}
                        onChange={this.handleFormChange}
                        formFieldTarget
                        value={formData.phoneNumber} placeholder={phoneExample(this.countryCode)}/>
      );
    }
    return (
      <input type="tel" name="phoneNumber" value={formData.phoneNumber}
             maxLength="15" placeholder={phoneExample(this.countryCode)} />
    );
  }

  get errorMessages() {
    return <ErrorAlerts error={this.props.err} ref="errorMessages"/>;
  }

  get actions() {
    return <i onClick={this.props.closeAction} className="fc-btn-close icon-close" title="Close"></i>;
  }

  render() {
    const props = this.props;
    const formData = props.formData;
    const countries = props.countries || [];

    const countryCode = this.countryCode;
    const regions = props.country && props.country.regions || [];
    const title = props.isAdding ? 'New Address' : 'Edit Address';

    return (
      <ContentBox title="Address Book" className="fc-address-form" actionBlock={this.actions}>
        {this.errorMessages}
        <article>
          <Form onSubmit={this.handleFormSubmit}
                onChange={this.handleFormChange}>
            <ul className="fc-address-form-fields">
              <li>
                <div className="fc-address-form-field-title">{title}</div>
              </li>
              <li>
                <FormField label="Name" validator="ascii" maxLength={255}>
                  <input name="name" type="text" value={formData.name} required />
                </FormField>
              </li>
              <li>
                <FormField label="Country">
                  <select name="countryId" data-type="int" value={this.props.countryId}>
                    {countries.map((country, index) => {
                      return <option value={country.id} key={`${index}-${country.id}`}>{country.name}</option>;
                      })}
                  </select>
                </FormField>
              </li>
              <li>
                <FormField label="Street Address" validator="ascii" maxLength={255}>
                  <input name="address1" type="text" value={formData.address1} required />
                </FormField>
              </li>
              <li>
                <FormField label="Street Address 2" validator="ascii" maxLength={255} optional>
                  <input name="address2" type="text" value={formData.address2} />
                </FormField>
              </li>
              <li>
                <FormField label="City" validator="ascii" maxLength={255}>
                  <input name="city" type="text" value={formData.city} required />
                </FormField>
              </li>
              <li>
                <FormField label={regionName(countryCode)}>
                  <select name="regionId" value={formData.regionId} data-type="int" required>
                    {regions.map((state, index) => {
                      return <option value={state.id} key={`${index}-${state.id}`}>{state.name}</option>;
                      })}
                  </select>
                </FormField>
              </li>
              <li>
                <FormField label={zipName(countryCode)} validator={this.validateZipCode.bind(this)}>
                  <input type="text" name="zip"
                         placeholder={zipExample(countryCode)}
                         value={formData.zip} className='control' required />
                </FormField>
              </li>
              <li>
                <FormField label="Phone Number" validator="phoneNumber">
                  {this.phoneInput}
                </FormField>
              </li>
              <li className="fc-address-form-controls">
                <a onClick={this.props.closeAction} className="fc-btn-link" href="javascript:void(0)">Cancel</a>
                <button className="fc-btn fc-btn-primary" type="submit">Save and choose</button>
              </li>
            </ul>
            </Form>
        </article>
      </ContentBox>
    );
  }
}

