/**
 * @flow
 */

// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import CustomPropertyModal from './custom-property-modal';

// style
import s from './custom-properties.css';

export default class CustomProperties extends Component {
  props: Props;
  state: State = {
    isAddingProperty: false,
    isEditingProperty: false,
    errors: {},
    currentEdit: {
      name: '',
      type: '',
      value: '',
    },
  };

  get customPropertyForm() {
    if (this.state.isAddingProperty) {
      return (
        <CustomPropertyModal
          isVisible={true}
          onSave={this.handleCreateProperty}
          onCancel={() => this.setState({ isAddingProperty: false })}
        />
      );
    }

    if (this.state.isEditingProperty) {
      return (
        <CustomPropertyModal
          isVisible={true}
          currentEdit={this.state.currentEdit}
          onSave={this.handleEditProperty}
          onCancel={() => this.setState({ isEditingProperty: false })}
        />
      );
    }
  }

  @autobind
  controlButtons(name: string, type: string, value: any) {
    const defaultProperties = _.keys(_.get(this.props.schema, 'properties', {}));
    if (defaultProperties.includes(name)) { return null; }

    return (
      <div className={s.controls}>
        <i className="icon-edit" onClick={() => this.onEdit(name, type, value)}/>
        <i className="icon-trash" onClick={() => this.handleDeleteProperty(name)}/>
      </div>
    );
  }

  @autobind
  handleCreateProperty(property: { fieldLabel: string, propertyType: string }) {
    const { fieldLabel, propertyType } = property;
    const label = fieldLabel.toLowerCase();
    // TODO show error message, if fieldLabel is not unique
    if (!this.isUnique(label)) {
      return null;
    }

    const value = (() => {
      switch(propertyType) {
        case('date'): return new Date().toString();
        case('bool'): return false;
        default: return '';
      }
    })();
    this.setState({
      isAddingProperty: false
    }, () => this.handleChange(label, propertyType, value));
  }

  @autobind
  handleEditProperty(property: { fieldLabel: string, propertyType: string, fieldValue: string | number }) {
    const { attributes } = this.props;
    const { currentEdit: { name } } = this.state;
    const { fieldLabel, propertyType, fieldValue } = property;

    const preparedObject = _.omit(attributes, name);
    const newAttributes = {
      ...preparedObject,
      [fieldLabel]: {
        t: propertyType,
        v: fieldValue,
      }
    };

    this.setState({
      isEditingProperty: false,
      currentEdit: {
        name: '',
        type: '',
        value: ''
      }
    }, this.props.onChange(newAttributes));
  }

  @autobind
  handleDeleteProperty(name: string) {
    const newAttributes = _.omit(this.props.attributes, name);
    this.setState({ isAddingProperty: false }, this.props.onChange(newAttributes));
  }

  @autobind
  handleChange(name: string, type: string, value: any) {
    const { attributes } = this.props;
    const newAttributes = {
      ...attributes,
      [name]: {
        t: type,
        v: value,
      }
    };

    this.props.onChange(newAttributes);
  }

  @autobind
  isUnique(fieldLabel: string) {
    const reservedNames = _.keys(_.get(this.props, 'attributes', {}));
    const unique = !reservedNames.includes(fieldLabel);
    return unique;
  }

  @autobind
  processAttr(content, name, type, value) {
    return (
      <div key={name}>
        {this.controlButtons(name, type, value)}
        {content}
      </div>
    );
  }

  @autobind
  handleAddProperty() {
    this.setState({ isAddingProperty: true });
  }

  @autobind
  onEdit(name: string, type: string, value: string | number) {
    this.setState({
      isEditingProperty: true,
      currentEdit: {
        name,
        type,
        value
      },
    });
  }

  get addCustomProperty() {
    if (this.props.canAddProperty) {
      return (
        <div className="fc-object-form__add-custom-property">
          Custom Property
          <a id="fct-add-btn__custom-property" className="fc-object-form__add-custom-property-icon"
             onClick={this.handleAddProperty}>
            <i className="icon-add" />
          </a>
        </div>
      );
    }
  }

  get children() {
    return React.cloneElement((this.props.children), {
      ...this.props,
      processAttr: this.processAttr
    });
  }

  render() {
    return (
      <div>
        {this.children}
        {this.addCustomProperty}
        {this.customPropertyForm}
      </div>
    );
  }
}
