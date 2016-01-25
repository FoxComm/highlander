import React, { PropTypes } from 'react';
import classnames from 'classnames';
import { Button } from '../common/buttons';
import TabView from './tab';
import { autobind } from 'core-decorators';
import _ from 'lodash';

import EditSearchOptions from '../live-search/edit-search-options';

export default class EditableTabView extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = EditableTabView.updateState({ isEditingMenu: false }, props);
  }

  componentWillReceiveProps(nextProps) {
    this.setState(EditableTabView.updateState(this.state, nextProps));
  }

  static propTypes = {
    cancelEdit: PropTypes.func,
    completeEdit: PropTypes.func,
    defaultValue: PropTypes.string.isRequired,
    isDirty: PropTypes.bool,
    isEditable: PropTypes.bool,
    startEdit: PropTypes.func,
    editMenuOptions: PropTypes.array
  };

  static defaultProps = {
    cancelEdit: _.noop,
    completeEdit: _.noop,
    isDirty: false,
    isEditable: true,
    startEdit: _.noop,
    editMenuOptions: []
  };

  static updateState(currentState, props) {
    return {
      ...currentState,
      editValue: props.defaultValue,
    };
  }

  get className() {
    return classnames({ '_editing': this.state.isEditing });
  }

  get dirtyState() {
     if (this.props.isDirty && !this.state.isEditing) {
      return <div className="fc-editable-tab__dirty-icon">&nbsp;</div>;
     }
  }

  get editButton() {
    if (!this.state.isEditing && this.props.isEditable) {
      return (
        <button className="fc-editable-tab__edit-icon" onClick={this.startEdit}>
          <i className="icon-edit"/>
        </button>
      );
    }
  }

  get editMenu() {
    if (!_.isEmpty(this.props.editMenuOptions)) {
      return (
        <EditSearchOptions
          isVisible={this.state.isEditingMenu}
          options={this.props.editMenuOptions} />
      );
    }
  }
  
  @autobind
  startEdit(event) {
    if (_.isEmpty(this.props.editMenuOptions)) {
      this.setState({ ...this.state, isEditing: true });
    } else {
      event.stopPropagation();
      this.setState({ ...this.state, isEditingMenu: true });
    }
  }

  @autobind
  cancelEdit(event) {
    this.preventAction(event);
    this.setState({ ...this.state, isEditing: false });
  }

  get tabContent() {
    if (this.state.isEditing) {
      return (
        <div className="fc-editable-tab__content fc-form-field">
          <input
            autoFocus
            className="fc-editable-tab__content-input"
            type="text"
            onBlur={this.blur}
            onChange={this.changeInput}
            onClick={this.preventAction}
            onKeyDown={this.keyDown}
            placeholder="Name your search"
            value={this.state.editValue}
          />
          <div className="fc-editable-tab__content-close">
            <a
              onClick={this.cancelEdit}
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
  blur(event) {
    this.setState({ ...this.state, isEditing: false });
    this.props.completeEdit(this.state.editValue);
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
        <TabView className={this.className} draggable={this.props.isEditable} {...this.props}>
          {this.tabContent}
          <div className="fc-editable-tab__edit-icon-container">
            {this.editButton}
          </div>
        </TabView>
        {this.editMenu}
      </div>
    );
  }
}
