/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import { Checkbox } from '../checkbox/checkbox';
import { Dropdown, DropdownItem } from '../dropdown';
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
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = { 
      fieldLabel: '',
      propertyType: '',
    };
  }

  get closeAction(): Element {
    return <a onClick={this.props.onCancel}>&times;</a>;
  }

  get propertyTypes(): Array<Element> {
    return _.map(propertyTypes, (type, key) => {
      return <DropdownItem key={`type-${key}`} value={key}>{type}</DropdownItem>;
    });
  }

  @autobind
  handleUpdateLabel({target}) {
    this.setState({ fieldLabel: target.value });
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

  render(): Element {
    const saveDisabled = _.isEmpty(this.state.fieldLabel) || _.isEmpty(this.state.propertyType);
    return (
      <div className="fc-product-details__custom-property">
        <div className="fc-modal-container">
          <ContentBox title="New Custom Property" actionBlock={this.closeAction}>
            <FormField 
              className="fc-product-details__field"
              label="Field Label"
              labelClassName="fc-product-details__field-label">
              <input
                type="text"
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
                name="type"
                value={this.state.propertyType}
                onChange={this.handleUpdateType}>
                {this.propertyTypes}
              </Dropdown>
            </FormField>
            <SaveCancel 
              onCancel={this.props.onCancel}
              onSave={this.handleSave}
              saveDisabled={saveDisabled}
              saveText="Save and Apply" />
          </ContentBox>
        </div>
      </div>
    );
  }
}

export default wrapModal(CustomProperty);
