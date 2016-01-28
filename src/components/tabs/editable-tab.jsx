import React, { PropTypes } from 'react';
import classnames from 'classnames';
import { Button } from '../common/buttons';
import TabView from './tab';
import { autobind } from 'core-decorators';
import _ from 'lodash';

export default class EditableTabView extends React.Component {
  constructor(props, context) {
    super(props, context);

    this.state = {
      editValue: props.defaultValue
    };
  }

  static propTypes = {
    cancelEdit: PropTypes.func,
    completeEdit: PropTypes.func,
    defaultValue: PropTypes.string.isRequired,
    isDirty: PropTypes.bool,
    isEditable: PropTypes.bool,
    isEditing: PropTypes.bool,
    startEdit: PropTypes.func
  };

  static defaultProps = {
    cancelEdit: _.noop,
    completeEdit: _.noop,
    isDirty: false,
    isEditable: true,
    isEditing: false,
    startEdit: _.noop
  };

  get className() {
    return classnames({ '_editing': this.props.isEditing });
  }

  get dirtyState() {
     if (this.props.isDirty && !this.props.isEditing) {
      return <div className="fc-editable-tab__dirty-icon">&nbsp;</div>;
     }
  }

  get editButton() {
    if (!this.props.isEditing && this.props.isEditable) {
      return (
        <button className="fc-editable-tab__edit-icon" onClick={this.props.startEdit}>
          <i className="icon-edit"/>
        </button>
      );
    }
  }

  get tabContent() {
    if (this.props.isEditing) {
      return (
        <div className="fc-editable-tab__content fc-form-field">
          <input
            autoFocus
            className="fc-editable-tab__content-input"
            type="text"
            onBlur={() => this.props.completeEdit(this.state.editValue)}
            onChange={this.changeInput}
            onKeyDown={this.keyDown}
            placeholder="Name your search"
            value={this.state.editValue}
          />
          <div className="fc-editable-tab__content-close">
            <a
              onClick={this.props.cancelEdit}
              onMouseDown={this.preventAction}
              onMouseUp={this.preventAction}>
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
  changeInput({target}) {
    this.setState({
      ...this.state,
      editValue: target.value
    });
  }

  @autobind
  keyDown(event) {
    if (event.keyCode == 13) {
      event.preventDefault();
      this.props.completeEdit(this.state.editValue);
    }
  }

  preventAction(event) {
    event.preventDefault();
    event.stopPropagation();
  }

  render() {
    return (
      <div className="fc-editable-tab">
        {this.dirtyState}
        <TabView className={this.className} {...this.props}>
          {this.tabContent}
          <div className="fc-editable-tab__edit-icon-container">
            {this.editButton}
          </div>
        </TabView>
      </div>
    );
  }
}
