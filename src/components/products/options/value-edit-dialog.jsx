/**
 * @flow
 */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';

// components
import ConfirmationDialog from 'components/modal/confirmation-dialog';
import { FormField } from 'components/forms';

// styles
import styles from './option-list.css';

// types
import type { Option, OptionValue } from 'paragons/product';

type Props = {
  option: Option,
  value: {
    id: string|number,
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

    this.setState({value});
  }

  @autobind
  save() {
    this.props.confirmAction(this.state.value, this.props.value.id);
  }

  renderDialogContent() {
    return (
      <div styleName="option-edit-dialog">
        <FormField
          className="fc-object-form__field"
          label="Name"
          key={`object-form-attribute-name`}
          required={true}
        >
          <input
            type="text"
            value={this.state.value.name}
            onChange={({target}) => this.handleChange(target.value, 'name')}
          />
        </FormField>
        <FormField
          className="fc-object-form__field"
          label="Color Swatch"
          key={`object-form-attribute-swatch`}
        >
          <input
            type="text"
            value={this.state.value.swatch}
            onChange={({target}) => this.handleChange(target.value, 'swatch')}
          />
        </FormField>
      </div>
    )
  }

  render(): Element {
    return (
      <ConfirmationDialog
        isVisible={true}
        header={this.title}
        body={this.renderDialogContent()}
        cancel="Cancel"
        confirm="Save value"
        cancelAction={this.props.cancelAction}
        confirmAction={this.save}
      />
    )
  }
}

export default ValueEditDialog;
