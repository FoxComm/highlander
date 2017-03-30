/* @flow weak */

import _ from 'lodash';
import React, { Component } from 'react';
import styles from './autocomplete.css';
import { autobind } from 'core-decorators';
import scrollIntoView from 'dom-scroll-into-view';

import { TextInput } from 'ui/text-input';

/* eslint react/sort-comp: 0 */

function matchStateToTerm(item, value) {
  return (
    this.getItemValue(item).toLowerCase().indexOf(value.toLowerCase()) !== -1
  );
}

type Props = {
  allowCustomValues?: boolean,
  items: Array<any>,
  inputProps: Object,
  selectedItem: any,
  onChange: Function,
  onSelect: Function,
  shouldItemRender: Function,
  renderMenu: Function,
  renderItem: Function,
  className?: string,
  compareValues: Function,
  getItemValue: Function,
  sortItems: boolean,
};

/* eslint-disable no-unused-vars */

type State = {
  value: string|number,
  isOpen: boolean,
  menuDirection: string,
  highlightedIndex: null|number,
  changingStarted?: boolean,
};

/* eslint-enable no-unused-vars */

class Autocomplete extends Component {

  props: Props;

  state: State = {
    value: this.props.selectedItem ? this.props.getItemValue(this.props.selectedItem) : '',
    isOpen: false,
    menuDirection: 'down',
    highlightedIndex: null,
  };

  static defaultProps = {
    allowCustomValues: false,
    className: '',
    sortItems: true,
    onChange() {},
    compareValues(value1, value2) {
      return value1 == value2;
    },
    renderMenu(items) {
      return items;
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
          styleName={isHighlighted ? 'item-highlighted' : 'item'}
          key={key}
        >{value}</div>
      );
    },
  };

  _ignoreBlur = false;
  _performAutoCompleteOnUpdate = false;
  _performAutoCompleteOnKeyUp = false;

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

  @autobind
  sortItems(a, b) {
    return this.props.getItemValue(a).toLowerCase() > this.props.getItemValue(b).toLowerCase() ? 1 : -1;
  }

  maybeSelectItem() {
    const props = this.props;

    const exactlyItem = _.find(props.items, (item) => {
      return props.compareValues(props.getItemValue(item), this.state.value);
    });

    if (exactlyItem) {
      props.onSelect(exactlyItem, this.state.value);
    } else if (props.allowCustomValues) {
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
            const input = this.getInput();
            if (input.setSelectionRange) {
              try {
                input.setSelectionRange(
                  this.state.value.length,
                  this.state.value.length
                );
              } catch (ex) {
                // ignore
              }
            }

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
      items = items.filter(item => (
        this.props.shouldItemRender(item, this.state.value)
      ));
    }

    if (this.props.sortItems) {
      items.sort((a, b) => (
        this.sortItems(a, b, this.state.value)
      ));
    }

    return items;
  }

  itemValueMatches(value) {
    return (value.toString().toLowerCase().indexOf(
      this.state.value.toString().toLowerCase()
    ) === 0);
  }

  maybeAutoCompleteText () {
    if (this.state.value === '') return;

    const { highlightedIndex } = this.state;
    const items = this.getFilteredItems();
    if (items.length === 0) return;

    const matchedItem = highlightedIndex !== null ?
      items[highlightedIndex] : items[0];
    const itemValue = this.props.getItemValue(matchedItem);

    if (this.itemValueMatches(itemValue)) {
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
    const { inputProps } = this.props;
    
    return (
      <div ref="container" styleName="autocomplete" >
        <TextInput
          hasSymbol
          type="text"
          {...inputProps}
          role="combobox"
          aria-autocomplete="both"
          aria-expanded={this.state.isOpen ? 'true' : 'false'}
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
