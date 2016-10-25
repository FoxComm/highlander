// @flow weak

import _ from 'lodash';
import { autobind, debounce } from 'core-decorators';
import React, { Component, Element, PropTypes } from 'react';
import { findDOMNode } from 'react-dom';
import * as validators from 'lib/validators';
import classNames from 'classnames';
import { isDefined } from 'lib/utils';

import AutoScroll from '../common/auto-scroll';

type FormFieldErrorProps = {
  error: Element|string,
  autoScroll?: boolean,
}

export function FormFieldError(props: FormFieldErrorProps) {
  return (
    <div className="fc-form-field-error">
      {props.error}
      {props.autoScroll && <AutoScroll />}
    </div>
  );
}

type FormFieldProps = {
  validator?: string|(value: any) => string;
  children: Element;
  required: ?any;
  maxLength: ?number;
  target: ?string;
  name: ?string;
  error: ?string|boolean;
  getTargetValue: (node: any) => any;
  className: ?string;
  labelClassName?: string;
  labelAtRight?: Element|string;
  labelAfterInput?: boolean;
  label?: Element|string;
  validationLabel?: string;
  requiredMessage?: string;
  isDefined: (value: any) => boolean;
  scrollToErrors?: boolean,
};

export default class FormField extends Component {
  props: FormFieldProps;
  beforeValue: any;
  _willUnmount: boolean = false;

  static contextTypes = {
    formDispatcher: PropTypes.object,
  };

  static defaultProps = {
    target: 'input,textarea,select',
    getTargetValue: node => node.type == 'checkbox' ? node.checked : node.value,
    isDefined: isDefined,
  };

  state = {
    fieldErrors: [],
    submitError: null,
    touched: false,
    isValid: true,
    submitted: false,
  };

  toggleBindToDispatcher(bind) {
    const { formDispatcher } = this.context;
    if (formDispatcher) {
      const toggleBind = bind ? formDispatcher.on : formDispatcher.removeListener;

      toggleBind.call(formDispatcher, 'submit', this.handleSubmit);
      toggleBind.call(formDispatcher, 'errors', this.handleErrors);
    }
  }

  componentWillMount() {
    this.toggleBindToDispatcher(true);
  }

  componentDidMount() {
    this.toggleBindToTarget(true);
  }

  componentWillUpdate() {
    this.beforeValue = this.getTargetValue();
    this.toggleBindToTarget(false);
  }

  // use this only for sync errors
  // browser displays this message each time that user changes input
  setCustomValidity(message) {
    const targetNode = this.findTargetNode();
    if (targetNode && targetNode.setCustomValidity) {
      targetNode.setCustomValidity(message);
    }
  }

  componentDidUpdate() {
    const targetNode = this.findTargetNode();

    if (!targetNode) return;

    this.toggleBindToTarget(true);
    if (this.getTargetValue() !== this.beforeValue && this.hasError) {
      // target value was changed
      setTimeout(this.fullValidate, 0);
    }
  }

  componentWillUnmount() {
    this.toggleBindToDispatcher(false);
    this.toggleBindToTarget(false);

    this._willUnmount = true;
  }

  @autobind
  handleSubmit(reportValidity) {
    this.setState({
      submitted: true,
    });
    reportValidity(this.validate());
  }

  @autobind
  handleErrors(errors = {}) {
    const { name } = this.props;

    if (name) {
      this.setState({
        submitError: errors[name],
      });
    }
  }

  getTargetValue() {
    return this.props.getTargetValue(this.findTargetNode());
  }

  findTargetNode() {
    if (this.props.target) {
      return findDOMNode(this).querySelector(this.props.target);
    }
  }

  get errors() {
    let errors = this.state.fieldErrors;

    if (this.props.error && this.props.error !== true) {
      errors = [this.props.error, ...errors];
    }

    if (this.state.submitError && this.state.submitError !== true) {
      errors = [this.state.submitError, ...errors];
    }

    if (this.state.validationMessage) {
      errors = [this.state.validationMessage, ...errors];
    }

    return errors;
  }

  get hasError(): boolean {
    return this.errors.length !== 0 || !this.state.isValid || !!this.props.error || !!this.state.submitError;
  }

  get readyToShowErrors() {
    return this.state.touched || this.state.submitted || (!this.state.touched && !this.state.isValid);
  }

  toggleBindToTarget(bind) {
    const targetNode = this.findTargetNode();
    if (!targetNode) return;

    const toggleBind = bind ? targetNode.addEventListener : targetNode.removeEventListener;

    toggleBind.call(targetNode, 'blur', this.handleBlur);
    toggleBind.call(targetNode, 'change', this.handleChange);
    toggleBind.call(targetNode, 'input', this.handleChange);
    toggleBind.call(targetNode, 'invalid', this.handleInvalid);
  }

  @autobind
  handleInvalid({target}) {
    this.setState({
      isValid: target.validity.valid,
      validationMessage: target.validationMessage,
    });
  }

  @autobind
  handleBlur({target}) {
    this.fullValidate(target);

    this.setState({
      touched: true,
    });
  }

  @autobind
  validate(): boolean {
    let errors = [];

    const validator: ?Function =
      typeof this.props.validator == 'string'
        ? validators[this.props.validator]
        : this.props.validator;

    const value = this.getTargetValue();
    let label = this.props.validationLabel || this.props.label;
    let requiredMessage = this.props.requiredMessage;
    if (!label) {
      label = 'This field';
      if (!requiredMessage) requiredMessage = `${label} is required`;
    } else {
      if (!requiredMessage) requiredMessage = `${label} is a required field`;
    }

    if (this.props.isDefined(value)) {
      if (this.props.maxLength && _.isString(value) && value.length > this.props.maxLength) {
        errors = [...errors, `${label} can not be more than ${this.props.maxLength} characters`];
      }

      if (validator) {
        const validatorError = validator(value, label);
        if (validatorError) {
          errors = [...errors, validatorError];
        }
      }
    } else if (this.props.required) {
      errors = [...errors, requiredMessage];
    }

    this.setState({
      fieldErrors: errors,
    });

    return errors.length === 0;
  }

  @autobind
  // $FlowFixMe: there is no global context
  fullValidate(target = this.findTargetNode()) {
    this.validate();

    const isValid = target.checkValidity ? target.checkValidity() : true;
    this.setState({
      isValid,
      validationMessage: target.validationMessage,
    });
  }

  @autobind
  @debounce(200)
  handleChange({target}) {
    // validate only if field had touched once (or we have error for this field)
    // so we don't produce error if user start typing for example
    if (!this._willUnmount && (this.state.touched || this.hasError)) {
      this.fullValidate(target);
    }
  }

  get errorMessages() {
    if (this.errors.length && this.readyToShowErrors) {
      return (
        <div>
          {this.errors.map((error, index) => {
            return (
              <FormFieldError
                key={`error-${index}`}
                error={error}
                autoScroll={this.props.scrollToErrors}
              />
            );
          })}
        </div>
      );
    }
  }

  get label(): ?Element {
    if (this.props.label) {
      const optionalMark = 'optional' in this.props ? <span className="fc-form-field-optional">(optional)</span> : null;
      const className = classNames('fc-form-field-label', this.props.labelClassName);
      return (
        <label className={className} htmlFor={this.state.targetId} key="label">
          {this.props.label}
          {optionalMark}
          <div className="fc-right">
            {this.props.labelAtRight}
          </div>
        </label>
      );
    }
  }

  render() {
    const className = classNames(
      'fc-form-field',
      this.props.className,
      {'_form-field-error': this.hasError},
      {'_form-field-required': this.props.required}
    );
    const children = React.cloneElement(this.props.children, {
      key: 'children',
    });

    const content = this.props.labelAfterInput
      ? [children, this.label]
      : [this.label, children];

    return (
      <div className={className}>
        {content}
        {this.errorMessages}
      </div>
    );
  }
}
