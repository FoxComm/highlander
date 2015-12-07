import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import _ from 'lodash';

import Menu from '../menu/menu';
import MenuItem from '../menu/menu-item';
import PilledInput from './pilled-input';

export default class PilledSearch extends React.Component {
  constructor(props, context) {
    super(props, context);

    this.state = {
      optionsVisible: false,
      searchDisplay: props.searchValue,
      searchOptions: props.searchOptions,
      searchValue: props.searchValue,
      selectionIndex: -1
    };
  }

  static propTypes = {
    className: PropTypes.string,
    onSubmit: PropTypes.func,
    placeholder: PropTypes.string,
    pillDelete: PropTypes.func,
    renderSearchOption: PropTypes.func,
    searchButton: PropTypes.node,
    searchOptions: PropTypes.array,
    searchValue: PropTypes.string
  };

  static defaultProps = {
    placeholder: '',
    searchOptions: [],
    searchValue: ''
  };

  get className() {
    return classNames('fc-pilled-search', this.props.className);
  }

  get searchOptions() {
    const options = _.transform(this.state.searchOptions, (result, option, idx) => {
      let renderSearchOption = this.renderSearchOption;
      if (this.props.renderSearchOption) {
        renderSearchOption = this.props.renderSearchOption;
      }

      result.push(renderSearchOption(
        option, 
        this.state.searchValue, 
        idx, 
        this.state.selectionIndex
      ));
    });

    return <Menu>{options}</Menu>;
  }

  @autobind
  renderSearchOption(option, searchTerm, idx, selectionIndex) {
    if (_.startsWith(option, searchTerm)) {
      const key = `search-option-${idx}`;
      const klass = classNames({
        'is-active': selectionIndex == idx,
        'is-first': idx == 0,
        'is-last': idx == selectionIndex - 1
      });

      return (
        <MenuItem className={klass} key={key} clickAction={() => this.props.onSubmit(option)}>
          {option}
        </MenuItem>
      );
    }
  }

  componentWillReceiveProps(nextProps) {
    const isVisible = nextProps.searchValue !== '' && nextProps.searchOptions.length > 0;

    this.setState({
      ...this.state,
      optionsVisible: isVisible,
      searchDisplay: nextProps.searchValue,
      searchOptions: nextProps.searchOptions,
      searchValue: nextProps.searchValue,
      selectionIndex: -1
    });
  }

  @autobind
  change({target}) {
    this.setState({
      ...this.state,
      searchDisplay: target.value,
      searchValue: target.value
    });
  }

  @autobind
  inputFocus() {
    if (!_.isEmpty(this.state.searchOptions)) {
      this.setState({
        ...this.state,
        optionsVisible: true
      });
    }
  }

  @autobind
  keyDown(event) {
    switch(event.keyCode) {
      case 40:
        // Down arrow
        event.preventDefault();
        if (!_.isEmpty(this.state.searchOptions)) {
          const newIdx = Math.min(
            this.state.selectionIndex + 1, 
            this.state.searchOptions.length - 1
          );

          this.setState({ 
            ...this.state, 
            optionsVisible: true,
            searchDisplay: this.state.searchOptions[newIdx].selectionValue,
            selectionIndex: newIdx
          });
        }
        break;
      case 38:
        // Up arrow
        event.preventDefault();
        if (!_.isEmpty(this.state.searchOptions)) {
          if (this.state.selectionIndex < 0) {
            this.setState({ ...this.state, optionsVisible: false });
          } else {
            const newIdx = this.state.selectionIndex - 1;
            const display = newIdx == -1
              ? this.state.searchValue
              : this.state.searchOptions[newIdx].selectionValue;

            this.setState({
              ...this.state,
              searchDisplay: display,
              selectionIndex: newIdx
            });
          }
        }
        break;
      case 13:
        // Enter
        event.preventDefault();
        this.props.onSubmit(this.state.searchDisplay);
        break;
      case 8:
        // Backspace
        if (_.isEmpty(this.state.searchValue) && !_.isEmpty(this.props.pills)) {
          this.props.deletePill(this.props.pills.length - 1);
        }
        break;
    }
  }

  render() {
    return (
      <div className={this.className}>
        <form>
          <PilledInput
            button={this.props.searchButton}
            onPillClose={(pill, idx) => this.props.deletePill(idx)}
            onPillClick={(pill, idx) => this.props.deletePill(idx)}
            placeholder={this.props.placeholder}
            onChange={this.change}
            onFocus={this.inputFocus}
            onKeyDown={this.keyDown}
            pills={this.props.pills}
            value={this.state.searchDisplay} />
        </form>
        <div>
          {this.state.optionsVisible && this.searchOptions}
        </div>
      </div>
    );
  }
}
