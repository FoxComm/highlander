import React from 'react';
import PropTypes from 'prop-types';
import ItemCardContainer from './item-card-container';
import { Button, EditButton, DeleteButton } from 'components/core/button';
import { Checkbox } from 'components/core/checkbox';

export default class EditableItemCardContainer extends React.Component {

  static propTypes = {
    className: PropTypes.string.isRequired,
    id: PropTypes.string.isRequired,
    isDefault: PropTypes.bool,
    checkboxLabel: PropTypes.string,
    checkboxChangeHandler: PropTypes.func,
    deleteHandler: PropTypes.func,
    editHandler: PropTypes.func,
    chooseHandler: PropTypes.func,
    children: PropTypes.node
  };

  constructor(props, context) {
    super(props, context);
  }

  get isDefault() {
    const props = this.props;

    if (props.checkboxLabel) {
      return (
        <Checkbox
          id={`${props.id}-is-default`}
          label={props.checkboxLabel}
          checked={ props.isDefault }
          onChange={ props.checkboxChangeHandler }
        />
      );
    }
  }

  get editButton() {
    let editButton = null;
    if (this.props.editHandler) {
      editButton = (<EditButton onClick={ this.props.editHandler } />);
    }
    return editButton;
  }

  get deleteButton() {
    let deleteButton = null;
    if (this.props.deleteHandler) {
      deleteButton = (<DeleteButton onClick={ this.props.deleteHandler } />);
    }
    return deleteButton;
  }

  get chooseButton() {
    if (this.props.chooseHandler) {
      return <Button onClick={this.props.chooseHandler}>Choose</Button>;
    }
  }

  get buttons() {
    return (
      <div>
        { this.deleteButton }
        { this.editButton }
      </div>
    );
  }

  render() {
    return (
      <ItemCardContainer className={ this.props.className }
                         leftControls={ this.isDefault }
                         rightControls={ this.buttons }
                         chooseControl={ this.chooseButton }>
        { this.props.children }
      </ItemCardContainer>
    );
  }
}
