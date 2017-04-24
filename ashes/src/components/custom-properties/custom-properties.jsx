// @flow

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { get, keys, flow, omit } from 'lodash';
import { makeLocalStore } from '@foxcomm/wings';
import { createAsyncActions } from '@foxcomm/wings';
import { bindActionCreators } from 'redux';

// components
import CustomPropertyModal from './custom-property-modal';
import ConfirmationDialog from '../modal/confirmation-dialog';

import { uploadImage } from '../../paragons/image'

// style
import s from './custom-properties.css';

type Props = {
  canAddProperty?: boolean,
  attributes: Attributes,
  onChange: (attributes: Attributes) => void,
  schema?: Object,
  children: Element<*>
};

type State = {
  isAddingProperty: boolean,
  isEditingProperty: boolean,
  isDeletingProperty: boolean,
  errors: {[id:string]: any},
  currentEdit: {
    name: string,
    type: string,
    value: any,
  },
  propertyToDelete: string,
}

export default class CustomProperties extends Component {
  props: Props;
  state: State = {
    isAddingProperty: false,
    isEditingProperty: false,
    isDeletingProperty: false,
    errors: {},
    currentEdit: {
      name: '',
      type: '',
      value: '',
    },
    propertyToDelete: '',
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

  get deletePropertyForm () {
    if (this.state.isDeletingProperty) {
      return (
        <ConfirmationDialog
        header="Delete Custom Property?"
        body="Are you sure you want to delete the custom property?"
        cancel="Cancel"
        confirm="Yes, Delete"
        isVisible={true}
        confirmAction={this.handleDeleteProperty}
        onCancel={() => this.setState({ isDeletingProperty: false }) }
        />
      );
    }
  }

  @autobind
  controlButtons(name: string, type: string, value: any) {
    const defaultProperties = keys(get(this.props.schema, 'properties', {}));
    if (defaultProperties.includes(name)) { return null; }

    return (
      <div className={s.controls}>
        <i className="icon-edit" onClick={() => this.onEdit(name, type, value)}/>
        <i className="icon-trash" onClick={() => this.onDelete(name)}/>
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
  handleEditProperty(property: { fieldLabel: string, propertyType: string, fieldValue: any }) {
    const { attributes } = this.props;
    const { currentEdit: { name } } = this.state;
    const { fieldLabel, propertyType, fieldValue } = property;

    const preparedObject = omit(attributes, name);
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
  handleDeleteProperty() {
    const newAttributes = omit(this.props.attributes, this.state.propertyToDelete);
    this.setState({ isDeletingProperty: false }, this.props.onChange(newAttributes));
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
    const reservedNames = keys(get(this.props, 'attributes', {}));
    const unique = !reservedNames.includes(fieldLabel);
    return unique;
  }

  @autobind
  processAttr(content: Element<*>, name: string, type: string, value: any) {
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
  onEdit(name: string, type: string, value: any) {
    this.setState({
      isEditingProperty: true,
      currentEdit: {
        name,
        type,
        value
      },
    });
  }

  @autobind
  onDelete(name: string) {
    this.setState({
      isDeletingProperty: true,
      propertyToDelete: name
    });
  }

  @autobind
  handleNewFiles(image: ImageFile, name: string): void {
    const { attributes } = this.props;
    const newAttributes = {
      ...attributes,
      [name]: {
        t: 'image',
        v: uploadImage(image),
      }
    };

    this.props.onChange(newAttributes)
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

  get children(): Element<*> {
    return React.cloneElement((this.props.children), {
      processAttr: this.processAttr,
      onDrop: this.handleNewFiles
    });
  }

  render() {
    return (
      <div>
        {this.children}
        {this.addCustomProperty}
        {this.customPropertyForm}
        {this.deletePropertyForm}
      </div>
    );
  }
}
