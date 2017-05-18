/**
 * @flow
 */

// libs
import { isEmpty, map, upperFirst } from 'lodash';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';

// components
import { Dropdown } from 'components/dropdown';
import { FormField } from 'components/forms';
import wrapModal from 'components/modal/wrapper';
import ContentBox from 'components/content-box/content-box';
import SaveCancel from 'components/core/save-cancel';
import renderers from 'components/object-form/renderers';

//style
import s from './custom-property-modal.css';

const propertyTypes = {
  string: 'Text',
  richText: 'Rich Text',
  date: 'Date',
  price: 'Price',
  bool: 'Yes/No',
  color: 'Color',
  image: 'Image'
};

type Props = {
  onSave: (state: State) => any,
  onCancel: () => any,
  currentEdit?: {
    name: string,
    type: string,
    value: any,
  }
};

type State = {
  fieldLabel: string,
  propertyType: string,
  fieldValue: any,
};

class CustomPropertyModal extends Component<void, Props, State> {
  props: Props;

  state: State = {
    fieldLabel: '',
    propertyType: '',
    fieldValue: '',
  };

  constructor(props: Props) {
    super(props);
    Object.keys(propertyTypes).map((type) => {
      if (Object.keys(renderers).indexOf(`render${upperFirst(type)}`) === -1) {
        console.warn(`Custom property type: "${type}", does not have renderer!`);
      }
    });
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
    return map(propertyTypes, (type, key) => [key, type]);
  }

  get saveDisabled(): boolean {
    return isEmpty(this.state.fieldLabel) || isEmpty(this.state.propertyType);
  }

  @autobind
  handleUpdateLabel({ target }) {
    this.setState({ fieldLabel: target.value });
  }

  @autobind
  handleUpdateType(value) {
    const fieldValue = (() => {
      switch (value) {
        case('date'):
          return new Date().toString();
        case('bool'):
          return false;
        case('image'):
          return {};
        default:
          return '';
      }
    })();

    this.setState({
      propertyType: value,
      fieldValue: fieldValue
    });
  }

  @autobind
  handleSave(event) {
    event.preventDefault();
    this.props.onSave(this.state);
  }

  @autobind
  handleKeyPress(event) {
    if (!this.saveDisabled && event.keyCode === 13 /*enter*/) {
      event.preventDefault();
      this.props.onSave(this.state);
    }
  }

  render() {
    const title = this.props.currentEdit ? 'Edit Custom Property' : 'New Custom Property';

    return (
      <div className={s.modal}>
        <div className="fc-modal-container" onKeyDown={this.handleKeyPress}>
          <ContentBox title={title} actionBlock={this.closeAction}>
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

const Wrapped: Class<Component<void, Props, State>> = wrapModal(CustomPropertyModal);

export default Wrapped;
