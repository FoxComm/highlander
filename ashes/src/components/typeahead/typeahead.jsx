// libs
import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import { debounce, autobind } from 'core-decorators';
import { cloneElement } from '../../lib/react-utils';
import _ from 'lodash';

// components
import TypeaheadItems from './items';
import TypeaheadInput from './input';
import { FormField } from 'components/forms';
import LoadingInputWrapper from 'components/forms/loading-input-wrapper';

// styles
import s from './typeahead.css';

export default class Typeahead extends React.Component {

  static propTypes = {
    onBlur: PropTypes.func, // blur handler
    onChange: PropTypes.func, // input keyup/change handler
    onItemSelected: PropTypes.func, // on item click/choose handler
    // fetchItems if passed should return promise for results
    fetchItems: PropTypes.func, // triggers when text is changed and text is valid
    hideOnBlur: PropTypes.bool,
    isFetching: PropTypes.bool,
    isAsync: PropTypes.bool,
    items: PropTypes.array.isRequired, // Array of data for suggestion. Each element passed to `component`
    label: PropTypes.string, // title for input
    name: PropTypes.string, // name attr for default input
    placeholder: PropTypes.string, // placeholder attr for default input
    className: PropTypes.string, // additional cl for root element of Typeahead
    component: PropTypes.func, // component of one item, props={model: item}
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
    items: [],
    onBlur: _.noop,
    hideOnBlur: false,
    placeholder: 'Start typing to search...',
    minQueryLength: 1,
    autoComplete: 'off',
    initialValue: '',
    isAsync: true,
  };

  state = {
    active: false,
    showMenu: false,
    query: this.props.initialValue,
    searchedOnce: false,
  };

  componentWillReceiveProps(nextProps) {
    if (nextProps.initialValue !== this.props.initialValue) {
      this.setState({ query: nextProps.initialValue });
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

    if (this.props.hideOnBlur || !this.queryIsValid()) {
      this.toggleVisibility(false);
    }
    this.props.onBlur(event);
  }

  @autobind
  onFocus() {
    const { items } = this.props;
    const { searchedOnce } = this.state;

    this.setState({ active: true });
    this.toggleVisibility(true);

    if (!searchedOnce && !items.length) {
      this.fetchItemsImmediately();
    }
  }

  @autobind
  inputKeyUp({ key }) {
    if (key === 'Escape') {
      this.toggleVisibility(false);
    }
  }

  @debounce(400)
  fetchItems(q: ?string) {
    this.fetchItemsImmediately(q);
  }

  fetchItemsImmediately(q: ?string) {
    const query = _.isString(q) ? q : this.state.query;

    if (this.queryIsValid(query)) {
      this._fetch = {
        query,
        request: this.props.fetchItems(query),
      };
      this.setState({ searchedOnce: true });
    }
  }

  queryIsValid(q) {
    const query = _.isString(q) ? q : this.state.query;

    return query.length >= this.props.minQueryLength;
  }

  @autobind
  textChange(value: string) {

    this.setState({
      query: value,
    });
    if (this.props.onChange) {
      this.props.onChange(value);
    }

    _.invoke(this, '_fetch.request.abort');

    if (!this.state.showMenu) {
      this.toggleVisibility(true);
    }

    this.fetchItems(value);
  }

  toggleVisibility(show) {
    this.setState({
      showMenu: show
    });
  }

  // Because we have debounce on `fetchItems`, we have to check if fQuery matches query
  get updating() {
    const { isFetching } = this.props;
    const { query } = this.state;
    const fQuery = this._fetch && this._fetch.query;

    return isFetching || (this.queryIsValid() && fQuery !== query);
  }

  get listContent() {
    const { isFetching, itemsElement, minQueryLength } = this.props;
    // const { searchedOnce } = this.state;
    // const noResults = !items.length && searchedOnce && !isFetching;

    const ourProps = {
      updating: this.updating,
      toggleVisibility: show => this.toggleVisibility(show),
      clearInputState: this.clearState,
    };

    if (!this.queryIsValid() && !isFetching) {
      let chars = minQueryLength === 1 ? 'character' : 'characters';

      // @todo do we need custom `please` component?
      return <div className={`${s.items} ${s.please}`}>{`Please enter at least ${minQueryLength} ${chars}.`}</div>;
    }

    if (itemsElement) {
      return React.cloneElement(itemsElement, ourProps);
    } else {
      return (
        <TypeaheadItems
          {...ourProps}
          component={this.props.component}
          items={this.props.items}
          onItemSelected={this.onItemSelected}
          query={this.state.query}
        />
      );
    }
  }

  get inputContent() {
    const { isFetching, inputElement } = this.props;

    const defaultProps = {
      value: this.state.query,
      name: this.props.name,
      placeholder: this.props.placeholder,
      autoComplete: this.props.autoComplete,
      className: s.input,
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
    } else {
      return <TypeaheadInput {...defaultProps} {...handlers} />;
    }
  }

  render() {
    const className = classNames(s.block, { [s.active]: this.state.active }, this.props.className);

    const listClass = classNames(s.list, {
      [s.visible]: this.state.showMenu,
      [s.modal]: this.props.view == 'modal',
      [s.search]: this.props.view != 'no-search' && this.props.view != 'users',
      [s.users]: this.props.view == 'users',
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
