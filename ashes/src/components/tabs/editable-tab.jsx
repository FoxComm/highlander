// libs
import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import TabView from './tab';
import TextInput from 'components/core/text-input';

export default class EditableTabView extends React.Component {
  state = {
    editValue: this.props.defaultValue,
  };

  static propTypes = {
    defaultValue: PropTypes.string.isRequired,
    isDirty: PropTypes.bool,
    isEditable: PropTypes.bool,
    editMode: PropTypes.bool,
    onEditNameComplete: PropTypes.func,
    onEditNameCancel: PropTypes.func,
  };

  static defaultProps = {
    isDirty: false,
    isEditable: true,
    editMode: false,
    onEditNameComplete: _.noop,
    onEditNameCancel: _.noop,
  };

  get className() {
    return classnames({ _editing: this.props.editMode });
  }

  get dirtyState() {
    if (this.props.isDirty && !this.props.editMode) {
      return <div className="fc-editable-tab__dirty-icon">&nbsp;</div>;
    }
  }

  @autobind
  endEditName() {
    this.props.onEditNameComplete(this.state.editValue);
  }

  @autobind
  cancelEdit(event) {
    this.preventAction(event);

    this.setState(
      {
        editValue: this.props.defaultValue,
      },
      () => this.props.onEditNameCancel()
    );
  }

  get tabContent() {
    if (this.props.editMode) {
      return (
        <div className="fc-editable-tab__content fc-form-field">
          <TextInput
            autoFocus
            className="fc-editable-tab__content-input"
            onBlur={this.endEditName}
            onChange={this.changeInput}
            onClick={this.preventAction}
            onKeyDown={this.keyDown}
            placeholder="Name your search"
            value={this.state.editValue}
          />
          <div className="fc-editable-tab__content-close">
            <a onClick={this.cancelEdit} onMouseDown={this.preventAction} onMouseUp={this.preventAction}>
              &times;
            </a>
          </div>
        </div>
      );
    } else {
      return this.props.defaultValue;
    }
  }

  @autobind
  changeInput(value) {
    this.setState({ editValue: value });
  }

  @autobind
  keyDown(event) {
    switch (event.keyCode) {
      case 13:
        this.preventAction(event);
        this.endEditName();
        break;
      case 27:
        this.preventAction(event);
        this.cancelEdit(event);
        break;
    }
  }

  preventAction(event) {
    event.preventDefault();
    event.stopPropagation();
  }

  render() {
    return (
      <div className="fc-editable-tab" ref="theTab">
        {this.dirtyState}
        <TabView className={this.className} draggable={this.props.isEditable} {...this.props}>
          {this.tabContent}
        </TabView>
      </div>
    );
  }
}
