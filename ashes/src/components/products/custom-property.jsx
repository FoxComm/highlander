/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import Modal from 'components/core/modal';
import { Dropdown } from 'components/dropdown';
import { FormField } from 'components/forms';
import SaveCancel from 'components/core/save-cancel';
import TextInput from 'components/core/text-input';

// styles
import s from './custom-property.css';

const propertyTypes = {
  string: 'Text',
  richText: 'Rich Text',
  date: 'Date',
  price: 'Price',
  bool: 'Yes/No',
};

type Props = {
  isVisible: boolean,
  onSave: (state: State) => void,
  onCancel: () => void,
};

type State = {
  fieldLabel: string,
  propertyType: string,
};

export default class CustomProperty extends Component<void, Props, State> {
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {
      fieldLabel: '',
      propertyType: '',
    };
  }

  get propertyTypes(): Array<Element<*>> {
    return _.map(propertyTypes, (type, key) => [key, type]);
  }

  get saveDisabled(): boolean {
    return _.isEmpty(this.state.fieldLabel) || _.isEmpty(this.state.propertyType);
  }

  @autobind
  handleUpdateLabel(value: string) {
    this.setState({ fieldLabel: value });
  }

  @autobind
  handleUpdateType(value: string) {
    this.setState({ propertyType: value });
  }

  @autobind
  handleSave(event: Event) {
    event.preventDefault();

    this.props.onSave(this.state);
  }

  get footer() {
    return (
      <SaveCancel
        onCancel={this.props.onCancel}
        onSave={this.handleSave}
        saveDisabled={this.saveDisabled}
        saveLabel="Save and Apply"
      />
    );
  }

  render() {
    return (
      <Modal
        className={s.modal}
        title="New Custom Property"
        isVisible={this.props.isVisible}
        footer={this.footer}
        onClose={this.props.onCancel}
      >
        <FormField
          className="fc-product-details__field"
          label="Field Label"
          labelClassName="fc-product-details__field-label">
          <TextInput
            id="fct-field-label-fld"
            ref="field"
            name="field"
            value={this.state.fieldLabel}
            onChange={this.handleUpdateLabel}
            autoFocus
          />
        </FormField>
        <FormField
          className="fc-product-details__field"
          label="Field Type"
          labelClassName="fc-product-details__field-label">
          <Dropdown
            id="fct-field-type-dd"
            className={s.dropdown}
            name="type"
            value={this.state.propertyType}
            onChange={this.handleUpdateType}
            items={this.propertyTypes}
          />
        </FormField>
      </Modal>
    );
  }
}
