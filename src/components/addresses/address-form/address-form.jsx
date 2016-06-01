// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import InputMask from 'react-input-mask';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { createSelector } from 'reselect';

// components
import FormField from '../../forms/formfield';
import FoxyForm from '../../forms/foxy-form';
import ErrorAlerts from '../../alerts/error-alerts';
import SaveCancel from '../../common/save-cancel';
import { Dropdown, DropdownItem } from '../../dropdown';
import TextInput from '../../forms/text-input';

// data
import * as validators from '../../../lib/validators';
import * as AddressFormActions from '../../../modules/address-form';
import * as CountryActions from '../../../modules/countries';
import {regionName, zipName, zipExample, phoneExample, phoneMask} from '../../../i18n';

const formNamespace = props => _.get(props, 'address.id', 'new');

const sortCountries = createSelector(
  state => state.countries,
  (countries = {}) => _.values(countries).sort((a, b) => a.name < b.name ? -1 : 1)
);

function mapStateToProps(state, props) {
  return {
    countries: sortCountries(state),
    ...state.addressForm[formNamespace(props)]
  };
}

function mapDispatchToProps(dispatch, props) {
  const aActions = _.transform(AddressFormActions, (result, action, key) => {
    result[key] = (...args) => {
      return dispatch(action(formNamespace(props), ...args));
    };
  });

  const cActions = bindActionCreators(CountryActions, dispatch);

  return { ...aActions, ...cActions };
}

@connect(mapStateToProps, mapDispatchToProps)
export default class AddressForm extends React.Component {

  static propTypes = {
    address: PropTypes.object,
    countries: PropTypes.array,
    customerId: PropTypes.number.isRequired,
    err: PropTypes.any,
    isAdding: PropTypes.bool,
    saveTitle: PropTypes.node,
    showFormTitle: PropTypes.bool,

    closeAction: PropTypes.func.isRequired,
    fetchCountry: PropTypes.func.isRequired,
    submitAction: PropTypes.func.isRequired,
  };

  static defaultProps = {
    address: {},
    saveTitle: 'Save',
    showFormTitle: true,
  };

  constructor(...args) {
    super(...args);

    const countryId = _.get(this.props,
      'address.countryId',
      _.find(this.props.countries, { alpha2: 'US' }).id
    );

    this.state = {
      countryId: countryId,
      phone: _.get(this.props, 'address', {}).phoneNumber
    };
  }

  componentDidMount() {
    this.props.fetchCountry(this.state.countryId);
  }

  get country() {
    return _.find(this.props.countries, { id: this.state.countryId });
  }

  get countryCode() {
    return _.get(this.country, 'alpha2');
  }

  get phoneInput() {
    const inputAttributes = {
      type: 'tel',
      name: 'phoneNumber',
      placeholder: phoneExample(this.countryCode),
      value: this.state.phone,
      onChange: ({target}) => this.handlePhoneChange(target.value),
    };

    return (this.countryCode === 'US')
      ? <InputMask {...inputAttributes} mask={phoneMask(this.countryCode)}/>
      : <TextInput {...inputAttributes} maxLength="15"/>;
  }

  get regionItems() {
    const regions = _.get(this.country, 'regions', []);
    return _.map(regions, region => {
      const key = `dd-item-region-${region.name}`;
      return <DropdownItem value={region.id} key={key}>{region.name}</DropdownItem>;
    });
  }

  get countryItems() {
    const countries = _.get(this.props, 'countries', []);
    return _.map(countries, country => {
      const key = `dd-item-country-${country.id}`;
      return <DropdownItem value={country.id} key={key}>{country.name}</DropdownItem>;
    });
  }

  get errorMessages() {
    return <ErrorAlerts error={this.props.err} />;
  }

  get formTitle() {
    if (this.props.showFormTitle) {
      const title = this.props.address.id ? 'Edit Address' : 'New Address';

      return (
        <li>
          <div className="fc-address-form-field-title">{title}</div>
        </li>
      );
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
      case 'countryId':
      case 'regionId':
        return parseInt(value);
      default:
        return value;
    }
  }

  @autobind
  handleCountryChange(countryId) {
    this.setState({
      countryId: countryId,
    }, () => this.props.fetchCountry(countryId));
  }

  handlePhoneChange(phone) {
    this.setState({phone});
  }

  @autobind
  handleFormSubmit(data) {
    const { submitAction } = this.props;

    const formData = _.reduce(data, (res, val, key) => {
      if (val !== '') {
        res[key] = this.prepareValue(key, val);
      }

      return res;
    }, this.props.address);

    submitAction(formData);
  }

  @autobind
  validateZipCode(value, label) {
    const countryCode = this.countryCode;

    if (validators.zipCode(value, countryCode)) {
      return null;
    } else {
      return `${zipName(countryCode)} is invalid for selected country`;
    }
  }

  render() {
    const { address, closeAction, saveTitle } = this.props;
    const countryCode = this.countryCode;
    const regionId = _.get(address, 'region.id');

    return (
      <div className="fc-address-form">
        {this.errorMessages}
        <article>
          <FoxyForm onSubmit={this.handleFormSubmit}>
            <ul className="fc-address-form-fields">
              {this.formTitle}
              <li>
                <FormField label="Name" validator="ascii" maxLength={255}>
                  <input name="name" type="text" defaultValue={address.name} required />
                </FormField>
              </li>
              <li>
                <FormField label="Country">
                  <Dropdown name="countryId"
                            value={this.state.countryId}
                            onChange={value => this.handleCountryChange(Number(value))}>
                    {this.countryItems}
                  </Dropdown>
                </FormField>
              </li>
              <li>
                <FormField label="Street Address" validator="ascii" maxLength={255}>
                  <input name="address1" type="text" defaultValue={address.address1} required />
                </FormField>
              </li>
              <li>
                <FormField label="Street Address 2" validator="ascii" maxLength={255} optional>
                  <input name="address2" type="text" defaultValue={address.address2} />
                </FormField>
              </li>
              <li>
                <FormField label="City" validator="ascii" maxLength={255}>
                  <input name="city" type="text" defaultValue={address.city} required />
                </FormField>
              </li>
              <li>
                <FormField label={regionName(countryCode)} required>
                  <Dropdown name="regionId" value={regionId}>
                    {this.regionItems}
                  </Dropdown>
                </FormField>
              </li>
              <li>
                <FormField label={zipName(countryCode)} validator={this.validateZipCode}>
                  <input type="text" name="zip"
                         placeholder={zipExample(countryCode)}
                         defaultValue={address.zip} className='control' required />
                </FormField>
              </li>
              <li>
                <FormField label="Phone Number" validator="phoneNumber">
                  {this.phoneInput}
                </FormField>
              </li>
              <li className="fc-address-form-controls">
                <SaveCancel onCancel={closeAction}
                            saveText={saveTitle}/>
              </li>
            </ul>
          </FoxyForm>
        </article>
      </div>
    );
  }
}
