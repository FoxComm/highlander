
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
    checkboxLabel: PropTypes.string,
    deleteAction: PropTypes.func,
    chooseAction: PropTypes.func,
    choosen: PropTypes.bool
  };

  static defaultProps = {
    checkboxLabel: 'Default shipping address'
  };

  get chooseButton() {
    const props = this.props;

    if (props.chooseAction) {
      return (
        <div>
          <Button className="fc-address-choose"
                  onClick={() => props.chooseAction(props.address)} disabled={props.choosen}>
            Choose
          </Button>
        </div>
      );
    }
  }

  render() {
    const props = this.props;
    const address = props.address;

    return (
      <EditableItemCardContainer className={ classNames('fc-address', {'is-active': props.choosen}) }
                                 checkboxLabel={ props.checkboxLabel }
                                 initiallyIsDefault={ address.isDefault }
                                 checkboxClickHandler={() => props.toggleDefaultAction(address) }
                                 editHandler={() => props.editAction(address) }
                                 deleteHandler={ props.deleteAction && () => props.deleteAction(address) }>
        <AddressDetails address={address} />
        { this.chooseButton }
      </EditableItemCardContainer>
    );
  }
}
