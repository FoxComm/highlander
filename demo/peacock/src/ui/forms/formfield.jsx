import _ from 'lodash';
import { autobind, debounce } from 'core-decorators';
import React, { PropTypes, Component, Element } from 'react';
import { findDOMNode } from 'react-dom';
import * as validators from './validators';
import styles from './formfield.css';
import formatString from 'lib/string-format';
import classNames from 'classnames';

import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

type FormFieldProps = Localized & {
  validator: ?string|?(value: any) => string,
  validateOnBlur?: boolean;
  children: Element<*>,
  required: ?any,
  maxLength: ?number,
  target: ?string,
  name: ?string,
  error: ?string|boolean,
  getTargetValue: ?(node: any) => any,
  className: ?string,
  label: ?string,
};

class FormField extends Component {
  props: FormFieldProps;
  beforeValue: any;
  _willUnmount: boolean = false;

  static contextTypes = {
    formDispatcher: PropTypes.object,
  };

  static defaultProps = {
    target: 'input,textarea,select',
    getTargetValue: (node) => { return node.type == 'checkbox' ? node.checked : node.value; },
    validateOnBlur: false,
  };

  static childContextTypes = {
    error: PropTypes.string,
  };

  state = {
    fieldErrors: [],
    submitError: null,
    touched: false,
    isValid: true,
    submitted: false,
  };

  getChildContext() {
    return {
      error: this.errors[0],
    };
  }

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
    return findDOMNode(this).querySelector(this.props.target);
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

  get hasError() {
    return this.errors.length !== 0 || !this.state.isValid || this.props.error || this.state.submitError;
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
    if (this.props.validateOnBlur) {
      this.fullValidate(target);
    }

    this.setState({
      touched: true,
    });
  }

  @autobind
  validate() {
    let errors = [];

    let validator = this.props.validator;

    if (_.isString(validator)) {
      validator = validators[validator];
    }

    const { t } = this.props;

    const value = this.getTargetValue();
    const label = this.props.label || t('This field');

    if (value !== void 0 && (!_.isString(value) || value)) {
      if (this.props.maxLength && _.isString(value) && value.length > this.props.maxLength) {
        errors = [...errors, formatString(t('Can not be more than %0 characters'), this.props.maxLength)];
      }

      if (validator) {
        const validatorError = validator.call({ t }, value);
        if (validatorError) {
          errors = [...errors, formatString(validatorError, label)];
        }
      }
    } else if ('required' in this.props) {
      errors = [...errors, formatString(t('%0 is required'), label)];
    }

    this.setState({
      fieldErrors: errors,
    });

    return errors.length === 0;
  }

  @autobind
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
    // validate only if we have error for this field
    // so we don't produce error if user start typing for example
    if (!this._willUnmount && this.hasError) {
      this.fullValidate(target);
    }
  }

  render() {
    const className = classNames(this.props.className, {
      [styles['has-error']]: this.hasError && this.readyToShowErrors,
    });

    return (
      <div className={className} >
        {this.props.children}
      </div>
    );
  }
}

export default localized(FormField);
