import React, { PropTypes } from 'react';
import ReactDOM from 'react-dom';
import classnames from 'classnames';
import { autobind } from 'core-decorators';
import _ from 'lodash';

import { Button } from '../common/buttons';
import Menu from '../menu/menu';
import MenuItem from '../menu/menu-item';
import TabView from './tab';

export default class EditableTabView extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = EditableTabView.updateState({ isEditingMenu: false }, props);
  }

  componentDidMount() {
    document.addEventListener('click', this.onDocumentClick);
  }

  componentWillReceiveProps(nextProps) {
    this.setState(EditableTabView.updateState(this.state, nextProps));
  }

  componentWillUnmount() {
    document.removeEventListener('click', this.onDocumentClick);
  }

  static propTypes = {
    defaultValue: PropTypes.string.isRequired,
    isDirty: PropTypes.bool,
    isEditable: PropTypes.bool,
    startEdit: PropTypes.func,
    editMenuOptions: PropTypes.array,
    onSaveUpdateComplete: PropTypes.func,
    onEditNameComplete: PropTypes.func,
    onCopySearchComplete: PropTypes.func,
    onDeleteSearchComplete: PropTypes.func,
    completeEdit: PropTypes.func,
  };

  static defaultProps = {
    isDirty: false,
    isEditable: true,
    startEdit: _.noop,
    editMenuOptions: [],
    onSaveUpdateComplete: _.noop,
    onEditNameComplete: _.noop,
    onCopySearchComplete: _.noop,
    onDeleteSearchComplete: _.noop,
    completeEdit: _.noop,
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
        <button ref="editIcon" className="fc-editable-tab__edit-icon" onClick={this.startEdit}>
          <i className="icon-edit" />
        </button>
      );
    }
  }

  get editMenuOptions() {
    const saveAction = this.props.isDirty
      ? [{ title: 'Save Search Update', action: this.props.onSaveUpdateComplete }]
      : [];

    return [
      ...saveAction,
      { title: 'Edit Name', action: this.startEditName },
      { title: 'Copy Search', action: this.props.onCopySearchComplete },
      { title: 'Delete Search', action: this.props.onDeleteSearchComplete }
    ];
  }

  get editMenu() {
    if (this.state.isEditingMenu) {
      const options = this.editMenuOptions.map((opt, idx) => {
        const key = `${idx}-${_.kebabCase(opt.title)}`;

        return (
          <MenuItem isFirst={idx == 0} clickAction={opt.action} key={key}>
            {opt.title}
          </MenuItem>
        );
      });

      return <Menu>{options}</Menu>;
    }
  }

  @autobind
  onDocumentClick(event) {
    const editIcon = ReactDOM.findDOMNode(this.refs.editIcon);
    const isEditClick = editIcon && editIcon.contains(event.target);

    if (!isEditClick && this.state.isEditingMenu) {
      this.setState({ isEditingMenu: false });
    }
  }

  @autobind
  startEdit(event) {
    event.preventDefault();
    event.stopPropagation();
    this.setState({ isEditingMenu: true });
  }

  @autobind
  startEditName() {
    this.setState({
      isEditing: true,
      isEditingMenu: false
    });
  }

  @autobind
  endEditName() {
    this.setState({
      isEditing: false
    }, () => this.props.onEditNameComplete(this.state.editValue));
  }

  @autobind
  cancelEdit(event) {
    this.preventAction(event);
    this.setState({
      isEditing: false,
      editValue: this.props.defaultValue,
    });
  }

  get tabContent() {
    if (this.state.isEditing) {
      return (
        <div className="fc-editable-tab__content fc-form-field">
          <input
            autoFocus
            className="fc-editable-tab__content-input"
            type="text"
            onBlur={this.endEditName}
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
  changeInput({ target }) {
    this.setState({ editValue: target.value });
  }

  @autobind
  keyDown(event) {
    if (event.keyCode == 13) {
      this.preventAction(event);
      this.endEditName();
    }
    if (event.keyCode == 27) {
      this.preventAction(event);
      this.cancelEdit(event);
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
          <div className="fc-editable-tab__edit-icon-container">
            {this.editButton}
          </div>
          {this.editMenu}
        </TabView>
      </div>
    );
  }
}
