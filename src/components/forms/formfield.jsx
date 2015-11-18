import _ from 'lodash';
import React, { PropTypes } from 'react';
import * as validators from '../../lib/validators';
import classnames from 'classnames';

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
    maxLength: PropTypes.number,
    label: PropTypes.node,
    labelClassName: PropTypes.string
  };

  static contextTypes = {
    formDispatcher: PropTypes.object
  };

  constructor(props, context) {
    super(props, context);

    this.onSubmit = _.bind(this.onSubmit, this);
    this.autoValidate = _.debounce(_.bind(this.autoValidate, this), 200);
  }

  isInputElement(element) {
    const isInputNode = _.contains(['input', 'textarea', 'select'], element.type);
    return 'formFieldTarget' in element.props || isInputNode;
  }

  cloneChildren(children, level=0) {
    return React.Children.map(children, (child, idx) => {
      if (!React.isValidElement(child)) return child;

      let newProps = {
        key: `form-field-${level}-${idx}`
      };
      let newChildren = null;

      if (child.props.children) {
        newChildren = this.cloneChildren(child.props.children, level + 1);
      }

      if (this.isInputElement(child)) {
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

      return React.cloneElement(child, newProps, newChildren);
    }, this);
  }


  updateChildren(children=this.props.children) {
    this.inputId = null;

    const clonedChildren = this.cloneChildren(children);

    if (!this.inputId) {
      console.error(
        `Warning: Couldn't find input element for ${this.props.label || '<unnamed>'} form field.
        Hint: if you using custom input element add 'formFieldTarget' attribute for it.`
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
    if (this.context.formDispatcher) {
      this.context.formDispatcher.on('submit', this.onSubmit);
    }

    this.updateChildren();
  }

  componentDidMount() {
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

    if (inputNode.setCustomValidity) {
      inputNode.setCustomValidity(this.state.errorMessage || '');
    }
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
    if (this.context.formDispatcher) {
      this.context.formDispatcher.removeListener('submit', this.onSubmit);
    }
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
    let errors = [];

    let validator = this.props.validator;
    const label = this.props.label;

    if (_.isString(validator)) {
      validator = validators[validator];
    }

    let value = this.getInputValue();

    if (!_.isString(value) || value) {
      if (this.props.maxLength && _.isString(value) && value.length > this.props.maxLength) {
        errors = [...errors, `${label} can not be more than ${this.props.maxLength} characters`];
      }

      if (validator) {
        const validatorError = validator(value, label);
        if (validatorError) {
          errors = [...errors, validatorError];
        }
      }
    } else if ('required' in this.props) {
      errors = [...errors, `${label} is required field`];
    }

    this.setState({
      errors
    });

    return errors.length === 0;
  }

  get errorMessages() {
    if (this.state.errors) {
      return (
        <div>
          {this.state.errors.map((error, index) => {
            return (
              <div key={`error-${index}`} className="fc-form-field-error">
                {error}
              </div>
            );
          })}
        </div>
      );
    }
  }

  get label() {
    if (this.props.label) {
      const optionalMark = 'optional' in this.props ? <span className="fc-form-field-optional">(optional)</span> : null;
      const className = classnames('fc-form-field-label', this.props.labelClassName);
      return (
        <label className={className} htmlFor={this.inputId}>
          {this.props.label}
          {optionalMark}
        </label>
      );
    }
  }

  render() {
    return (
      <div className="fc-form-field">
        {this.label}
        {this.state.children}
        {this.errorMessages}
      </div>
    );
  }
}
