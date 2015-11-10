import React, { PropTypes } from 'react';
import AddressDetails from './address-details';
import EditableItemCardContainer from '../item-card-container/editable-item-card-container';

export default class AddressBox extends React.Component {

  static propTypes = {
    address: PropTypes.object,
    customerId: PropTypes.number.isRequired
  };

  constructor(props, context) {
    super(props, context);
  }

  handleIsDefaultChange() {
    console.log('Is default state changed');
  }

  handleEditClick() {
    console.log('Edit button action triggered');
  }

  handleDeleteClick() {
    console.log('Delete button action triggered');
  }

  render() {
    let address = this.props.address;

    return (
      <EditableItemCardContainer className="fc-customer-address"
                                 checkboxLabel="Default shipping address"
                                 initiallyIsDefault={ address.isDefault }
                                 checkboxClickHandler={ this.handleIsDefaultChange }
                                 editHandler={ this.handleEditClick }
                                 deleteHandler={ this.handleDeleteClick }>
        <AddressDetails address={address} />
      </EditableItemCardContainer>
    );
  }
}
