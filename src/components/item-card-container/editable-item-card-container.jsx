import React, { PropTypes } from 'react';
import ItemCardContainer from './item-card-container';
import { Checkbox } from '../checkbox/checkbox';

export default class EditableItemCardContainer extends React.Component {

  static propTypes = {
    className: PropTypes.string.isRequired,
    isDefault: PropTypes.bool,
    checkboxLabel: PropTypes.string,
    checkboxChangeHandler: PropTypes.func,
    deleteHandler: PropTypes.func,
    editHandler: PropTypes.func,
    children: PropTypes.node
  };

  constructor(props, context) {
    super(props, context);
  }

  get isDefault() {
    const props = this.props;

    if (props.checkboxLabel) {
      return (
        <label className="fc-card-is-default">
          <Checkbox checked={ props.isDefault }
                    onChange={ props.checkboxChangeHandler } />
          <span>{ props.checkboxLabel }</span>
        </label>
      );
    }
  }

  get editButton() {
    let editButton = null;
    if (this.props.editHandler) {
      editButton = (<button className="fc-btn icon-edit" onClick={ this.props.editHandler }></button>);
    }
    return editButton;
  }

  get deleteButton() {
    let deleteButton = null;
    if (this.props.deleteHandler) {
      deleteButton = (<button className="fc-btn icon-trash" onClick={ this.props.deleteHandler }></button>);
    }
    return deleteButton;
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
                         rightControls={ this.buttons }>
        { this.props.children }
      </ItemCardContainer>
    );
  }
}
