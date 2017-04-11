/**
 * @flow
 */


// libs
import React, { Component } from 'react';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import { stripTags } from 'lib/text-utils';
import { isDefined } from 'lib/utils';

// components
import { FormField, FormFieldError } from '../forms';
import { SliderCheckbox } from '../checkbox/checkbox';
import CurrencyInput from '../forms/currency-input';
import CustomProperty from '../products/custom-property';
import DatePicker from '../datepicker/datepicker';
import RichTextEditor from '../rich-text-editor/rich-text-editor';
import { Dropdown } from '../dropdown';
import SwatchInput from '../forms/swatch-input';

import type { AttrSchema } from 'paragons/object';

// style
import s from './object-form-inner.css';

type Props = {
  canAddProperty?: boolean,
  fieldsToRender?: Array<string>,
  fieldsOptions?: Object,
  attributes: Attributes,
  onChange: (attributes: Attributes) => void,
  schema?: Object,
  className?: string,
};

type State = {
  isAddingProperty: boolean,
  isEditingProperty: boolean,
  errors: {[id:string]: any},
  name: string,
  type: string,
  value: string | number
};

type AttrOptions = {
  required: boolean,
  label: string,
  isDefined: (value: any) => boolean,
  disabled?: boolean,
};

const inputClass = 'fc-object-form__field-value';

function formatLabel(label: string): string {
  return _.snakeCase(label).split('_').reduce((res, val) => {
    return `${res} ${_.capitalize(val)}`;
  });
}

// TODO: fix content type
export function renderFormField(name: string, content: any, options: AttrOptions) {
  return (
    <FormField
      {...options}
      className="fc-object-form__field"
      labelClassName="fc-object-form__field-label"
      key={`object-form-attribute-${name}`}
    >
      {content}
    </FormField>
  );
}

function guessType(value: any): string {
  const typeOf = typeof value;
  switch (typeOf) {
    case 'string':
    case 'number':
    case 'boolean':
      return typeOf;
    default:
      return 'string';
  }
}

export default class ObjectFormInner extends Component {
  props: Props;
  state: State = {
    isAddingProperty: false,
    isEditingProperty: false,
    errors: {},
    name: '',
    type: '',
    value: '',
  };

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

  get customPropertyForm() {
    if (this.state.isAddingProperty) {
      return (
        <CustomProperty
          isVisible={true}
          onSave={this.handleCreateProperty}
          onCancel={() => this.setState({ isAddingProperty: false })}
        />
      );
    }

    if (this.state.isEditingProperty) {
      const property = {
        name: this.state.name,
        type: this.state.type,
        value: this.state.value
      };

      return (
        <CustomProperty
          property={property}
          isVisible={true}
          onSave={this.handleEditProperty}
          onCancel={() => this.setState({ isEditingProperty: false })}
        />
      );
    }
  }

  @autobind
  handleAddProperty() {
    this.setState({ isAddingProperty: true });
  }

  @autobind
  handleCreateProperty(property: { fieldLabel: string, propertyType: string }) {
    const { fieldLabel, propertyType } = property;
    const value = (() => {
      switch(propertyType) {
        case('date'): return new Date().toString();
        case('bool'): return false;
        default: return '';
      }
    })();
    this.setState({
      isAddingProperty: false
    }, () => this.handleChange(fieldLabel, propertyType, value));
  }

  @autobind
  handleEditProperty(property: { fieldLabel: string, propertyType: string, fieldValue: string | number }) {
    const { attributes } = this.props;
    const { name } = this.state;
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
      name: '',
      type: '',
      value: ''
    }, this.props.onChange(newAttributes));
  }

  @autobind
  handleDeleteProperty(name: string) {
    const newAttributes = _.omit(this.props.attributes, name);
    this.setState({ isAddingProperty: false }, this.props.onChange(newAttributes));
  }

  @autobind
  onEdit(name: string, type: string, value: string | number) {
    this.setState({
      isEditingProperty: true,
      name,
      type,
      value
    });
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

    if (['options', 'richText'].indexOf(type) >= 0) {
      const attrOptions = this.getAttrOptions(name);
      if (attrOptions.required) {
        const error = attrOptions.isDefined(value) ? null : `${attrOptions.label} is a required field`;
        const { errors } = this.state;
        errors[name] = error;
        this.setState({ errors });
      }
    }
    this.props.onChange(newAttributes);
  }

  renderBoolean(name: string, value: boolean, options: AttrOptions) {
    const onChange = () => this.handleChange(name, 'bool', !value);
    const sliderCheckbox = (
      <div>
        {this.controlButtons(name, 'bool', value)}
        <SliderCheckbox
          id={name}
          checked={value}
          onChange={onChange}
        />
      </div>
    );

    return renderFormField(name, sliderCheckbox, options);
  }

  renderBool(...args: Array<any>) {
    return this.renderBoolean(...args);
  }

  renderElement(name: string, value: any, options: AttrOptions) {
    return renderFormField(name, value, options);
  }

  renderDate(name: string, value: string, options: AttrOptions) {
    const dateValue = new Date(value);
    const onChange = (v: Date) => this.handleChange(name, 'date', v.toISOString());
    const dateInput = (
      <div>
        {this.controlButtons(name, 'string', value)}
        <DatePicker date={dateValue} onChange={onChange} />
      </div>
    );
    return renderFormField(name, dateInput, options);
  }

  renderPrice(name: string, value: any, options: AttrOptions) {
    const priceValue: string = _.get(value, 'value', '');
    const priceCurrency: string = _.get(value, 'currency', 'USD');
    const onChange = value => this.handleChange(name, 'price', {
      currency: priceCurrency,
      value: Number(value)
    });
    const currencyInput = (
      <div>
        {this.controlButtons(name, 'price', value)}
        <CurrencyInput
          inputClass={inputClass}
          inputName={name}
          value={priceValue}
          onChange={onChange}
        />
      </div>
    );

    return renderFormField(name, currencyInput, options);
  }

  renderRichText(name: string, value: any, options: AttrOptions) {
    const onChange = v => this.handleChange(name, 'richText', v);
    const error = _.get(this.state, ['errors', name]);
    const classForContainer = classNames('fc-object-form__field', {
      '_has-error': error != null,
    });
    const nameVal = _.kebabCase(name);

    return (
      <div className={classForContainer}>
        {this.controlButtons(name, 'richText', value)}
        <RichTextEditor
          className={`fc-rich-text__name-${nameVal}`}
          label={options.label}
          value={value}
          onChange={onChange}
        />
        {error && <FormFieldError error={error} />}
      </div>
    );
  }

  renderString(name: string, value: string = '', options: AttrOptions) {
    const onChange = ({target}) => {
      return this.handleChange(name, 'string', target.value);
    };
    const stringInput = (
      <div>
        {this.controlButtons(name, 'string', value)}
        <input
          className={inputClass}
          type="text"
          name={name}
          value={value || ''}
          onChange={onChange}
          disabled={options.disabled}
        />
      </div>
    );

    return renderFormField(name, stringInput, options);
  }

  renderNumber(name: string, value: ?number = null, options: AttrOptions) {
    const onChange = ({target}) => {
      return this.handleChange(name, 'number', target.value == '' ? null : Number(target.value));
    };
    const stringInput = (
      <input
        className={inputClass}
        type="number"
        name={name}
        value={value == null ? '' : value}
        onChange={onChange}
      />
    );

    return renderFormField(name, stringInput, options);
  }

  renderOptions(name: string, value: any, options: AttrOptions) {
    const fieldOptions = this.props.fieldsOptions && this.props.fieldsOptions[name];
    if (!fieldOptions) throw new Error('You must define fieldOptions for options fields');

    const onChange = v => this.handleChange(name, 'options', v);
    const error = _.get(this.state, ['errors', name]);

    return (
      <div className="fc-object-form_field">
        <div className="fc-object-form__field-label">{options.label}</div>
        <Dropdown
          value={value}
          items={fieldOptions}
          onChange={onChange}
        />
        {error && <FormFieldError error={error} />}
      </div>
    );
  }

  renderText(name: string, value: string = '', options: AttrOptions) {
    const onChange = ({target}) => {
      return this.handleChange(name, 'text', target.value);
    };
    const textInput = (
      <textarea
        className={inputClass}
        name={name}
        onChange={onChange}
        value={value}
      />
    );

    return renderFormField(name, textInput, options);
  }

  renderColor(name: string, value: any, options: AttrOptions) {
    const onChange = v => this.handleChange(name, 'color', v);
    const colorSwatch = (
      <div>
        {this.controlButtons(name, 'color', value)}
        <SwatchInput
          value={value}
          onChange={onChange}
        />
      </div>
    );

    return renderFormField(name, colorSwatch, options);
  }

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

  shouldComponentUpdate(nextProps: Props, nextState: State): boolean {
    const attributesChanged = !_.eq(this.props.attributes, nextProps.attributes);
    const stateChanged = !_.eq(this.state, nextState);

    return attributesChanged || stateChanged;
  }

  isRequired(name: string): boolean {
    const { schema } = this.props;
    return schema ? _.includes(schema.required, name) : false;
  }

  guessRenderName(schema: ?AttrSchema, attribute: ?Attribute): string {
    let name = null;

    if (attribute) {
      name = attribute.t;
    }
    if (name == null && schema) {
      name = schema.widget || schema.type;
    }
    if (name == 'integer') {
      name = 'number';
    }

    let renderName = `render${_.upperFirst(name)}`;
    if (!(renderName in this)) {
      renderName = 'renderString';
    }
    return renderName;
  }

  getAttrOptions(name: string,
                 // $FlowFixMe: there is no global context
                 schema: ?AttrSchema = this.props.schema && this.props.schema.properties[name]): Object {
    const options = {
      required: this.isRequired(name),
      label: schema && schema.title || formatLabel(name),
      isDefined: isDefined,
      disabled: schema && schema.disabled,
    };
    if (schema && schema.widget == 'richText') {
      options.isDefined = value => isDefined(stripTags(value));
    }

    return options;
  }

  render() {
    const { props } = this;
    const { attributes, schema, className } = props;
    const fieldsToRender = _.isEmpty(props.fieldsToRender) ? Object.keys(attributes) : props.fieldsToRender;

    const renderedAttributes: Array<Element<*>> = _.map(fieldsToRender, name => {
      const attribute: Attribute = attributes[name];
      const attrSchema: ?AttrSchema = schema ? schema.properties[name] : null;

      const renderName = this.guessRenderName(attrSchema, attribute);
      const attrOptions = this.getAttrOptions(name, attrSchema);
      // $FlowFixMe: guessRenderName is enough
      return React.cloneElement(this[renderName](name, attribute && attribute.v, attrOptions), { key: name });
    });

    return (
      <div className={classNames('fc-object-form', className)}>
        {renderedAttributes}
        {this.addCustomProperty}
        {this.customPropertyForm}
      </div>
    );
  }
}
