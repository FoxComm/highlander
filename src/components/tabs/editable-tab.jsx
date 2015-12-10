import React, { PropTypes } from 'react';
import classnames from 'classnames';
import  { Button } from '../common/buttons';
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
    editing: PropTypes.bool,
    startEdit: PropTypes.func
  };

  static defaultProps = {
    cancelEdit: _.noop,
    completeEdit: _.noop,
    editing: false,
    startEdit: _.noop
  };

  get className() {
    return classnames({ '_editing': this.props.editing });
  }

  get tabContent() {
    if (this.props.editing) {
      return (
        <div className="fc-tab__edit-content fc-form-field">
          <input
            autoFocus
            className="fc-tab__edit-content-input"
            type="text"
            onBlur={() => this.props.completeEdit(this.state.editValue)}
            onChange={this.changeInput}
            placeholder="Name your search"
            value={this.state.editValue}
          />
          <div className="fc-tab__edit-content-close">
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

  get editButton() {
    if (!this.props.editing) {
      return (
        <button className="fc-tab__edit-icon" onClick={this.props.startEdit}>
          <i className="icon-edit"/>
        </button>
      );
    }
  }

  @autobind
  changeInput({target}) {
    this.setState({
      ...this.state,
      editValue: target.value
    });
  }

  preventAction(event) {
    event.preventDefault();
    event.stopPropagation();
  }

  render() {
    return (
      <TabView className={this.className} {...this.props}>
        {this.tabContent}
        {this.editButton}
      </TabView>
    );
  }
}