
/* @flow */

import _ from 'lodash';
import React, { Component } from 'react';
import cssModules from 'react-css-modules';
import styles from './checkout.css';
import { reduxForm } from 'redux-form';
import { connect } from 'react-redux';

import Button from 'ui/buttons';
import { TextInput } from 'ui/inputs';
import EditableBlock from 'ui/editable-block';
import { FormField } from 'ui/forms';

import Autocomplete from 'ui/autocomplete';

import selectableList from 'selectors/countries';

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

/* ::`*/
@connect(state => ({
  countries: selectableList(state),
}))
@reduxForm({
  form: 'checkout-shipping',
  fields: ['name', 'address1', 'address2', 'country', 'zip', 'city', 'state', 'phone'],
},
  () => ({ // mapStateToProps
    initialValues: {
      country: 'USA',
    },
  })
)
@cssModules(styles)
/* ::`*/
class EditShipping extends Component {
  props: EditShippinProps;

  get initialCountryValue() {
    // $FlowFixMe: decorators are not supported
    const { fields: {country}, countries} = this.props;

    const item = _.find(countries, {id: country.value});
    return item && item.value;
  }

  render() {
    const props: EditShippinProps = this.props;
    const { handleSubmit } = props;
    // $FlowFixMe: decorators are not supported
    const { fields: {name, address1, address2, country, zip, city, state, phone}} = props;
    // $FlowFixMe: decorators are not supported
    const { countries } = props;

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
              items={countries}
              onSelect={item => country.onChange(item.id)}
              initialValue={this.initialCountryValue}
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
            <TextInput placeholder="STATE" {...state} />
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
      title="SHIPPING"
      isEditing={props.isEditing}
      editAction={props.editAction}
      viewContent={<ViewShipping />}
      editContent={<EditShipping {...props} />}
    />
  );
};

export default cssModules(Shipping, styles);
