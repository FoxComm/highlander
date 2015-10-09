'use strict';

import _ from 'lodash';
import React, { PropTypes } from 'react';
import * as validators from '../../lib/validators';

function isInputElement(element) {
  const isInputNode = _.contains(['input', 'textarea', 'select'], element.type);
  return isInputNode || element.type && _.contains(['InputElement'], element.type.displayName);
}

function overrideEventHandlers(child, newEventHandlers) {
  return _.transform(newEventHandlers, (result, handler, type) => {
    result[type] = (event) => {
      handler(event);
      if (child.props[type]) {
        return child.props[type](event);
      }
    };
  });
}

export default class FormField extends React.Component {

  static propTypes = {
    validator: PropTypes.oneOfType([
      PropTypes.func,
      PropTypes.string
    ]),
    children: PropTypes.node.isRequired,
    required: PropTypes.any,
    label: PropTypes.node
  };

  static contextTypes = {
    formDispatcher: PropTypes.object.isRequired
  };

  constructor(props, context) {
    super(props, context);

    this.onSubmit = _.bind(this.onSubmit, this);
    this.autoValidate = _.debounce(_.bind(this.autoValidate, this), 200);
  }

  updateChildren(children=this.props.children) {
    this.inputId = null;

    const clonedChildren = React.Children.map(children, (child, idx) => {
      let newProps = {
        key: `form-field-${idx}`
      };

      if (isInputElement(child)) {
        if (!child.props.id) {
          newProps.id = _.uniqueId('form-field-');
        }
        this.inputId = child.props.id || newProps.id;
        this.inputUnbound = true;

        newProps = {...newProps, ...overrideEventHandlers(child, {
          onBlur: (event) => {
            this.validate();
          },
          onChange: this.autoValidate
        })};
      }

      return React.cloneElement(child, newProps);
    }, this);

    if (!this.inputId) {
      console.error(
        `Warning: Couldn't find input element for ${this.props.label || '<unnamed>'} form field
         Hint: check isInputElement function`
      );
    }

    this.setState({
      children: clonedChildren
    });
  }

  autoValidate() {
    // validate only if field has error message
    // so we don't produce error if user start typing for example
    if (this.state.errorMessage) {
      this.validate();
    }
  }

  getInputNode() {
    return document.getElementById(this.inputId);
  }

  componentWillMount() {
    this.context.formDispatcher.on('submit', this.onSubmit);

    this.updateChildren();
    this.updateInputBind();
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.children && this.props.children !== nextProps.children) {
      this.updateChildren(nextProps.children);
    }
  }

  componentDidUpdate() {
    const inputNode = this.getInputNode();

    if (!inputNode) return;

    inputNode.setCustomValidity(this.state.errorMessage || '');
    this.updateInputState(false);
    this.updateInputBind();
  }

  updateInputState(checkNativeValidity) {
    const inputNode = this.getInputNode();

    let isError = !!this.state.errorMessage;

    if (checkNativeValidity && !isError) {
      isError = !inputNode.validity.valid;
    }

    inputNode.classList[isError ? 'add' : 'remove']('is-error');
  };

  updateInputBind() {
    const inputNode = this.getInputNode();

    if (inputNode) {
      inputNode.removeEventListener('invalid', this.updateInputState.bind(this, true));

      if (this.inputUnbound) {
        inputNode.addEventListener('invalid', this.updateInputState.bind(this, true));
        this.inputUnbound = false;
      }
    }
  }

  componentWillUnmount() {
    this.context.formDispatcher.removeListener('submit', this.onSubmit);
    this.updateInputBind();
  }

  onSubmit(reportValidity) {
    reportValidity(this.validate());
  }

  getInputValue() {
    const node = this.getInputNode();

    if (node.type == 'checkbox') {
      return node.checked;
    } else {
      return node.value;
    }
  }

  validate() {
    let errorMessage = null;

    let validator = this.props.validator;

    if (validator) {
      if (_.isString(validator)) {
        validator = validators[validator];
      }

      let value = this.getInputValue();
      if (!_.isString(value) || value) {
        errorMessage = validator(value);
      } else if ('required' in this.props) {
        errorMessage = '$label is required field';
      }

      if (errorMessage) {
        errorMessage = errorMessage.replace('$label', this.props.label);
      }
    }

    this.setState({
      errorMessage
    });

    return errorMessage === null;
  }

  render() {
    let label = null;
    let errors = null;

    if (this.state.errorMessage) {
      errors = (
        <div className="fc-form-field-error">
          {this.state.errorMessage}
        </div>
      );
    }

    if (this.props.label) {
      const optionalMark = 'optional' in this.props ? <span className="fc-form-field-optional">(optional)</span> : null;
      label = (
        <label htmlFor={this.inputId}>
          {this.props.label}
          {optionalMark}
        </label>
      );
    }

    return (
      <div className="fc-form-field">
        {label}
        {this.state.children}
        {errors}
      </div>
    );
  }
}