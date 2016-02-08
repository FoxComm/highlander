// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import InputMask from 'react-input-mask';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { createSelector } from 'reselect';

// components
import FormField from '../../forms/formfield';
import Form from '../../forms/form';
import ErrorAlerts from '../../alerts/error-alerts';
import SaveCancel from '../../common/save-cancel';
import { Dropdown, DropdownItem } from '../../dropdown';

// data
import * as validators from '../../../lib/validators';
import * as AddressFormActions from '../../../modules/address-form';
import {regionName, zipName, zipExample, phoneExample, phoneMask} from '../../../i18n';

const formNamespace = props => props.address && props.address.id || 'new';

const selectCurrentCountry = createSelector(
  state => state.countries,
  (state, props) => {
    const addressForm = state.addressForm[formNamespace(props)];
    return addressForm && addressForm.countryId;
  },
  (countries={}, countryId) => countries[countryId]
);

const sortCountries = createSelector(
  state => state.countries,
  (countries = {}) => _.values(countries).sort((a, b) => a.name < b.name ? -1 : 1)
);

function mapStateToProps(state, props) {
  return {
    countries: sortCountries(state),
    country: selectCurrentCountry(state, props),
    formData: {},
    ...state.addressForm[formNamespace(props)]
  };
}

function mapDispatchToProps(dispatch, props) {
  return _.transform(AddressFormActions, (result, action, key) => {
    result[key] = (...args) => {
      return dispatch(action(formNamespace(props), ...args));
    };
  });
}

@connect(mapStateToProps, mapDispatchToProps)
export default class AddressForm extends React.Component {

  static propTypes = {
    address: PropTypes.object,
    customerId: PropTypes.number,
    onSaved: PropTypes.func,
    closeAction: PropTypes.func.isRequired,
    submitAction: PropTypes.func,
    showFormTitle: PropTypes.bool,
    setAddress: PropTypes.func,
    changeValue: PropTypes.func,
    formData: PropTypes.object,
    err: PropTypes.any,
    isAdding: PropTypes.bool,
    countryId: PropTypes.number,
    saveTitle: PropTypes.node
  };

  static defaultProps = {
    showFormTitle: true,
    saveTitle: 'Save'
  };

  componentDidMount() {
    this.props.setAddress(this.props.address);
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
      default:
        return value;
    }
  }

  @autobind
  handleFormSubmit(event) {
    event.preventDefault();
    const props = this.props;

    const customerId = props.customerId;

    const formData = _.transform(props.formData, (result, value, name) => {
      result[name] = this.prepareValue(name, value);
    });

    let willSaved;

    if (props.submitAction) {
      willSaved = props.submitAction(formData);
    } else {
      willSaved = props.submitForm(customerId, formData);
    }

    willSaved
      .then(address => {
        if (props.onSaved) {
          props.onSaved(address.id);
        }

        props.closeAction();
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
                        value={formData.phoneNumber} placeholder={phoneExample(this.countryCode)}/>
      );
    }
    return (
      <input type="tel" name="phoneNumber" value={formData.phoneNumber}
             maxLength="15" placeholder={phoneExample(this.countryCode)} />
    );
  }

  get errorMessages() {
    return <ErrorAlerts error={this.props.err} />;
  }

  get formTitle() {
    if (this.props.showFormTitle) {
      const title = this.props.isAdding ? 'New Address' : 'Edit Address';

      return (
        <li>
          <div className="fc-address-form-field-title">{title}</div>
        </li>
      );
    }
  }

  render() {
    const props = this.props;
    const formData = props.formData;
    const countries = props.countries || [];

    const countryCode = this.countryCode;
    const regions = props.country && props.country.regions || [];

    return (
      <div className="fc-address-form">
        {this.errorMessages}
        <article>
          <Form onSubmit={this.handleFormSubmit}
                onChange={this.handleFormChange}>
            <ul className="fc-address-form-fields">
              { this.formTitle }
              <li>
                <FormField label="Name" validator="ascii" maxLength={255}>
                  <input name="name" type="text" value={formData.name} required />
                </FormField>
              </li>
              <li>
                <FormField label="Country">
                  <Dropdown value={props.countryId} onChange={value => props.changeValue('countryId', Number(value))}>
                    {countries.map((country, index) => {
                      return (
                        <DropdownItem value={country.id} key={`${index}-${country.id}`}>{country.name}</DropdownItem>
                      );
                    })}
                  </Dropdown>
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
                <FormField label={regionName(countryCode)} required>
                  <Dropdown value={formData.regionId} onChange={value => props.changeValue('regionId', Number(value))}>
                    {regions.map((state, index) => {
                      return <DropdownItem value={state.id} key={`${index}-${state.id}`}>{state.name}</DropdownItem>;
                    })}
                  </Dropdown>
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
                <SaveCancel onCancel={props.closeAction}
                            saveText={props.saveTitle}/>
              </li>
            </ul>
            </Form>
        </article>
      </div>
    );
  }
}
