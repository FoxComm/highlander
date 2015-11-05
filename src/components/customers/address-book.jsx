import React, { PropTypes } from 'react';
import ContentBox from '../content-box/content-box';
import AddressBox from '../addresses/address-box';
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
    addresses: PropTypes.array
  }

  componentDidMount() {
    const customer = this.props.customerId;

    this.props.fetchAddresses(customer);
  }

  get actionBlock() {
    return (
      <button className="fc-btn">
        <i className="icon-add"></i>
      </button>
    );
  }

  render() {
    let createAddressBox = (addr) => {
      let key = `cutomer-address-${ addr.id }`;
      return (
        <AddressBox key={ key }
                    address={ addr }
                    customerId={ this.props.customerId } />
      );
    };
    return (
      <ContentBox title="Address Book"
                  className="fc-customer-address-book"
                  actionBlock={ this.actionBlock }>

        <ul className="fc-float-list">
          {(this.props.addresses && this.props.addresses.map(createAddressBox))}
        </ul>
      </ContentBox>
    );
  }
}
