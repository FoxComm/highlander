
import React, { PropTypes } from 'react';
import ContentBox from '../content-box/content-box';
import Addresses from '../addresses/addresses';
import AddressBox from '../addresses/address-box';
import { AddButton } from '../common/buttons';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import * as CustomerAddressesActions from '../../modules/customers/addresses';

@connect((state, props) => ({
  ...state.customers.addresses[props.customerId]
}), CustomerAddressesActions)
export default class CustomerAddressBook extends React.Component {

  static propTypes = {
    customerId: PropTypes.number.isRequired,
    fetchAddresses: PropTypes.func,
    startAddingAddress: PropTypes.func,
    addresses: PropTypes.array
  };

  componentDidMount() {
    this.props.fetchAddresses(this.props.customerId);
  }

  @autobind
  injectNewAddressCard(addresses) {
    if (this.props.isAdding) {
      return [
        ...addresses,
        <div>
          Hey! Wanna add some more new addresses ?
        </div>
      ];
    }

    return addresses;
  }

  render() {
    const props = this.props;

    return (
      <ContentBox title="Address Book"
                  className="fc-customer-address-book"
                  actionBlock={ <AddButton onClick={() => props.startAddingAddress(props.customerId)}/> }>

        <Addresses {...props} processContent={ this.injectNewAddressCard } />
      </ContentBox>
    );
  }
}
