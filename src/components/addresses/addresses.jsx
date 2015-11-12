import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { Button } from '../common/buttons';
import AddressBox from './address-box';
import AddressForm from './address-form';
import AddressStore from '../../stores/addresses';

export default class AddressBook extends React.Component {

  static propTypes = {
    onSelectAddress: PropTypes.func,
    onDeleteAddress: PropTypes.func,
    isAddressSelected: PropTypes.func
  };

  constructor(props, context) {
    super(props, context);

    let customerId;
    if (this.props.order) {
      customerId = this.props.order.customer.id;
    } else if (this.props.params && this.props.params.customer) {
      customerId = this.props.params.customer;
    } else {
      throw new Error('customer not provided to AddressBook');
    }

    this.state = {
      addresses: [],
      customerId: customerId
    };
  }

  componentDidMount() {
    AddressStore.listenToEvent('change', this);
    AddressStore.fetch(customerId);
  }

  componentWillUnmount() {
    AddressStore.stopListeningToEvent('change', this);
  }

  onChangeAddressStore(customerId, addresses) {
    if (customerId === this.state.customerId) {
      this.setState({addresses});
    }
  }

  @autobind
  addNew() {
    dispatch('toggleModal', <AddressForm customerId={this.state.customerId} />);
  }

  render() {
    const addresses = this.state.addresses;

    return (
      <div className="fc-addresses">
        <header>
          <div className="fc-addresses-title">Address Book</div>
          <Button icon="add" onClick={this.addNew} />
        </header>
        <ul className="fc-addresses-list">
          {addresses.map((address, idx) => {
            return (
              <AddressBox key={`${idx}-${address.id}`}
                address={address}
                choosen={this.props.isAddressSelected ? this.props.isAddressSelected(address) : false}
                chooseAction={this.props.chooseAction}
                onDeleteAddress={this.props.onDeleteAddress}
                customerId={this.state.customerId}
              />
            );
          })}
        </ul>
      </div>
    );
  }
}
