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
      pills: [],
      options: [{display: 'Order :'}, {display: 'Shipment :'}],
      optionsVisible: false,
      searchDisplay: '',
      searchValue: '',
      selectionIndex: -1
    };
  }

  static propTypes = {
    className: PropTypes.string,
    placeholder: PropTypes.string,
    pillFormater: PropTypes.func,
    searchButton: PropTypes.node,
    searchOptions: PropTypes.array
  };

  static defaultProps = {
    placeholder: '',
    searchOptions: []
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
        <MenuItem className={klass} key={key}>
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
  }

  @autobind
  deletePill(idx) {
    this.setState({
      ...this.state,
      pills: _.without(this.state.pills, this.state.pills[idx])
    });
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
        if (!_.isEmpty(this.state.searchValue)) {
          this.setState({
            ...this.state,
            pills: this.state.pills.concat(this.state.searchValue),
            searchDisplay: '',
            searchValue: ''
          });
        }
        break;
      case 8:
        // Backspace
        if (_.isEmpty(this.state.searchValue) && !_.isEmpty(this.state.pills)) {
          this.deletePill(this.state.pills.length - 1);
        }
        break;
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
                  onKeyDown={this.keyDown}
                  type="text"
                  value={this.state.searchDisplay}
                />
              </div>
            </div>
            {this.searchButton}
          </div>
        </form>
        {this.state.optionsVisible && this.searchOptions}
      </div>
    );
  }
}
