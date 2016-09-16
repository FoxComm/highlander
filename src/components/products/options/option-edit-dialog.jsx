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
  option: { [key:string]: Variant },
};

class OptionEditDialog extends Component {
  props: Props;

  state: State = {
    option: this.props.option.option,
  };

  get title() {
    return this.props.option.id === 'new' ? 'New option' : 'Edit option';
  }

  @autobind
  handleChange(value, field) {
    const option = assoc(this.state.option, field, value);

    this.setState({option});
  }

  @autobind
  saveOption() {
    this.props.confirmAction(this.state.option, this.props.option.id);
  }

  renderDialogContent() {
    return (
      <div styleName="new-option-dialog">
        <FormField
          className="fc-object-form__field"
          labelClassName="fc-object-form__field-label"
          label="Name"
          key={`object-form-attribute-name`}
        >
          <input
            type="text"
            value={this.state.option.name}
            onChange={({target}) => this.handleChange(target.value, 'name')}
          />
        </FormField>
        <FormField
          className="fc-object-form__field"
          labelClassName="fc-object-form__field-label"
          label="Display Type"
          key={`object-form-attribute-type`}
        >
          <input
            type="text"
            value={this.state.option.type}
            onChange={({target}) => this.handleChange(target.value, 'type')}
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
        confirm="Save option"
        cancelAction={this.props.cancelAction}
        confirmAction={this.saveOption}
      />
    )
  }
}

export default OptionEditDialog;
