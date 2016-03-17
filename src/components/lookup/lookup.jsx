// libs
import _ from 'lodash';
import React, { Component, PropTypes } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

// helpers
import { prefix } from '../../lib/text-utils';

// components
import LookupInput from './lookup-input';
import LookupItem from './lookup-item';
import LookupItems from './lookup-items';


const prefixed = prefix('fc-lookup__');

/**
 * Simplistic lookup component, that is to be extended if needed
 *
 * Used for looking up in given
 */
export default class Lookup extends Component {

  static propTypes = {
    data: PropTypes.arrayOf(PropTypes.shape({
      id: PropTypes.any,
      label: PropTypes.string,
    })),
    value: PropTypes.any,
    showMenu: PropTypes.bool,
    inputComponent: PropTypes.func,
    itemComponent: PropTypes.func,
    minQueryLength: PropTypes.number,
    onSelect: PropTypes.func.isRequired,
    className: PropTypes.string,
    notFound: PropTypes.string,
  };

  static defaultProps = {
    data: [],
    showMenu: false,
    inputComponent: LookupInput,
    itemComponent: LookupItem,
    minQueryLength: 1,
    notFound: 'No results found.',
  };

  constructor(props, context) {
    super(props, context);

    this.state = {
      query: this.getQuery(props.data, props.value),
      showMenu: false,
    };
  }

  componentWillReceiveProps({data, value, showMenu}) {
    const query = this.getQuery(data, value);

    if (query != this.state.query) {
      this.setQuery(query);
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
  setQuery(query) {
    this.setState({
      query,
    });
  }

  showMenu(showMenu) {
    this.setState({showMenu});
  }

  @autobind
  onBlur(event) {
    this.showMenu(false);
  }

  @autobind
  onFocus() {
    if (this.state.query.length >= this.props.minQueryLength) {
      this.showMenu(true);
    }
  }

  @autobind
  onInputKeyUp({ key }) {
    if (key === 'Escape') {
      this.showMenu(false);
    }
    if (key === 'ArrowDown') {
      this.showMenu(true);
    }
  }

  get items() {
    const {query} = this.state;
    const {data} = this.props;

    if (!query) {
      return data;
    }

    return data.filter(({label}) => {
      return label.toLowerCase().includes(query.toLowerCase());
    });
  }

  get input() {
    const {props} = this;

    return React.createElement(props.inputComponent, {
      value: this.state.query,
      onBlur: this.onBlur,
      onFocus: this.onFocus,
      onChange: value=> {
        this.setQuery(value);
        this.showMenu(true);
      },
      onKeyUp: this.onInputKeyUp,
    });
  }

  get menu() {
    const {minQueryLength, itemComponent, onSelect, notFound} = this.props;
    const {query} = this.state;

    if (query.length < minQueryLength) {
      return null;
    }

    const items = this.items;
    const menuClass = classNames(prefixed('menu'), {
      '_visible': items.length && this.state.showMenu,
    });
    const handleSelect = item => {
      onSelect(item);
      this.showMenu(false);
    };

    return (
      <div className={menuClass}>
        <LookupItems component={itemComponent}
                     query={query}
                     items={items}
                     onSelect={handleSelect}
                     notFound={notFound} />
      </div>
    );
  }

  render() {
    const {className} = this.props;

    return (
      <div className={classNames('fc-lookup', className)}>
        {this.input}
        {this.menu}
      </div>
    );
  }
};

export default Lookup;
