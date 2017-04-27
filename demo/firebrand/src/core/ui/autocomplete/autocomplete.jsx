
import _ from 'lodash';
import React, { Component } from 'react';
import styles from './autocomplete.css';
import { autobind } from 'core-decorators';
import scrollIntoView from 'dom-scroll-into-view';

import { TextInputWithLabel } from 'ui/inputs';

/* eslint react/sort-comp: 0 */

function matchStateToTerm(item, value) {
  return (
    this.getItemValue(item).toLowerCase().indexOf(value.toLowerCase()) !== -1
  );
}

type AutocompletePropTypes = {
  allowCustomValues: boolean;
  items: Array<any>,
  selectedItem: any;
  onChange?: Function;
  onSelect?: Function;
  shouldItemRender?: Function;
  renderItem?: Function;
  className?: string;
  inputProps: Object;
  compareValues?: (value1: string, value2: string) => boolean;
}

/* eslint-disable no-unused-vars */

const defaultProps = {
  items: [],
  inputProps: {},
  onChange() {},
  onSelect(item, value) {},
  compareValues(value1, value2) {
    return value1 == value2;
  },
  renderMenu(items, value) {
    return items;
  },
  sortItems(a, b) {
    return this.getItemValue(a).toLowerCase() > this.getItemValue(b).toLowerCase() ? 1 : -1;
  },
  shouldItemRender: matchStateToTerm,
  getItemValue: item => item.value,
  getItemKey(item) {
    return this.getItemValue(item);
  },
  renderItem(item, isHighlighted) {
    const value = this.getItemValue(item);
    const key = this.getItemKey(item);

    return (
      <div
        className={styles[isHighlighted ? 'item-highlighted' : 'item']}
        key={key}
      >{value}</div>
    );
  },
};

/* eslint-enable no-unused-vars */

class Autocomplete extends Component {

  props: AutocompletePropTypes;

  static defaultProps = defaultProps;

  constructor(props, ...args) {
    super(props, ...args);

    this.state = {
      value: props.selectedItem ? props.getItemValue(props.selectedItem) : '',
      isOpen: false,
      menuDirection: 'down',
      highlightedIndex: null,
    };
  }

  componentWillMount() {
    this._ignoreBlur = false;
    this._performAutoCompleteOnUpdate = false;
    this._performAutoCompleteOnKeyUp = false;
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.selectedItem != this.props.selectedItem) {
      this.setState({
        value: this.props.getItemValue(nextProps.selectedItem),
      });
    }
    this._performAutoCompleteOnUpdate = true;
  }

  componentDidUpdate(prevProps, prevState) {
    if (this.state.isOpen === true && prevState.isOpen === false) {
      this.setMenuOrientation();
    }

    if (this.state.isOpen && this._performAutoCompleteOnUpdate) {
      this._performAutoCompleteOnUpdate = false;
      this.maybeAutoCompleteText();
    }

    if (!this.state.isOpen && this.state.changingStarted) {
      this.finishFiltering();
    }

    this.maybeScrollItemIntoView();
  }

  finishFiltering() {
    this.setState({
      changingStarted: false,
    }, () => {
      this.maybeSelectItem();
    });
  }

  maybeSelectItem() {
    const props = this.props;

    const exactlyItem = _.find(props.items, item => {
      return props.compareValues(props.getItemValue(item), this.state.value);
    });

    if (exactlyItem) {
      props.onSelect(exactlyItem, this.state.value);
    } else {
      if (props.allowCustomValues) {
        props.onSelect(this.state.value);
      } else {
        const item = props.selectedItem || props.items[0];
        if (item) {
          this.setState({
            value: props.getItemValue(item),
          });
        }
      }
    }
  }

  maybeScrollItemIntoView() {
    if (this.state.isOpen && this.state.highlightedIndex !== null) {
      const itemNode = this.refs[`item-${this.state.highlightedIndex}`];
      if (itemNode) {
        const menuNode = this.refs.menu;

        scrollIntoView(itemNode, menuNode, { onlyScrollIfNeeded: true });
      }
    }
  }

  @autobind
  handleKeyDown(event) {
    if (this.keyDownHandlers[event.key]) {
      this.keyDownHandlers[event.key].call(this, event);
    } else {
      this.setState({
        highlightedIndex: null,
        isOpen: true,
      });
    }
  }

  @autobind
  handleChange(event) {
    event.stopPropagation();

    this._performAutoCompleteOnKeyUp = true;
    this.setState({
      value: event.target.value,
      changingStarted: true,
    }, () => {
      this.props.onChange(this.state.value);
    });
  }

  @autobind
  handleKeyUp() {
    if (this._performAutoCompleteOnKeyUp) {
      this._performAutoCompleteOnKeyUp = false;
      this.maybeAutoCompleteText();
    }
  }

  getInput() {
    return this.refs.container.querySelector('input');
  }

  get keyDownHandlers() {
    return {
      ArrowDown(event) {
        event.preventDefault();
        const { highlightedIndex } = this.state;
        const index = (
          highlightedIndex === null ||
          highlightedIndex === this.getFilteredItems().length - 1
        ) ? 0 : highlightedIndex + 1;
        this._performAutoCompleteOnKeyUp = true;
        this.setState({
          highlightedIndex: index,
          isOpen: true,
        });
      },
      ArrowUp(event) {
        event.preventDefault();
        const { highlightedIndex } = this.state;
        const index = (
          highlightedIndex === 0 ||
          highlightedIndex === null
        ) ? this.getFilteredItems().length - 1 : highlightedIndex - 1;
        this._performAutoCompleteOnKeyUp = true;
        this.setState({
          highlightedIndex: index,
          isOpen: true,
        });
      },
      Enter(event) {
        event.stopPropagation();
        event.preventDefault();
        if (this.state.isOpen === false) {
          // already selected this, do nothing
        } else if (this.state.highlightedIndex == null) {
          // hit enter after focus but before typing anything so no autocomplete attempt yet
          this.setState({
            isOpen: false,
          }, () => {
            this.getInput().select();
          });
        } else {
          const item = this.getFilteredItems()[this.state.highlightedIndex];

          this.setState({
            value: this.props.getItemValue(item),
            isOpen: false,
            highlightedIndex: null,
          }, () => {
            // this.getInput().focus() // TODO: file issue
            this.getInput().setSelectionRange(
              this.state.value.length,
              this.state.value.length
            );
            this.props.onSelect(item, this.state.value);
          });
        }
      },
      Escape() {
        this.setState({
          highlightedIndex: null,
          isOpen: false,
        });
      },
    };
  }

  getFilteredItems () {
    let items = this.props.items;

    if (this.props.shouldItemRender && this.state.changingStarted) {
      items = items.filter((item) => (
        this.props.shouldItemRender(item, this.state.value)
      ));
    }

    if (this.props.sortItems) {
      items.sort((a, b) => (
        this.props.sortItems(a, b, this.state.value)
      ));
    }

    return items;
  }

  maybeAutoCompleteText () {
    if (this.state.value === '') return;

    const { highlightedIndex } = this.state;
    const items = this.getFilteredItems();
    if (items.length === 0) return;

    const matchedItem = highlightedIndex !== null ?
      items[highlightedIndex] : items[0];
    const itemValue = this.props.getItemValue(matchedItem);
    const itemValueDoesMatch = (itemValue.toLowerCase().indexOf(
      this.state.value.toLowerCase()
    ) === 0);
    if (itemValueDoesMatch) {
      if (highlightedIndex === null) {
        this.setState({ highlightedIndex: 0 });
      }
    }
  }

  setMenuOrientation () {
    const menuNode = this.refs.menu;
    const inputNode = this.getInput();
    const viewportHeight = window.innerHeight;

    const inputPos = inputNode.getBoundingClientRect();
    const spaceAtTop = inputPos.top;
    const spaceAtBottom = viewportHeight - inputPos.bottom;

    let direction = 'down';

    if (!menuNode) {
      if (spaceAtBottom < viewportHeight / 2) direction = 'up';
    } else {
      const menuRect = menuNode.getBoundingClientRect();
      if (spaceAtBottom < menuRect.height && spaceAtBottom < spaceAtTop) {
        direction = 'up';
      }
    }

    this.setState({
      menuDirection: direction,
    });
  }

  highlightItemFromMouse (index) {
    this.setState({ highlightedIndex: index });
  }

  selectItemFromMouse (item) {
    this.setState({
      value: this.props.getItemValue(item),
      isOpen: false,
      highlightedIndex: null,
    }, () => {
      this.props.onSelect(item, this.state.value);
      this.getInput().focus();
      this.setIgnoreBlur(false);
    });
  }

  setIgnoreBlur (ignore) {
    this._ignoreBlur = ignore;
  }

  get menu() {
    const items = this.getFilteredItems().map((item, index) => {
      const element = this.props.renderItem(
        item,
        this.state.highlightedIndex === index,
        {cursor: 'default'}
      );
      return React.cloneElement(element, {
        onMouseDown: () => this.setIgnoreBlur(true),
        onMouseEnter: () => this.highlightItemFromMouse(index),
        onClick: () => this.selectItemFromMouse(item),
        ref: `item-${index}`,
      });
    });
    const menu = this.props.renderMenu(items, this.state.value);

    return <div styleName={`menu-drop${this.state.menuDirection}`} ref="menu">{menu}</div>;
  }

  @autobind
  handleInputBlur () {
    if (this._ignoreBlur) return;

    this.setState({
      isOpen: false,
      highlightedIndex: null,
    });
  }

  @autobind
  handleInputFocus () {
    if (this._ignoreBlur) return;

    this.setState({ isOpen: true });
  }

  @autobind
  handleInputClick () {
    if (this.state.isOpen === false) {
      this.setState({ isOpen: true });
    }
  }

  render () {
    const { inputProps, ...rest } = this.props;
    const restProps = _.omit(rest, Object.keys(defaultProps));

    return (
      <div ref="container" styleName="autocomplete" {...restProps}>
        <TextInputWithLabel
          label={this.state.isOpen ? '▲' : '▼'}
          type="text"
          {...inputProps}
          styleName="input"
          role="combobox"
          aria-autocomplete="both"
          onFocus={this.handleInputFocus}
          onBlur={this.handleInputBlur}
          onChange={this.handleChange}
          onKeyDown={this.handleKeyDown}
          onKeyUp={this.handleKeyUp}
          onClick={this.handleInputClick}
          value={this.state.value}
        />
        {this.state.isOpen && this.menu}
      </div>
    );
  }
}

export default Autocomplete;
