// @flow

// libs
import React, { Element, Component } from 'react';
import classNames from 'classnames';
import { debounce, autobind } from 'core-decorators';
import _ from 'lodash';

// components
import TypeaheadItems from './items';
import TypeaheadInput from './input';
import { FormField } from 'ui/forms';
import Alert from '@foxcommerce/wings/lib/ui/alerts/alert';
import LoadingInputWrapper from 'components/forms/loading-input-wrapper';

// styles
import s from './typeahead.css';

type State = {
  active: boolean,
  showMenu: boolean,
  showAlert: boolean,
  query: string,
};

type Props = {
  onBlur: () => void, // blur handler
  onChange: ?() => void, // input keyup/change handler
  onItemSelected: ?(item: Object) => void, // on item click/choose handler
  // fetchItems if passed should return promise for results
  fetchItems: () => void, // triggers when text is changed and text is valid
  hideOnBlur: boolean,
  isFetching: boolean,
  isAsync: boolean,
  items: Array<Element<*>>, // Array of data for suggestion. Each element passed to `component`
  label?: string, // title for input
  name?: string, // name attr for default input
  placeholder?: string, // placeholder attr for default input
  className?: string, // additional cl for root element of Typeahead
  inputClassName?: string, // class for typeahead input
  component: () => void, // component of one item, props={model: item}
  itemsElement?: Element<*>, // custom component for items as a list (not just for one item)
  inputElement?: Element<*>, // custom component for input field, default is `TypeaheadInput`
  minQueryLength: number, // if < then no fetching
  autoComplete?: string, // autoComplete attr for default input
  initialValue: string, // value attr for default input
  view?: string,
  onToggleVisibility?: () => void,
};

function mergeHandlers(...handlers) {
  return (...args) => {
    handlers.forEach(handler => handler(...args));
  };
}

function mergeEventHandlers(child, newEventHandlers) {
  return _.transform(newEventHandlers, (result, handler, type) => {
    result[type] = child.props[type] ? mergeHandlers(handler, child.props[type]) : handler; // eslint-disable-line
  });
}

type CloneProps = {
  props?: Object,
  handlers?: Object,
  defaultProps?: Object,
};

function cloneElement(element: Element<*>,
  { props, handlers, defaultProps }: CloneProps,
  children?: Array<Element<*>>) {
  const newProps = {
    ...defaultProps,
    ...element.props,
    ...props,
    ...mergeEventHandlers(element, handlers),
  };

  return React.cloneElement(element, newProps, children);
}

export default class Typeahead extends Component {

  props: Props;

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

  state: State = {
    active: false,
    showMenu: false,
    showAlert: false,
    query: this.props.initialValue,
  };

  _fetchRequest = null;

  componentWillReceiveProps(nextProps: Props) {
    if (this.props.isAsync) {
      if (this.props.isFetching && !nextProps.isFetching) {
        this.toggleVisibility(true);
      }
    } else {
      this.toggleVisibility(true);
    }

    if (nextProps.initialValue !== this.props.initialValue) {
      this.setState({ query: nextProps.initialValue });
    }
  }

  @autobind
  onItemSelected(item: Object) {
    let doHide = true;

    if (this.props.onItemSelected) {
      const event = {
        preventHiding() {
          doHide = false;
        },
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
  onBlur(event: SyntheticEvent) {
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
  inputKeyUp({ key }: { key: string }) {
    if (key === 'Escape') {
      this.toggleVisibility(false);
    }
  }

  @debounce(400)
  fetchItems(value: string) {
    if (value.length < this.props.minQueryLength) {
      return this.toggleAlert(true);
    }

    this._fetchRequest = this.props.fetchItems(value);
  }

  @autobind
  textChange({ target }: { target: { value: string }}) {
    const value = target.value;

    this.setState({
      query: value,
      showAlert: false,
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

  toggleVisibility(show: boolean) {
    this.setState({
      showMenu: show,
    }, () => {
      if (this.props.onToggleVisibility) {
        this.props.onToggleVisibility(show);
      }
    });
  }

  toggleAlert(show: boolean) {
    this.setState({
      showAlert: show,
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
    }
    return (
      <TypeaheadItems
        {...ourProps}
        component={this.props.component}
        items={this.props.items}
        onItemSelected={this.onItemSelected}
      />
    );
  }

  get inputContent() {
    const { isFetching, inputElement, inputClassName } = this.props;

    const defaultProps = {
      value: this.state.query,
      name: this.props.name,
      placeholder: this.props.placeholder,
      autoComplete: this.props.autoComplete,
      className: inputClassName || s.input,
      isFetching,
    };

    const handlers = {
      onBlur: this.onBlur,
      onFocus: this.onFocus,
      onChange: this.textChange,
      onKeyUp: this.inputKeyUp,
    };

    if (inputElement) {
      return (
        <LoadingInputWrapper inProgress={isFetching}>
          {cloneElement(inputElement, { defaultProps, handlers })}
        </LoadingInputWrapper>
      );
    }
    return <TypeaheadInput {...defaultProps} {...handlers} />;
  }

  render() {
    const className = classNames(s.block, { [s._active]: this.state.active }, this.props.className);

    const listClass = classNames(s.list, {
      [s._visible]: this.state.showMenu,
      [s._modal]: this.props.view == 'modal',
      [s._search]: this.props.view != 'no-search' && this.props.view != 'users',
      [s._users]: this.props.view == 'users',
    });

    return (
      <div className={className}>
        <FormField label={this.props.label}>
          {this.inputContent}
        </FormField>
        <div className={listClass}>
          {this.listContent}
        </div>
      </div>
    );
  }
}
