/**
 * @flow
 */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';
import _ from 'lodash';

// components
import ConfirmationModal from 'components/core/confirmation-modal';
import { FormField, Form } from 'components/forms';
import TextInput from 'components/core/text-input';

// styles
import styles from './option-list.css';

type OptionEntry = {
  id: number|string,
  option: Option,
};

type Props = {
  option: ?OptionEntry,
  confirmAction: Function,
  cancelAction: Function,
};

type State = {
  option: Option,
};

class OptionEditDialog extends Component {
  props: Props;

  state: State = {
    option: _.get(this.props, 'option.option'),
  };

  get title(): string {
    return _.get(this.props, 'option.id') === 'new' ? 'New option' : 'Edit option';
  }

  @autobind
  handleChange(value: string, field: string): void {
    const option = assoc(this.state.option,
      ['attributes', field, 'v'], value
    );

    this.setState({option});
  }

  updateOption(): void {
    if (this.props.option != null) {
      this.props.confirmAction(this.props.option.id, this.state.option);
    }
  }

  @autobind
  handleConfirm() {
    if (this.refs.form.checkValidity()) {
      this.updateOption();
    }
  }

  get content(): Element<*> {
    const name = _.get(this.state, 'option.attributes.name.v');
    const type = _.get(this.state, 'option.attributes.type.v');

    return (
      <Form ref="form" styleName="option-edit-dialog">
        <FormField
          className="fc-object-form__field"
          label="Name"
          key={`object-form-attribute-name`}
          required
        >
          <TextInput
            id="fct-option-name-fld"
            ref="nameInput"
            value={name}
            name="name"
            onChange={this.handleChange}
            autoFocus
          />
        </FormField>
        <FormField
          className="fc-object-form__field"
          label="Display Type"
          key={`object-form-attribute-type`}
        >
          <TextInput
            id="option-display-type-fld"
            value={type}
            name="type"
            onChange={this.handleChange}
          />
        </FormField>
      </Form>
    );
  }

  render() {
    return (
      <ConfirmationModal
        isVisible
        title={this.title}
        confirmLabel="Save option"
        onCancel={this.props.cancelAction}
        onConfirm={this.handleConfirm}
      >
        {this.content}
      </ConfirmationModal>
    );
  }
}

export default OptionEditDialog;
