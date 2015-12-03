import _ from 'lodash';
import { autobind, debounce } from 'core-decorators';
import React, { PropTypes } from 'react';
import { findDOMNode } from 'react-dom';
import * as validators from '../../lib/validators';
import classNames from 'classnames';
import { mergeEventHandlers } from '../../lib/react-utils';

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
    labelClassName: PropTypes.string,
    labelAtRight: PropTypes.node,
    target: PropTypes.string,
    getTargetValue: PropTypes.func,
    className: PropTypes.string,
  };

  static contextTypes = {
    formDispatcher: PropTypes.object
  };

  static defaultProps = {
    target: 'input,textarea,select',
    getTargetValue: node => node.type == 'checkbox' ? node.checked : node.value,
  };

  constructor(...args) {
    super(...args);

    this.state = {
      targetId: ''
    };
  }

  findTargetNode() {
    return findDOMNode(this).querySelector(this.props.target);
  }

  toggleBindToTarget(bind) {
    const targetNode = this.findTargetNode();
    if (!targetNode) return;

    const toggleBind = bind ? targetNode.addEventListener : targetNode.removeEventListener;

    toggleBind.call(targetNode, 'blur', this.validate);
    toggleBind.call(targetNode, 'change', this.autoValidate);
    toggleBind.call(targetNode, 'invalid', () => this.updateInputState(true));

    let id = targetNode.getAttribute('id');

    if (bind) {
      if (!id) {
        id = _.uniqueId('form-field-');
        targetNode.setAttribute('id', id);
      }

      if (this.state.targetId != id) {
        this.setState({
          targetId: id
        });
      }
    }
  }

  @autobind
  @debounce(200)
  autoValidate() {
    // validate only if field has error message
    // so we don't produce error if user start typing for example
    if (this.state.errorMessage) {
      this.validate();
    }
  }

  componentWillMount() {
    if (this.context.formDispatcher) {
      this.context.formDispatcher.on('submit', this.onSubmit);
    }
  }

  componentDidMount() {
    this.toggleBindToTarget(true);
  }

  componentWillUpdate() {
    this.toggleBindToTarget(false);
  }

  componentDidUpdate() {
    const targetNode = this.findTargetNode();

    if (!targetNode) return;

    if (targetNode.setCustomValidity) {
      targetNode.setCustomValidity(this.state.errorMessage || '');
    }
    this.updateInputState(false);
    this.toggleBindToTarget(true);
  }

  updateInputState(checkNativeValidity) {
    const inputNode = this.findTargetNode();

    let isError = !!this.state.errorMessage;

    if (checkNativeValidity && !isError) {
      isError = !inputNode.validity.valid;
    }

    inputNode.classList[isError ? 'add' : 'remove']('is-error');
  };

  componentWillUnmount() {
    if (this.context.formDispatcher) {
      this.context.formDispatcher.removeListener('submit', this.onSubmit);
    }
    this.toggleBindToTarget(false);
  }

  @autobind
  onSubmit(reportValidity) {
    reportValidity(this.validate());
  }

  getTargetValue() {
    return this.props.getTargetValue(this.findTargetNode());
  }

  @autobind
  validate() {
    let errors = [];

    let validator = this.props.validator;
    const label = this.props.label;

    if (_.isString(validator)) {
      validator = validators[validator];
    }

    const value = this.getTargetValue();

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
      const className = classNames('fc-form-field-label', this.props.labelClassName);
      return (
        <label className={className} htmlFor={this.state.targetId}>
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
    return (
      <div className={ classNames('fc-form-field', this.props.className) }>
        {this.label}
        {this.props.children}
        {this.errorMessages}
      </div>
    );
  }
}
