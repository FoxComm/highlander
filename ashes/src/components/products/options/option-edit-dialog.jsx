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

  componentDidMount() {
    const { nameInput } = this.refs;
    if (nameInput) {
      nameInput.focus();
    }
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

  renderDialogContent(): Element<*> {
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
          <input
            id="fct-option-name-fld"
            type="text"
            ref="nameInput"
            value={name}
            onChange={({target}) => this.handleChange(target.value, 'name')}
          />
        </FormField>
        <FormField
          className="fc-object-form__field"
          label="Display Type"
          key={`object-form-attribute-type`}
        >
          <input
            id="option-display-type-fld"
            type="text"
            value={type}
            onChange={({target}) => this.handleChange(target.value, 'type')}
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
        body={this.renderDialogContent()}
        confirmLabel="Save option"
        onCancel={this.props.cancelAction}
        onConfirm={this.handleConfirm}
      />
    );
  }
}

export default OptionEditDialog;
