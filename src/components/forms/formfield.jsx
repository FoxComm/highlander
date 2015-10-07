'use strict';

import _ from 'lodash';
import React, { PropTypes } from 'react';

function isInputElement(element) {
  return _.contains(['input', 'textarea', 'select'], element.type);
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
    validator: PropTypes.func,
    children: PropTypes.node.isRequired
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
    const clonedChildren = React.Children.map(children, (child, idx) => {
      let newProps = {
        key: `form-field-${idx}`
      };

      if (isInputElement(child)) {
        if (!child.props.id) {
          newProps.id = _.uniqueId('form-field-');
        }
        this.inputId = child.props.id || newProps.id;

        newProps = {...newProps, ...overrideEventHandlers(child, {
          onBlur: (event) => {
            this.validate();
          },
          onChange: this.autoValidate
        })};
      }

      return React.cloneElement(child, newProps);
    }, this);

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
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.children) {
      this.updateChildren(nextProps.children);
    }
  }

  componentDidUpdate() {
    const inputNode = this.getInputNode();

    if (!inputNode) return;

    inputNode.setCustomValidity(this.state.errorMessage || '');
  }

  componentWillUnmount() {
    this.context.formDispatcher.removeListener('submit', this.onSubmit);
  }

  onSubmit(reportValidity) {
    reportValidity(this.validate());
  }

  validate() {
    let errorMessage = null;

    if (this.props.validator) {
      errorMessage = this.props.validator();
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
        <div className="fc-invalid-error">
          {this.state.errorMessage}
        </div>
      );
    }

    if (this.props.label) {
      const optionalMark = 'optional' in this.props ? <span className="fc-form-filed-optional">(optional)</span> : null;
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