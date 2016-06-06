/**
 * @flow
 */

import React, { Component, Element, PropTypes } from 'react';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import classNames from 'classnames';

import { illuminateAttributes, setAttribute } from '../../paragons/form-shadow-object';

import { FormField } from '../forms';
import { SliderCheckbox } from '../checkbox/checkbox';
import CurrencyInput from '../forms/currency-input';
import CustomProperty from '../products/custom-property';
import DatePicker from '../datepicker/datepicker';
import RichTextEditor from '../rich-text-editor/rich-text-editor';
import { Dropdown } from '../dropdown';

type Props = {
  canAddProperty?: boolean,
  fieldsToRender?: Array<string>,
  fieldsOptions?: Object,
  form: FormAttributes,
  shadow: ShadowAttributes,
  onChange: (form: FormAttributes, shadow: ShadowAttributes) => void,
  options: Object,
};

type State = {
  isAddingProperty: bool,
  errors: {[id:string]: number},
};

const inputClass = 'fc-object-form__field-value';

function formatLabel(label: string): string {
  return _.snakeCase(label).split('_').reduce((res, val) => {
    return `${res} ${_.capitalize(val)}`;
  });
}

function renderFormField(label: string, input: Element, args?: any): Element {
  const formattedLabel = formatLabel(label);
  const isRequired = _.get(args, 'required', false) === true ? { required: true } : null;
  const maybeValidator = _.get(args, 'validator');
  const validator = maybeValidator != null ? { validator: maybeValidator } : null;
  return (
    <FormField
      className="fc-object-form__field"
      labelClassName="fc-object-form__field-label"
      label={formattedLabel}
      key={`object-form-attribute-${label}`}
      {...validator}
      {...isRequired} >
      {input}
    </FormField>
  );
}

export default class ObjectFormInner extends Component {
  props: Props;
  state: State = { isAddingProperty: false, errors: {} };

  static defaultProps = {
    options: {},
  };

  get addCustomProperty(): ?Element {
    if (this.props.canAddProperty) {
      return (
        <div className="fc-object-form__add-custom-property">
          Custom Property
          <a className="fc-object-form__add-custom-property-icon"
             onClick={this.handleAddProperty}>
            <i className="icon-add" />
          </a>
        </div>
      );
    }
  }

  get customPropertyForm(): ?Element {
    if (this.state.isAddingProperty) {
      return (
        <CustomProperty
          isVisible={true}
          onSave={this.handleCreateProperty}
          onCancel={() => this.setState({ isAddingProperty: false })}
        />
      );
    }
  }

  get renderFunctions(): Object {
    return {
      bool: this.renderBool,
      date: this.renderDate,
      price: this.renderPrice,
      richText: this.renderRichText,
      string: this.renderString,
      options: this.renderOptions,
      text: this.renderText,
    };
  }

  @autobind
  handleAddProperty() {
    this.setState({ isAddingProperty: true });
  }

  @autobind
  handleCreateProperty(property: { fieldLabel: string, propertyType: string }) {
    const { fieldLabel, propertyType } = property;
    const val = propertyType == 'date' ? new Date().toString() : '';
    this.setState({
      isAddingProperty: false
    }, () => this.handleChange(fieldLabel, propertyType, val));
  }

  @autobind
  handleChange(label: string, type: string, value: string) {
    const { form, shadow, options } = this.props;
    const [newForm, newShadow] = setAttribute(label, type, value, form, shadow);
    if (['options', 'richText'].indexOf(type) >= 0) {
      const validator = _.get(options, [label, 'validator'], _.noop);
      const error = validator(value);
      const { errors } = this.state;
      errors[label] = error;
      this.setState({ errors });
    }
    this.props.onChange(newForm, newShadow);
  }

  @autobind
  renderBool(label: string, value: bool, args?: any): Element {
    const formattedLabel = formatLabel(label);
    const onChange = v => this.handleChange(label, 'bool', v);
    return (
      <div className="fc-object-form__field">
        <div className="fc-object-form__field-label">{formattedLabel}</div>
        <SliderCheckbox
          id={label}
          checked={value}
          onChange={onChange}
        />
      </div>
    );
  }

  @autobind
  renderDate(label: string, value: string, args?: any): Element {
    const dateValue = new Date(value);
    const onChange = (v: Date) => this.handleChange(label, 'date', v.toISOString());
    const dateInput = <DatePicker date={dateValue} onChange={onChange} />;
    return renderFormField(label, dateInput, args);
  }

  @autobind
  renderPrice(label: string, value: any, args?: any): Element {
    const priceValue: string = _.get(value, 'value', '');
    const onChange = v => this.handleChange(label, 'price', v);
    const currencyInput = (
      <CurrencyInput
        inputClass={inputClass}
        inputName={label}
        value={priceValue}
        onChange={onChange}
      />
    );

    return renderFormField(label, currencyInput, args);
  }

  @autobind
  renderRichText(label: string, value: any, args?: any): Element {
    const formattedLabel = formatLabel(label);
    const onChange = v => this.handleChange(label, 'richText', v);
    const error = _.get(this.state, ['errors', label]);
    const classForContainer = classNames('fc-object-form__field', {
      '_with-error': error != null,
    });
    const errorMessage = error && (
      <div className="fc-form-field-error">
        {error}
      </div>
    );
    return (
      <div className={classForContainer}>
        <RichTextEditor
          label={formattedLabel}
          value={value}
          onChange={onChange}
        />
        {errorMessage}
      </div>
    );
  }

  @autobind
  renderString(label: string, value: string = '', args?: any): Element {
    const onChange = ({target}) => {
      return this.handleChange(label, 'richText', target.value);
    };
    const stringInput = (
      <input
        className={inputClass}
        type="text"
        name={label}
        value={value}
        onChange={onChange}
      />
    );

    return renderFormField(label, stringInput, args);
  }

  @autobind
  renderOptions(label: string, value: any, args?: any): Element {
    const options = this.props.fieldsOptions && this.props.fieldsOptions[label];
    if (!options) throw new Error('You must define fieldOptions for options fields');

    const formattedLabel = formatLabel(label);
    const onChange = v => this.handleChange(label, 'options', v);
    const error = _.get(this.state, ['errors', label]);
    const errorMessage = error && (
      <div className="fc-form-field-error">
        {error}
      </div>
    );

    return (
      <div className="fc-object-form_field">
        <div className="fc-object-form__field-label">{formattedLabel}</div>
        <Dropdown
          value={value}
          items={options}
          onChange={onChange}
        />
        {errorMessage}
      </div>
    );
  }

  @autobind
  renderText(label: string, value: string = '', args?: any): Element {
    const onChange = ({target}) => {
      return this.handleChange(label, 'richText', target.value);
    };
    const textInput = (
      <textarea
        className={inputClass}
        name={label}
        onChange={onChange} value={value}
      />
    );

    return renderFormField(label, textInput, args);
  }

  render(): Element {
    const { form, shadow, options } = this.props;
    const attributes = illuminateAttributes(form, shadow);
    const fieldsToRender = _.isEmpty(this.props.fieldsToRender) ? Object.keys(attributes) : this.props.fieldsToRender;

    const renderedAttributes: Array<Element> = _.map(fieldsToRender, name => {
      const attribute = attributes[name];
      const optionalArgs = options[name];
      if (attribute) {
        const { label, type, value } = attribute;
        const renderFn = _.get(this.renderFunctions, type, this.renderString);
        return React.cloneElement(renderFn(label, value, optionalArgs), {key: name});
      } else {
        console.warn(`You tried to render ${name} attribute, but there is no such attribute in a model`);
      }
    });

    return (
      <div className="fc-object-form">
        {renderedAttributes}
        {this.addCustomProperty}
        {this.customPropertyForm}
      </div>
    );
  }
}
