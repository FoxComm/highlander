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
import type { Variant } from 'paragons/product';

type Props = {
  option: Variant,
};

type State = {
  value: { [key:string]: Variant },
};

class ValueEditDialog extends Component {
  props: Props;

  state: State = {
    value: this.props.value.value,
  };

  get title() {
    return this.props.value.id === 'new' ? 'New value' : 'Edit value';
  }

  @autobind
  handleChange(newValue, field) {
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
          labelClassName="fc-object-form__field-label"
          label="Name"
          key={`object-form-attribute-name`}
        >
          <input
            type="text"
            value={this.state.value.name}
            onChange={({target}) => this.handleChange(target.value, 'name')}
          />
        </FormField>
        <FormField
          className="fc-object-form__field"
          labelClassName="fc-object-form__field-label"
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
