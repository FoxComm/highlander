/**
 * @flow
 */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';
import _ from 'lodash';

// components
import ConfirmationDialog from 'components/modal/confirmation-dialog';
import { FormField } from 'components/forms';

// styles
import styles from './option-list.css';

// types
import type { Option } from 'paragons/product';

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
    const option = assoc(this.state.option, field, value);

    this.setState({option});
  }

  @autobind
  updateOption(): void {
    if (this.props.option != null) {
      this.props.confirmAction(this.state.option, this.props.option.id);
    }
  }

  renderDialogContent(): Element {
    const name = _.get(this.state, 'option.name');
    const type = _.get(this.state, 'option.type');

    return (
      <div styleName="option-edit-dialog">
        <FormField
          className="fc-object-form__field"
          label="Name"
          key={`object-form-attribute-name`} >
          <input
            type="text"
            value={name}
            onChange={({target}) => this.handleChange(target.value, 'name')}
          />
        </FormField>
        <FormField
          className="fc-object-form__field"
          label="Display Type"
          key={`object-form-attribute-type`} >
          <input
            type="text"
            value={type}
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
        confirmAction={this.updateOption}
      />
    )
  }
}

export default OptionEditDialog;
