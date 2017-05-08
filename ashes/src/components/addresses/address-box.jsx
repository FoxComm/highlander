
import { noop } from 'lodash';
import classNames from 'classnames';
import React from 'react';
import PropTypes from 'prop-types';
import AddressDetails from './address-details';
import EditableItemCardContainer from '../item-card-container/editable-item-card-container';
import { Button } from 'components/core/button';

const AddressBoxMainAction = props => {
  return (
    <div className="fc-address-main-action" {...props}>
      {props.children}
    </div>
  );
};

AddressBoxMainAction.propTypes = {
  children: PropTypes.node
};

export default class AddressBox extends React.Component {

  static propTypes = {
    className: PropTypes.string,
    address: PropTypes.object,
    editAction: PropTypes.func.isRequired,
    toggleDefaultAction: PropTypes.func,
    checkboxLabel: PropTypes.string,
    deleteAction: PropTypes.func,
    chooseAction: PropTypes.func,
    chosen: PropTypes.bool,
    actionBlock: PropTypes.node,
    children: PropTypes.node
  };

  static defaultProps = {
    checkboxLabel: 'Default shipping address',
    toggleDefaultAction: noop,
    deleteAction: noop,
  };

  get chooseButton() {
    const props = this.props;

    if (props.chooseAction) {
      return (
        <AddressBoxMainAction>
          <Button onClick={() => props.chooseAction(props.address)} disabled={props.chosen}>
            Choose
          </Button>
        </AddressBoxMainAction>
      );
    }
  }

  get mainActionBlock() {
    const props = this.props;

    if (props.actionBlock) {
      return (
        <AddressBoxMainAction>
          {props.actionBlock}
        </AddressBoxMainAction>
      );
    }

    return this.chooseButton;
  }

  get content() {
    if (this.props.children) {
      return this.props.children;
    } else {
      return (
        <div>
          <AddressDetails address={this.props.address} />
          { this.mainActionBlock }
        </div>
      );
    }
  }

  render() {
    const props = this.props;
    const address = props.address;

    return (
      <EditableItemCardContainer className={ classNames('fc-address', props.className, {'is-active': props.chosen}) }
                                 id={`address-${address.id}`}
                                 checkboxLabel={ props.checkboxLabel }
                                 isDefault={ address.isDefault }
                                 checkboxChangeHandler={() => props.toggleDefaultAction(address)}
                                 editHandler={() => props.editAction(address) }
                                 deleteHandler={() => props.deleteAction(address) }>
        {this.content}
      </EditableItemCardContainer>
    );
  }
}
