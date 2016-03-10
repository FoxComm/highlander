
/* @flow */

import _ from 'lodash';
import React, { Component } from 'react';
import cssModules from 'react-css-modules';
import styles from './checkout.css';
import { reduxForm } from 'redux-form';

import Button from 'ui/buttons';
import { TextInput } from 'ui/inputs';
import EditableBlock from 'ui/editable-block';
import { FormField } from 'ui/forms';

import Autocomplete from 'ui/autocomplete';

type ShippingProps = {
  isEditing: boolean;
  editAction: () => any;
};

const ViewShipping = () => {
  return <span>view content</span>;
};

type EditShippinProps = {
  continueAction?: Function;
  handleSubmit?: Function;
  fields?: Object;
}

const DEFAULT_COUNTRY = 'USA';

function mapStateToProps(state) {
  const currentCountry = _.get(state.form, 'checkout-shipping.country.value');

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

/* ::`*/
@reduxForm({
  form: 'checkout-shipping',
  fields: ['name', 'address1', 'address2', 'country', 'zip', 'city', 'state', 'phone'],
}, mapStateToProps)
@cssModules(styles)
/* ::`*/
class EditShipping extends Component {
  props: EditShippinProps;

  get initialCountryValue() {
    // $FlowFixMe: decorators are not supported
    const { fields: {country}, countries} = this.props;

    if (country.value) {
      const item = _.find(countries, {alpha3: country.value.alpha3});
      return item && item.name;
    }
  }

  render() {
    const props: EditShippinProps = this.props;
    const { handleSubmit } = props;
    // $FlowFixMe: decorators are not supported
    const { fields: {name, address1, address2, country, zip, city, state, phone}} = props;
    // $FlowFixMe: decorators are not supported
    const { countries, selectedCountry } = props;

    return (
      <form onSubmit={handleSubmit} styleName="checkout-form">
        <FormField styleName="checkout-field" field={name}>
          <TextInput placeholder="FIRST & LAST NAME" {...name} />
        </FormField>
        <FormField styleName="checkout-field" field={address1}>
          <TextInput placeholder="STREET ADDRESS 1" {...address1} />
        </FormField>
        <FormField styleName="checkout-field" field={address2}>
          <TextInput placeholder="STREET ADDRESS 2 (optional)" {...address2} />
        </FormField>
        <div styleName="union-fields">
          <FormField styleName="checkout-field" field={country}>
            <Autocomplete
              inputProps={{
                placeholder: 'COUNTRY',
              }}
              getItemValue={item => item.name}
              items={countries}
              onSelect={item => country.onChange(item)}
              selectedItem={country.value}
            />
          </FormField>
          <FormField styleName="checkout-field" field={zip}>
            <TextInput placeholder="ZIP" {...zip} />
          </FormField>
        </div>
        <div styleName="union-fields">
          <FormField styleName="checkout-field" field={city}>
            <TextInput placeholder="CITY" {...city} />
          </FormField>
          <FormField styleName="checkout-field" field={state}>
            <Autocomplete
              inputProps={{
                placeholder: 'STATE',
              }}
              getItemValue={item => item.name}
              items={selectedCountry.regions}
              onSelect={item => state.onChange(item)}
              selectedItem={state.value}
            />
          </FormField>
        </div>
        <FormField styleName="checkout-field" field={phone}>
          <TextInput placeholder="PHONE" {...phone} />
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
