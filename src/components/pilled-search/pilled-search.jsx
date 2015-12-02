import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import _ from 'lodash';

import Menu from '../menu/menu';
import MenuItem from '../menu/menu-item';

export default class PilledSearch extends React.Component {
  constructor(props, context) {
    super(props, context);

    this.state = {
      pills: props.pills,
      options: props.searchOptions,
      optionsVisible: false,
      searchDisplay: props.searchValue,
      searchValue: props.searchValue,
      selectionIndex: -1
    };
  }

  static propTypes = {
    className: PropTypes.string,
    onChange: PropTypes.func,
    onDeletePill: PropTypes.func,
    onSubmit: PropTypes.func,
    placeholder: PropTypes.string,
    pills: PropTypes.array,
    pillFormatter: PropTypes.func,
    searchButton: PropTypes.node,
    searchOptions: PropTypes.array,
    searchValue: PropTypes.string
  };

  static defaultProps = {
    onChange: null,
    onDeletePill: null,
    onSubmit: null,
    pills: [],
    placeholder: '',
    searchOptions: [],
    searchValue: ''
  };

  get className() {
    return classNames('fc-pilled-search', this.props.className);
  }

  get pills() {
    if (this.props.pillFormatter) {
      return this.state.pills.map(this.props.pillFormatter);
    } else {
      return this.state.pills.map(this.formatPill);
    }
  }

  get searchButton() {
    if (this.props.searchButton) {
      return (
        <div className="fc-pilled-search__btn-container">
          {this.props.searchButton}
        </div>
      );
    } else {
      return null;
    }
  }

  get searchOptions() {
    const options = this.state.options.map((option, idx) => {
      const key = `option-${idx}`;
      const klass = classNames({
        'is-active': this.state.selectionIndex == idx,
        'is-first': idx == 0,
        'is-last': idx == this.state.selectionIndex - 1
      });

      return (
        <MenuItem className={klass} key={key} onClick={() => this.selectOption(idx)}>
          {option.display}
        </MenuItem>
      );
    });

    return <Menu>{options}</Menu>;
  }

  @autobind
  change({target}) {
    this.setState({
      ...this.state,
      searchDisplay: target.value,
      searchValue: target.value
    });

    if (this.props.onChange) {
      this.props.onChange(target.value);
    }
  }

  @autobind
  deletePill(idx) {
    if (this.props.onDeletePill) {
      this.props.onDeletePill(idx);
    } else {
      this.setState({
        ...this.state,
        pills: _.without(this.state.pills, this.state.pills[idx])
      });
    }
  }

  @autobind
  formatPill(pill, idx) {
    return (
      <div
        className="fc-pilled-search__pill"
        key={`pill-${idx}`}
        onClick={() => this.deletePill(idx)}
        >
        {pill}
        <a className="fc-pilled-search__pill-close">
          &times;
        </a>
      </div>
    );
  }

  @autobind
  inputFocus() {
    if (!_.isEmpty(this.state.options)) {
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
        if (!_.isEmpty(this.state.options)) {
          const newIdx = Math.min(
            this.state.selectionIndex + 1, 
            this.state.options.length - 1
          );

          this.setState({ 
            ...this.state, 
            optionsVisible: true,
            searchDisplay: this.state.options[newIdx].display,
            selectionIndex: newIdx
          });
        }
        break;
      case 38:
        // Up arrow
        event.preventDefault();
        if (!_.isEmpty(this.state.options)) {
          if (this.state.selectionIndex < 0) {
            this.setState({ ...this.state, optionsVisible: false });
          } else {
            const newIdx = this.state.selectionIndex - 1;
            const display = newIdx == -1
              ? this.state.searchValue
              : this.state.options[newIdx].display;

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
        this.submitSearch(this.state.searchDisplay);
        break;
      case 8:
        // Backspace
        if (_.isEmpty(this.state.searchValue) && !_.isEmpty(this.state.pills)) {
          this.deletePill(this.state.pills.length - 1);
        }
        break;
    }
  }

  @autobind
  selectOption(idx) {
    if (idx > -1 && idx < this.state.options.length) {
      this.submitSearch(this.state.options[idx].display);
    }
  }

  submitSearch(text) {
    if (this.props.onSubmit) {
      this.props.onSubmit(text);
    } else if (!_.isEmpty(text)) {
      this.setState({
        ...this.state,
        optionsVisible: false,
        pills: this.state.pills.concat(text),
        searchDisplay: '',
        searchValue: '',
        selectionIndex: -1
      });
    }
  }

  render() {
    return (
      <div className={this.className}>
        <form>
          <div className="fc-pilled-search__container">
            <div className="fc-pilled-search__input-container">
              <div className="fc-pilled-search__pills-wrapper">
                {this.pills}
              </div>
              <div className="fc-pilled-search__icon-wrapper">
                <i className="icon-search"></i>
              </div>
              <div className="fc-pilled-search__input-wrapper">
                <input
                  className="fc-pilled-search__input-field"
                  placeholder={this.props.placeholder}
                  onChange={this.change}
                  onFocus={this.inputFocus}
                  onKeyDown={this.keyDown}
                  type="text"
                  value={this.state.searchDisplay}
                />
              </div>
            </div>
            {this.searchButton}
          </div>
        </form>
        <div>
          {this.state.optionsVisible && this.searchOptions}
        </div>
      </div>
    );
  }
}
