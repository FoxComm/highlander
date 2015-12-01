import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import _ from 'lodash';

export default class PilledSearch extends React.Component {
  constructor(props, context) {
    super(props, context);

    this.state = {
      pills: [],
      searchValue: ''
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

  @autobind
  change({target}) {
    this.setState({
      ...this.state,
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
      case 13:
        // Enter
        event.preventDefault();
        if (!_.isEmpty(this.state.searchValue)) {
          this.setState({
            ...this.state,
            pills: this.state.pills.concat(this.state.searchValue),
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
                  value={this.state.searchValue}
                />
              </div>
            </div>
            {this.searchButton}
          </div>
        </form>
      </div>
    );
  }
}
