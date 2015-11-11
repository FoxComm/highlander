import _ from 'lodash';
import { autobind } from 'core-decorators';
import React, { PropTypes } from 'react';
import FormField from '../forms/formfield';
import InputMask from 'react-input-mask';
import Form from '../forms/form';
import CountryStore from '../../stores/countries';
import AddressStore from '../../stores/addresses';
import * as validators from '../../lib/validators';
import ErrorAlerts from '../alerts/error-alerts';
import ContentBox from '../content-box/content-box';
import modalWrapper from '../modal/wrapper';
import { connect } from 'react-redux';
import * as CountriesActions from '../../modules/countries';
import * as AddressFormActions from '../../modules/addressForm';
import { createSelector } from 'reselect';

const DEFAULT_COUNTRY = 'US';

const selectCurrentCountry = createSelector(
  state => state.countries,
  state => state.addressForm.countryId,
  (countries, countryId) => countries[countryId]
);

@connect(state => state, {
  ...CountriesActions,
  ...AddressFormActions
})
@modalWrapper
export default class AddressForm extends React.Component {

  static propTypes = {
    address: PropTypes.object,
    customerId: PropTypes.number,
    onSaved: PropTypes.func,
    closeAction: PropTypes.func.isRequired
  };

  get isAddingForm() {
    return !this.props.address;
  }

  componentDidMount() {
    this.props.fetchCountries();
    this.props.assignAddress(this.props.address);
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
      .catch(error => {
        this.setState({error});
      });
  }

  componentWillUpdate(nextProps, nextState) {
    if (!this.state.error && nextState.error) {
      this.shouldScrollToErrors = true;
    }
  }

  componentDidUpdate() {
    if (this.shouldScrollToErrors && this.state.error) {
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

  @autobind
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
        this.setState({error: null});

        if (this.props.onSaved) {
          this.props.onSaved(address.id);
        }
      })
      .then(() => this.props.onClose())
      .catch(error => {
        this.setState({error});
      });
  }

  @autobind
  onChangeValue({target}) {
    const value = target.dataset.type === 'int' ? Number(target.value) : target.value;

    this.props.changeValue(target.name, value);
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
      return `${CountryStore.zipName(countryCode)} is invalid for selected country`;
    }
  }

  get phoneInput() {
    const formData = this.state.formData;

    if (this.countryCode === 'US') {
      return (
        <InputMask type="tel" name="phoneNumber" mask={CountryStore.phoneMask(this.countryCode)}
                        onChange={this.onChangeValue}
                        formFieldTarget
                        value={formData.phoneNumber} placeholder={CountryStore.phoneExample(this.countryCode)}/>
      );
    }
    return (
      <input type="tel" name="phoneNumber" value={formData.phoneNumber}
             maxLength="15" placeholder={CountryStore.phoneExample(this.countryCode)} />
    );
  }

  get errorMessages() {
    return <ErrorAlerts error={this.state.error} ref="errorMessages"/>;
  }

  get actions() {
    return <i onClick={this.props.closeAction} className="fc-btn-close icon-close" title="Close"></i>;
  }

  render() {
    const state = this.state;
    const formData = state.formData;
    const countries = state.countries || [];

    const countryCode = this.countryCode;
    const regions = state.country && state.country.regions || [];
    const title = this.isAddingForm ? 'New Address' : 'Edit Address';

    return (
      <ContentBox title="Address Book" className="fc-address-form" actionBlock={this.actions}>
        {this.errorMessages}
        <article>
          <Form action={AddressStore.uri(this.props.customerId)}
                onSubmit={this.onSubmitForm}
                onChange={this.onChangeValue}>
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
                  <select name="countryId" data-type="int" value={this.state.countryId}>
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

