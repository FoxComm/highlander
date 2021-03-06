/**
 * @flow
 */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';

// components
import ConfirmationModal from 'components/core/confirmation-modal';
import { FormField, Form } from 'components/forms';
import SwatchInput from 'components/core/swatch-input';
import TextInput from 'components/core/text-input';

// styles
import styles from './option-list.css';

type Props = {
  value: {
    id: string | number,
    value: OptionValue,
  },
  confirmAction: Function,
  cancelAction: Function,
};

type State = {
  value: OptionValue,
};

class ValueEditDialog extends Component {
  props: Props;

  state: State = {
    value: this.props.value.value,
  };

  get title(): string {
    return this.props.value.id === 'new' ? 'New value' : 'Edit value';
  }

  @autobind
  handleChange(newValue: string, field: string) {
    const value = assoc(this.state.value, field, newValue);

    this.setState({ value });
  }

  @autobind
  handleSwatchChange(newValue: string) {
    this.handleChange(newValue, 'swatch');
  }

  @autobind
  handleConfirm() {
    if (this.refs.form.checkValidity()) {
      this.save();
    }
  }

  save() {
    this.props.confirmAction(this.state.value, this.props.value.id);
  }

  get content() {
    const name = _.get(this.state, 'value.name', '');
    const swatch = _.get(this.state, 'value.swatch', '');

    return (
      <Form ref="form" styleName="option-edit-dialog">
        <FormField className="fc-object-form__field" label="Name" key={`object-form-attribute-name`} required>
          <TextInput
            id="fct-value-name-fld"
            value={name}
            ref="nameInput"
            onChange={value => this.handleChange(value, 'name')}
            autoFocus
          />
        </FormField>
        <FormField className="fc-object-form__field" label="Color Swatch" key={`object-form-attribute-swatch`}>
          <SwatchInput value={swatch} onChange={this.handleSwatchChange} />
        </FormField>
      </Form>
    );
  }

  render() {
    return (
      <ConfirmationModal
        isVisible
        title={this.title}
        confirmLabel="Save value"
        onCancel={this.props.cancelAction}
        onConfirm={this.handleConfirm}
      >
        {this.content}
      </ConfirmationModal>
    );
  }
}

export default ValueEditDialog;
