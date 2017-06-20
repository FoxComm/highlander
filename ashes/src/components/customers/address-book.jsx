
import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';
import ContentBox from '../content-box/content-box';
import Addresses, { createAddressBox } from '../addresses/addresses';
import AddressBox from '../addresses/address-box';
import AddressForm from '../addresses/address-form';
import ItemCardContainer from '../item-card-container/item-card-container';
import { AddButton } from 'components/core/button';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import * as AddressesDetailsActions from '../../modules/customers/addresses-details';
import * as AddressesActions from '../../modules/customers/addresses';

@connect((state, props) => ({
  ...state.customers.addressesDetails
}), {
  ...AddressesDetailsActions,
  ...AddressesActions
})
export default class CustomerAddressBook extends React.Component {

  static propTypes = {
    customerId: PropTypes.number.isRequired,
    fetchAddresses: PropTypes.func,
    patchAddress: PropTypes.func,
    createAddress: PropTypes.func,
    startAddingAddress: PropTypes.func,
    stopAddingAddress: PropTypes.func,
    stopEditingAddress: PropTypes.func,
    addresses: PropTypes.array.isRequired,
  };

  componentWillUnmount() {
    this.props.stopEditingAddress();
    this.props.stopAddingAddress();
  }

  @autobind
  injectNewAddressCard(addresses) {
    const props = this.props;

    if (props.isAdding) {
      const title = <div className="fc-address-new-title">New Address</div>;

      return [
        ...addresses,
        <ItemCardContainer
          key="address-new"
          leftControls={ title }
          className="fc-address is-editing">
          <AddressForm
            customerId={props.customerId}
            onCancel={() => props.stopAddingAddress()}
            showFormTitle={ false }
            submitAction={this.handleSubmitAddress}
          />
        </ItemCardContainer>
      ];
    }

    return addresses;
  }

  @autobind
  handleSubmitAddress(address) {
    if (address.id) {
      this.props.patchAddress(this.props.customerId, address.id, address);
      this.props.stopEditingAddress(address.id);
    } else {
      this.props.createAddress(this.props.customerId, address);
      this.props.stopAddingAddress();
    }
  }

  @autobind
  createAddressBox(address, idx, props) {
    if (_.includes(props.editingIds, address.id)) {
      return (
        <AddressBox
          key={`address-${idx}`}
          address={address}
          toggleDefaultAction={() => props.setAddressDefault(props.customerId, address.id, !address.isDefault)}
          editAction={() => props.stopEditingAddress(address.id)}
          className="is-editing">
          <AddressForm
            address={address}
            customerId={props.customerId}
            onCancel={() => props.stopEditingAddress(address.id)}
            showFormTitle={ false }
            submitAction={this.handleSubmitAddress}
          />
        </AddressBox>
      );
    } else {
      return createAddressBox(address, idx, props);
    }
  }

  render() {
    const props = this.props;

    return (
      <ContentBox title="Address Book"
                  className="fc-customer-address-book"
                  actionBlock={
                    <AddButton id="fct-add-btn__new-address"
                               onClick={() => props.startAddingAddress(props.customerId)}
                    />
                  }
                  editAction={() => props.startEditingAddress(address.id)}
      >

        <Addresses
          {...props}
          processContent={ this.injectNewAddressCard }
          createAddressBox={ this.createAddressBox }
        />
      </ContentBox>
    );
  }
}
