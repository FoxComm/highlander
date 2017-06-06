/* @flow */

// libs
import React, { Component } from 'react';

// components
import InlineForm from './inline-form';
import Icon from 'components/core/icon';

type Props = {
  label: string,
  value: string,
};

type State = {
  isEditing: bool,
};

export default class InlineField extends Component<void, Props, State> {
  props: Props;
  state: State;

  constructor(...args: Array<any>) {
    super(...args);

    this.state = { isEditing: false };
  }

  handleStartEdit() {
    this.setState({ isEditing: true });
  }

  handleCancelEdit() {
    this.setState({ isEditing: false });
  }

  handleSave(contents: { [key:string]: any }) {
    this.setState({ isEditing: false });
  }

  render() {
    let valueElement = null;

    if (this.state.isEditing) {
      valueElement = (
        <InlineForm
          label={this.props.label}
          type="string"
          value={this.props.value}
          onSave={this.handleSave.bind(this)}
          onCancel={this.handleCancelEdit.bind(this)} />
      );
    } else {
      valueElement = (
        <div className="fc-inline-field__value" onClick={this.handleStartEdit.bind(this)}>
          <div>{this.props.value}</div>
          <Icon name="edit" />
        </div>
      );
    }

    return (
      <div className="fc-inline-field">
        <div className="fc-inline-field__label">{this.props.label}</div>
        {valueElement}
      </div>
    );
  }
}
