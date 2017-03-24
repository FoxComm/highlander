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

// styles
import s from './typeahead.css';

export default class Typeahead extends React.Component {

  static propTypes = {
    onBlur: PropTypes.func, // blur handler
    onChange: PropTypes.func, // input keyup/change handler
    onItemSelected: PropTypes.func, // on item click/choose handler
    // fetchItems if passed should return promise for results
    fetchItems: PropTypes.func, // triggers when text is changed and text is valid
    component: PropTypes.func, // component of one item, props={model: item}
    hideOnBlur: PropTypes.bool,
    isFetching: PropTypes.bool,
    items: PropTypes.array, // Array of data for suggestion. Each element passed to `component`
    label: PropTypes.string, // title for input
    name: PropTypes.string, // name attr for default input
    placeholder: PropTypes.string, // placeholder attr for default input
    className: PropTypes.string, // additional cl for root element of Typeahead
    itemsElement: PropTypes.element, // custom component for items as a list (not just for one item)
    inputElement: PropTypes.element, // custom component for input field, default is `TypeaheadInput`
    minQueryLength: PropTypes.number, // if < then no fetching
    autoComplete: PropTypes.string, // autoComplete attr for default input
    initialValue: PropTypes.string, // value attr for default input
    view: PropTypes.string,
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
  };

  state = {
    active: false,
    showMenu: false,
    showAlert: false,
    query: this.props.initialValue,
  };

  componentWillReceiveProps(nextProps) {
    if (this.props.isFetching && !nextProps.isFetching) {
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
      <Alert type={Alert.WARNING} className={s['need-more-characters']}>
        Please enter at least {this.props.minQueryLength} characters.
      </Alert>
    );
  }

  get listContent() {
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
      className: s.input,
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
    const className = classNames(s.block, { [s._active]: this.state.active }, this.props.className);

    const listClass = classNames(s.list, {
      [s._visible]: this.state.showMenu,
      [s._modal]: this.props.view == 'modal',
      [s._search]: this.props.view != 'no-search',
    });

    return (
      <div className={className}>
        <FormField label={this.props.label}>
          <LoadingInputWrapper inProgress={this.props.isFetching}>
            {this.inputContent}
          </LoadingInputWrapper>
        </FormField>
        <div className={listClass}>
          {this.listContent}
        </div>
      </div>
    );
  }
}
