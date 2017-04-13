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
import SaveCancel from '../common/save-cancel';

const propertyTypes = {
  string: 'Text',
  richText: 'Rich Text',
  date: 'Date',
  price: 'Price',
  bool: 'Yes/No',
  color: 'Color',
};

type Props = {
  onSave: (state: State) => void,
  onCancel: () => void,
  currentEdit: {
    name: string,
    type: string,
    value: string | number,
  }
};

type State = {
  fieldLabel: string,
  propertyType: string,
  fieldValue: string | number,
};

class CustomPropertyModal extends Component<void, Props, State> {
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {
      fieldLabel: '',
      propertyType: '',
      fieldValue: '',
    };
  }

  componentDidMount() {
    const fieldLabelInput = this.refs.field;
    if (fieldLabelInput) {
      fieldLabelInput.focus();
    }

    if (this.props.currentEdit) {
      this.setState({
        fieldLabel: this.props.currentEdit.name,
        propertyType: this.props.currentEdit.type,
        fieldValue: this.props.currentEdit.value,
      });
    }
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
  handleUpdateLabel({target}) {
    this.setState({ fieldLabel: target.value });
  }

  @autobind
  handleUpdateType(value) {
    this.setState({
      propertyType: value,
      fieldValue: ''
    });
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
              <input
                id="fct-field-label-fld"
                type="text"
                ref="field"
                className="fc-product-details__field-value"
                name="field"
                value={this.state.fieldLabel}
                onChange={this.handleUpdateLabel} />
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

export default wrapModal(CustomPropertyModal);
