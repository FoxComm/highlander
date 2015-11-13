
import classNames from 'classnames';
import React, { PropTypes } from 'react';
import AddressDetails from './address-details';
import EditableItemCardContainer from '../item-card-container/editable-item-card-container';
import { Button } from '../common/buttons';

export default class AddressBox extends React.Component {

  static propTypes = {
    address: PropTypes.object,
    editAction: PropTypes.func,
    toggleDefaultAction: PropTypes.func,
    deleteAction: PropTypes.func,
    chooseAction: PropTypes.func,
    choosen: PropTypes.bool
  };

  get chooseButton() {
    if (this.props.chooseAction) {
      return (
        <div>
          <Button className="fc-address-choose" onClick={this.props.chooseAction} disabled={this.props.choosen}>
            Choose
          </Button>
        </div>
      );
    }
  }

  render() {
    const address = this.props.address;

    return (
      <EditableItemCardContainer className={ classNames('fc-address', {'is-active': this.props.choosen}) }
                                 checkboxLabel="Default shipping address"
                                 initiallyIsDefault={ address.isDefault }
                                 checkboxClickHandler={() => this.props.toggleDefaultAction(address) }
                                 editHandler={() => this.props.editAction(address) }
                                 deleteHandler={() => this.props.deleteAction(address) }>
        <AddressDetails address={address} />
        { this.chooseButton }
      </EditableItemCardContainer>
    );
  }
}
