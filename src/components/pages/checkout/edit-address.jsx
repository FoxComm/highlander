
import _ from 'lodash';
import React, { Component } from 'react';
import styles from './checkout.css';
import { autobind, debounce } from 'core-decorators';
import { connect } from 'react-redux';

import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

import { TextInput } from 'ui/inputs';
import { FormField } from 'ui/forms';
import Autocomplete from 'ui/autocomplete';

import * as checkoutActions from 'modules/checkout';
import { AddressKind } from 'modules/checkout';

type EditShippingProps = Localized & {
  setAddressData: Function;
  selectedCountry: Object;
  state: Object;
}

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
    const { initAddressData, addressKind } = this.props;
    initAddressData(addressKind);
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
  changeCountry(item) {
    this.setAddressData('country', item);
  }

  @autobind
  changeState(item) {
    this.setAddressData('state', item);
  }

  render() {
    const props: EditShippingProps = this.props;
    const { countries, selectedCountry, data, t } = props;

    return (
      <div styleName="checkout-form">
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
        <div styleName="union-fields">
          <FormField styleName="text-field">
            <Autocomplete
              inputProps={{
                placeholder: t('COUNTRY'),
              }}
              getItemValue={item => item.name}
              items={countries}
              onSelect={this.changeCountry}
              selectedItem={selectedCountry}
            />
          </FormField>
          <FormField styleName="text-field" validator="zipCode">
            <TextInput required placeholder={t('ZIP')} onChange={this.handleZipChange} value={data.zip} />
          </FormField>
        </div>
        <div styleName="union-fields">
          <FormField styleName="text-field">
            <TextInput required name="city" placeholder={t('CITY')} onChange={this.changeFormData} value={data.city} />
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
        </div>
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
