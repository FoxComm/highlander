// libs
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { debounce, autobind } from 'core-decorators';
import { cloneElement } from '../../lib/react-utils';
import _ from 'lodash';

// components
import TypeaheadItems from './items';
import TypeaheadInput from './input';
import { FormField } from '../forms';
import Alert from '../alerts/alert';
import LoadingInputWrapper from '../forms/loading-input-wrapper';

export default class Typeahead extends React.Component {

  static propTypes = {
    onBlur: PropTypes.func,
    onChange: PropTypes.func,
    onItemSelected: PropTypes.func,
    // fetchItems if passed should return promise for results
    fetchItems: PropTypes.func,
    component: PropTypes.func,
    hideOnBlur: PropTypes.bool,
    isFetching: PropTypes.bool,
    isAsync: PropTypes.bool,
    items: PropTypes.array,
    label: PropTypes.string,
    name: PropTypes.string,
    placeholder: PropTypes.string,
    className: PropTypes.string,
    itemsElement: PropTypes.element,
    inputElement: PropTypes.element,
    minQueryLength: PropTypes.number,
    autoComplete: PropTypes.string,
    initialValue: PropTypes.string,
  };

  static defaultProps = {
    name: 'typeahead',
    fetchItems: _.noop,
    onBlur: _.noop,
    hideOnBlur: false,
    placeholder: 'Search',
    minQueryLength: 1,
    autoComplete: 'off',
    initialValue: '',
    isAsync: true,
  };

  state = {
    active: false,
    showMenu: false,
    showAlert: false,
    query: this.props.initialValue,
  };

  componentWillReceiveProps(nextProps) {
    if(this.props.isAsync){
      if (this.props.isFetching && !nextProps.isFetching) {
        this.toggleVisibility(true);
      }
    } else {
      this.toggleVisibility(true);
    }
  }

  @autobind
  onItemSelected(item) {
    let doHide = true;

    if (this.props.onItemSelected) {
      const event = {
        preventHiding() {
          doHide = false;
        }
      };

      this.props.onItemSelected(item, event);
    }

    if (doHide) this.clearState();
  }

  @autobind
  clearState() {
    this.setState({
      showMenu: false,
      query: this.props.initialValue,
    });
  }

  @autobind
  onBlur(event) {
    this.setState({ active: false });

    if (this.props.hideOnBlur) {
      this.toggleVisibility(false);
    }
    this.props.onBlur(event);
  }

  @autobind
  onFocus() {
    this.setState({ active: true });

    if (this.state.query.length >= this.props.minQueryLength) {
      this.toggleVisibility(true);
    }
  }

  @autobind
  inputKeyUp({ key }) {
    if (key === 'Escape') {
      this.toggleVisibility(false);
    }
  }

  @debounce(400)
  fetchItems(value) {
    if (value.length < this.props.minQueryLength) {
      return this.toggleAlert(true);
    }

    this._fetchRequest = this.props.fetchItems(value);
  }

  @autobind
  textChange({ target }) {
    let value = target.value;

    this.setState({
      query: value,
      showAlert: false
    });
    if (this.props.onChange) {
      this.props.onChange(value);
    }

    if (this._fetchRequest && this._fetchRequest.abort) {
      this._fetchRequest.abort();
    }

    if (value.length === 0) {
      return this.toggleVisibility(false);
    }

    this.fetchItems(value);

  }

  toggleVisibility(show) {
    this.setState({
      showMenu: show
    });
  }

  toggleAlert(show) {
    this.setState({
      showAlert: show
    });
  }

  renderAlert() {
    return (
      <div className="fc-typeahead__need-more-characters">
        <Alert type={Alert.WARNING}>
          Please enter at least {this.props.minQueryLength} characters.
        </Alert>
      </div>
    );
  }

  get menuContent() {
    if (this.state.showAlert) return this.renderAlert();

    const itemsElement = this.props.itemsElement;

    const ourProps = {
      updating: this.props.isFetching,
      toggleVisibility: show => this.toggleVisibility(show),
    };

    const clearState = {
      clearInputState: this.clearState,
    };

    if (itemsElement) {
      return React.cloneElement(itemsElement, { ...ourProps, ...clearState });
    } else {
      return (
        <TypeaheadItems {...ourProps}
          component={this.props.component}
          items={this.props.items}
          onItemSelected={this.onItemSelected} />
      );
    }
  }

  get inputContent() {
    const inputElement = this.props.inputElement;

    const defaultProps = {
      value: this.state.query,
      name: this.props.name,
      placeholder: this.props.placeholder,
      autoComplete: this.props.autoComplete,
    };

    const handlers = {
      onBlur: this.onBlur,
      onFocus: this.onFocus,
      onChange: this.textChange,
      onKeyUp: this.inputKeyUp,
    };

    if (inputElement) {
      return cloneElement(inputElement, { defaultProps, handlers });
    } else {
      return <TypeaheadInput {...defaultProps} {...handlers} />;
    }
  }

  render() {
    const elementClass = classNames('fc-typeahead', { '_active': this.state.active }, this.props.className);

    const menuClass = classNames('fc-typeahead__menu', {
      '_visible': this.state.showMenu
    });

    return (
      <div className={elementClass}>
        <FormField className="fc-typeahead__input-group" label={this.props.label}>
          <LoadingInputWrapper inProgress={this.props.isFetching}>
            {this.inputContent}
          </LoadingInputWrapper>
        </FormField>
        <div className={menuClass}>
          {this.menuContent}
        </div>
      </div>
    );
  }
}
