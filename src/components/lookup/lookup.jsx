// libs
import _ from 'lodash';
import React, { Component, PropTypes } from 'react';
import ReactDOM from 'react-dom';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

// helpers
import { prefix } from '../../lib/text-utils';
import { isElementVisible } from '../../lib/dom-utils';

// components
import LookupInput from './lookup-input';
import LookupItem from './lookup-item';
import LookupItems from './lookup-items';


const prefixed = prefix('fc-lookup');

/**
 * Simplistic lookup component, that is to be extended if needed
 *
 * Used for looking up in given
 */
export default class Lookup extends Component {

  static propTypes = {
    className: PropTypes.string,
    inputClassName: PropTypes.string,
    data: PropTypes.arrayOf(PropTypes.shape({
      id: PropTypes.any,
      label: PropTypes.string,
    })),
    value: PropTypes.any,
    minQueryLength: PropTypes.number,
    inputComponent: PropTypes.func,
    itemComponent: PropTypes.func,
    onSelect: PropTypes.func.isRequired,
    onToggleMenu: PropTypes.func,
    showMenu: PropTypes.bool,
    notFound: PropTypes.string,
  };

  static defaultProps = {
    data: [],
    minQueryLength: 1,
    inputComponent: LookupInput,
    itemComponent: LookupItem,
    onToggleMenu: _.noop,
    showMenu: false,
    notFound: 'No results found.',
  };

  constructor(props, context) {
    super(props, context);

    this.state = {
      query: this.getQuery(props.data, props.value),
      showMenu: false,
      activeIndex: -1,
    };
  }

  componentWillReceiveProps({data, value, showMenu}) {
    const query = this.getQuery(data, value);

    if (query && query !== this.state.query) {
      this.setQuery(data, query);
    }

    if (showMenu != this.state.showMenu) {
      this.showMenu(showMenu);
    }
  }

  getQuery(data, value) {
    const item = _.find(data, ({id}) => id === value);

    return item ? item.label : '';
  }

  @autobind
  setQuery(data, query) {
    const items = this.filter(data, query);
    const index = items.indexOf(_.find(items, ({label}) => label === query));

    this.setState({
      query,
      activeIndex: index >= 0 ? index : this.state.activeIndex,
    });
  }

  showMenu(show) {
    const {activeIndex, showMenu} = this.state;
    if (showMenu === show) {
      return;
    }

    //if menu is hidden, activeIndex is reset
    this.setState({
      showMenu: show,
      activeIndex: show ? activeIndex : -1
    });
    this.props.onToggleMenu(show);
  }

  setActive(index) {
    if (this.indexIsValid(index)) {
      this.scrollTo(index, index - this.state.activeIndex > 0 ? 'bottom' : 'top');
      this.setState({activeIndex: index});
    }
  }

  scrollTo(index, direction) {
    const component = ReactDOM.findDOMNode(this);

    const menu = component.querySelector('.fc-lookup__menu');
    const menuRect = menu.getBoundingClientRect();

    const items = component.querySelector('.fc-lookup__items');

    const item = component.querySelector(`.fc-lookup__item:nth-child(${index + 1})`);
    const itemRect = item.getBoundingClientRect();

    if (isElementVisible(items, menu) && !isElementVisible(item, items)) {
      items.scrollTop += itemRect[direction] - menuRect[direction];
    }
  }

  @autobind
  select(index) {
    if (this.indexIsValid(index)) {
      this.props.onSelect(this.items[index]);
      this.showMenu(false);
    }
  }

  @autobind
  onFocus() {
    if (this.isShowable) {
      this.showMenu(true);
    }
  }

  @autobind
  onBlur() {
    this.showMenu(false);
  }

  @autobind
  onInputKeyDown(event) {
    const {key} = event;
    if (key === 'Escape') {
      this.showMenu(false);
    }

    const {activeIndex, showMenu} = this.state;
    if (showMenu) {
      if (key === 'ArrowUp') {
        this.setActive(activeIndex - 1);
      }
      if (key === 'ArrowDown') {
        this.setActive(activeIndex + 1);
      }
      if (key === 'Enter') {
        event.preventDefault();
        this.select(activeIndex);
      }
    } else {
      if (key === 'ArrowDown') {
        this.showMenu(true);
        this.setActive(activeIndex + 1);
      }
    }
  }

  indexIsValid(index) {
    return 0 <= index && index <= this.items.length - 1;
  }

  get isShowable() {
    return this.items.length && this.state.query.length >= this.props.minQueryLength;
  }

  filter(data, query) {
    return data.filter(({label}) => {
      return label.toLowerCase().includes(query.toLowerCase());
    });
  }

  get items() {
    const {data} = this.props;
    const {query} = this.state;

    return query ? this.filter(data, query) : [];
  }

  get input() {
    const {props} = this;

    return React.createElement(props.inputComponent, {
      value: this.state.query,
      className: props.inputClassName,
      onBlur: this.onBlur,
      onFocus: this.onFocus,
      onChange: value=> {
        this.setQuery(props.data, value);
        this.showMenu(true);
      },
      onKeyDown: this.onInputKeyDown,
    });
  }

  get menu() {
    const {itemComponent, notFound} = this.props;
    const {query, showMenu, activeIndex} = this.state;

    const menuClass = classNames(prefixed('menu'), {
      '_visible': showMenu,
    });

    return (
      <div className={menuClass}>
        <LookupItems component={itemComponent}
                     query={query}
                     items={this.items}
                     activeIndex={activeIndex}
                     onSelect={this.select}
                     notFound={notFound}/>
      </div>
    );
  }

  render() {
    const {className} = this.props;

    return (
      <div className={classNames('fc-lookup', className)}>
        {this.input}
        {this.isShowable ? this.menu : null}
      </div>
    );
  }
};
