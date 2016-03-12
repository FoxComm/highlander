
/* @flow */

import _ from 'lodash';
import React, { Component } from 'react';
import cssModules from 'react-css-modules';
import styles from './checkout.css';
import { autobind, debounce } from 'core-decorators';
import { connect } from 'react-redux';

import Button from 'ui/buttons';
import { TextInput } from 'ui/inputs';
import EditableBlock from 'ui/editable-block';
import { FormField } from 'ui/forms';

import Autocomplete from 'ui/autocomplete';

type ShippingProps = {
  isEditing: boolean;
  editAction: () => any;
  continueAction: Function;
};

type FieldValue = {
  value: any;
}

type ViewProps = {
  name: FieldValue;
  address1: FieldValue;
  address2: FieldValue;
  country: FieldValue;
  zip: FieldValue;
  city: FieldValue;
  state: FieldValue;
  phone: FieldValue;
}

let ViewShipping = (props: any) => {
  return <span>v</span>;

  const values = _.transform(props, (result, {value}, key) => {
    result[key] = value;
  });

  return (
    <ul>
      <li><strong>{values.name}</strong></li>
      <li>{values.address1}</li>
      {values.address2 && <li>{values.address2}</li>}
      <li>{values.city}, {values.state} {values.zip}</li>
      <li>{values.country.name}</li>
      {values.phone && <li>{values.phone}</li>}
    </ul>
  );
};
ViewShipping = connect(state => (state.form['checkout-shipping'] || {}))(ViewShipping);

type EditShippinProps = {
  continueAction?: Function;
  handleSubmit?: Function;
}

const DEFAULT_COUNTRY = 'USA';

function mapStateToProps(state) {
  const currentCountry = {alpha3: DEFAULT_COUNTRY};

  const countries = state.countries.list;
  const selectedCountry = _.find(countries, {alpha3: _.get(currentCountry, 'alpha3', DEFAULT_COUNTRY)});
  const countryDetails = state.countries.details[selectedCountry && selectedCountry.id] || {
    regions: [],
  };

  return {
    countries: state.countries.list,
    selectedCountry: countryDetails,
    initialValues: {
      country: selectedCountry,
      state: countryDetails.regions[0] || {},
    },
  };
}

const validate = values => {
  const errors = {};
  return errors;
};

/* ::`*/
@connect(mapStateToProps)
@cssModules(styles)
/* ::`*/
class EditShipping extends Component {
  props: EditShippinProps;
  lookupXhr: ?XMLHttpRequest;

  get initialCountryValue() {
    return '';

    if (country.value) {
      const item = _.find(countries, {alpha3: country.value.alpha3});
      return item && item.name;
    }
  }

  @debounce(200)
  tryAutopopulateFromZip() {
    return false;
    // $FlowFixMe: decorators are not supported
    const { selectedCountry } = this.props;

    if (zip.value && selectedCountry.alpha3 == 'USA') {
      if (this.lookupXhr) {
        this.lookupXhr.abort();
        this.lookupXhr = null;
      }

      this.lookupXhr = makeXhr(`/lookup-zip/usa/${zip.value}`).then(
        result => {
          city.onChange(result.city);
          const currentState = _.find(selectedCountry.regions, region => {
            return region.name.toLowerCase() == result.state.toLowerCase();
          });
          if (currentState) {
            state.onChange(currentState);
          }
        },
        err => {
          console.error(err);
        }
      );
    }
  }

  @autobind
  handleZipChange(event) {
    this.tryAutopopulateFromZip();
  }

  render() {
    const props: EditShippinProps = this.props;
    // $FlowFixMe: decorators are not supported
    // $FlowFixMe: decorators are not supported
    const { countries, selectedCountry } = props;

    return (
      <form styleName="checkout-form">
        <FormField styleName="checkout-field">
          <TextInput placeholder="FIRST & LAST NAME" />
        </FormField>
        <FormField styleName="checkout-field">
          <TextInput placeholder="STREET ADDRESS 1" />
        </FormField>
        <FormField styleName="checkout-field">
          <TextInput placeholder="STREET ADDRESS 2 (optional)" />
        </FormField>
        <div styleName="union-fields">
          <FormField styleName="checkout-field">
            <Autocomplete
              inputProps={{
                placeholder: 'COUNTRY',
              }}
              getItemValue={item => item.name}
              items={countries}
              onSelect={item => console.log(item)}
            />
          </FormField>
          <FormField styleName="checkout-field">
            <TextInput placeholder="ZIP" onChange={this.handleZipChange} />
          </FormField>
        </div>
        <div styleName="union-fields">
          <FormField styleName="checkout-field">
            <TextInput placeholder="CITY" />
          </FormField>
          <FormField styleName="checkout-field">
            <Autocomplete
              inputProps={{
                placeholder: 'STATE',
              }}
              getItemValue={item => item.name}
              items={selectedCountry.regions}
              onSelect={item => console.log(item)}
            />
          </FormField>
        </div>
        <FormField styleName="checkout-field">
          <TextInput placeholder="PHONE" />
        </FormField>
        <Button onClick={props.continueAction}>CONTINUE</Button>
      </form>
    );
  }
}


const Shipping = (props: ShippingProps) => {
  return (
    <EditableBlock
      styleName="shipping"
      title="SHIPPING"
      isEditing={props.isEditing}
      editAction={props.editAction}
      viewContent={<ViewShipping />}
      editContent={<EditShipping {...props} />}
    />
  );
};

export default cssModules(Shipping, styles);
