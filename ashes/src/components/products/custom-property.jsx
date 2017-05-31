/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import { Dropdown } from '../dropdown';
import { FormField } from '../forms';
import wrapModal from '../modal/wrapper';
import ContentBox from '../content-box/content-box';
import SaveCancel from 'components/core/save-cancel';
import TextInput from 'components/core/text-input';

const propertyTypes = {
  string: 'Text',
  richText: 'Rich Text',
  date: 'Date',
  price: 'Price',
  bool: 'Yes/No',
};

type Props = {
  onSave: (state: State) => void,
  onCancel: () => void,
};

type State = {
  fieldLabel: string,
  propertyType: string,
};

class CustomProperty extends Component<void, Props, State> {
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {
      fieldLabel: '',
      propertyType: '',
    };
  }

  get closeAction(): Element<*> {
    return <a onClick={this.props.onCancel}>&times;</a>;
  }

  get propertyTypes(): Array<Element<*>> {
    return _.map(propertyTypes, (type, key) => [key, type]);
  }

  get saveDisabled(): boolean {
    return _.isEmpty(this.state.fieldLabel) || _.isEmpty(this.state.propertyType);
  }

  @autobind
  handleUpdateLabel(value) {
    this.setState({ fieldLabel: value });
  }

  @autobind
  handleUpdateType(value) {
    this.setState({ propertyType: value });
  }

  @autobind
  handleSave(event) {
    event.preventDefault();
    this.props.onSave(this.state);
  }

  @autobind
  handleKeyPress(event){
    if (!this.saveDisabled && event.keyCode === 13 /*enter*/) {
      event.preventDefault();
      this.props.onSave(this.state);
    }
  }

  render() {
    return (
      <div className="fc-product-details__custom-property">
        <div className="fc-modal-container" onKeyDown={this.handleKeyPress}>
          <ContentBox title="New Custom Property" actionBlock={this.closeAction}>
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
                name="type"
                value={this.state.propertyType}
                onChange={this.handleUpdateType}
                items={this.propertyTypes}
              />
            </FormField>
            <SaveCancel
              onCancel={this.props.onCancel}
              onSave={this.handleSave}
              saveDisabled={this.saveDisabled}
              saveText="Save and Apply" />
          </ContentBox>
        </div>
      </div>
    );
  }
}

const Wrapped: Class<Component<void, Props, State>> = wrapModal(CustomProperty);

export default Wrapped;
