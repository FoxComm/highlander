
// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind, debounce } from 'core-decorators';
import { connect } from 'react-redux';

// localization
import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

// components
import { TextInput } from 'ui/inputs';
import { FormField } from 'ui/forms';
import Autocomplete from 'ui/autocomplete';
import Checkbox from 'ui/checkbox/checkbox';
import Loader from 'ui/loader';

// styles
import styles from '../checkout.css';

// actions
import * as checkoutActions from 'modules/checkout';
import { AddressKind } from 'modules/checkout';

type EditShippingProps = Localized & {
  setAddressData: Function,
  selectedCountry: Object,
  state: Object,
};

function mapStateToProps(state, props) {
  const { addressKind } = props;
  const addressData = addressKind == AddressKind.SHIPPING
    ? state.checkout.shippingAddress
    : state.checkout.billingAddress;

  const countries = state.countries.list;
  const selectedCountry = _.find(countries, {alpha3: _.get(addressData.country, 'alpha3', 'USA')});
  const countryDetails = state.countries.details[selectedCountry && selectedCountry.id] || {
    regions: [],
  };

  return {
    countries: state.countries.list,
    selectedCountry: countryDetails,
    state: _.get(addressData, 'state', countryDetails.regions[0]) || {},
    data: addressData,
  };
}

/* ::`*/
@connect(mapStateToProps, checkoutActions)
@localized
/* ::`*/
export default class EditAddress extends Component {
  props: EditShippingProps;
  lookupXhr: ?XMLHttpRequest;

  componentDidMount() {
    const { initAddressData, addressKind, address } = this.props;
    initAddressData(addressKind, address);
  }

  setAddressData(key, value) {
    const { setAddressData, addressKind } = this.props;

    setAddressData(addressKind, key, value);
  }

  @debounce(200)
  tryAutopopulateFromZip(zip) {
    // $FlowFixMe: decorators are not supported
    const { selectedCountry } = this.props;

    if (zip && selectedCountry.alpha3 == 'USA') {
      if (this.lookupXhr) {
        this.lookupXhr.abort();
        this.lookupXhr = null;
      }

      this.lookupXhr = makeXhr(`/lookup-zip/usa/${zip}`).then(
        result => {
          this.setAddressData('city', result.city);
          const currentState = _.find(selectedCountry.regions, region => {
            return region.name.toLowerCase() == result.state.toLowerCase();
          });
          if (currentState) {
            this.setAddressData('state', currentState);
          }
        },
        err => {
          console.error(err);
        }
      );
    }
  }

  @autobind
  handleZipChange({target}) {
    this.setAddressData('zip', target.value);

    this.tryAutopopulateFromZip(target.value);
  }

  @autobind
  changeFormData({target}) {
    this.setAddressData(target.name, target.value);
  }

  @autobind
  changeState(item) {
    this.setAddressData('state', item);
  }

  @autobind
  changeDefault(value) {
    this.setAddressData('isDefault', value);
  }

  render() {
    if (!this.props.isAddressLoaded) return <Loader size="m"/>;

    const props: EditShippingProps = this.props;
    const { selectedCountry, data, t } = props;

    const checked = _.get(data, 'isDefault', false);

    return (
      <div styleName="checkout-form">
        <Checkbox
          styleName="checkbox-field"
          name="isDefault"
          checked={checked}
          onChange={({target}) => this.changeDefault(target.checked)}
          id="set-default-address"
        >
          Make this address my default
        </Checkbox>

        <FormField styleName="text-field">
          <TextInput required
            name="name" placeholder={t('FIRST & LAST NAME')} value={data.name} onChange={this.changeFormData}
          />
        </FormField>
        <FormField styleName="text-field">
          <TextInput
            required
            name="address1" placeholder={t('STREET ADDRESS 1')} value={data.address1} onChange={this.changeFormData}
          />
        </FormField>
        <FormField styleName="text-field">
          <TextInput
            name="address2" placeholder={t('STREET ADDRESS 2 (optional)')} value={data.address2}
            onChange={this.changeFormData}
          />
        </FormField>
        <FormField styleName="text-field" validator="zipCode">
          <TextInput required placeholder={t('ZIP')} onChange={this.handleZipChange} value={data.zip} />
        </FormField>
        <FormField styleName="text-field">
          <TextInput required name="city" placeholder={t('CITY')} onChange={this.changeFormData} value={data.city}/>
        </FormField>
        <FormField styleName="text-field">
          <Autocomplete
            inputProps={{
              placeholder: t('STATE'),
            }}
            getItemValue={item => item.name}
            items={selectedCountry.regions}
            onSelect={this.changeState}
            selectedItem={props.state}
          />
        </FormField>
        <FormField label={t('Phone Number')} styleName="text-field" validator="phoneNumber">
          <TextInput
            required
            name="phoneNumber"
            type="tel"
            placeholder={t('PHONE')}
            onChange={this.changeFormData}
            value={data.phoneNumber}
          />
        </FormField>
      </div>
    );
  }
}
