/**
 * @flow weak
 */

// libs
import React, { Component, PropTypes } from 'react';
import classNames from 'classnames';
import s from './suggester.css';

type State = {
  value: string,
};

export class Suggester extends Component {
  state: State = {
    value: '',
  };

  static defaultProps = {
    data: {},
    primaryTitle: 'Proposed:',
    othersTitle: 'Other:',
    onChange: () => {},
    onPick: () => {},
    className: '',
    minLength: 3,
  };

  static propTypes = {
    proposed: PropTypes.array,
    data: PropTypes.shape({
      primary: PropTypes.array,
      secondary: PropTypes.array,
    }).isRequired,
    primaryTitle: PropTypes.string,
    othersTitle: PropTypes.string,
    onChange: PropTypes.func,
    onPick: PropTypes.func,
    className: PropTypes.string,
  };

  componentDidUpdate() {
    if (this._input) {
      this._input.scrollLeft = this._input.scrollWidth;
    }
  }

  render() {
    const {
      primaryTitle,
      othersTitle,
      className,
      minLength,
      data: { primary, secondary },
      inProgress,
    } = this.props;
    const { value } = this.state;

    const primaryHtml = !!primary && primary.map(line => (
      <div className={classNames(s.item, s._proposed)} key={line.id} onClick={() => this._onPick(line)}>

        <div className={s.itemPrefix}>{line.prefix}</div>
        <div className={s.itemValue}>{line.text}</div>
      </div>
    ));
    const secondaryHtml = !!secondary && secondary.map(line => (
      <div className={s.item} key={line.id} onClick={() => this._onPick(line)}>
        <div className={s.itemPrefix}>{line.prefix}</div>
        <div className={s.itemValue}>{line.text}</div>
      </div>
    ));
    const noResults = !primaryHtml && !secondaryHtml && !inProgress;
    const noResText = value.length >= minLength ? 'No category found' : `Type at least ${minLength} letters`;

    return (
      <div className={classNames(s.root, className)}>
        <input
          onChange={(e) => this._onType(e)}
          ref={d => this._input = d}
          value={value}
          type="text"
          className={classNames(s.input, 'fc-input')} />

        <div className={s.dropdown}>
          {!!primary && [
            <div className={s.listHeader} key="primaryTitle">{primaryTitle}</div>,
            <div key="primaryHtml">{primaryHtml}</div>,
            <div className={s.listHeader} key="othersTitle">{othersTitle}</div>,
          ]}

          <div className={s.others}>
            {secondaryHtml}
          </div>

          {noResults && (
            <div className={classNames(s.listHeader, s._no)}>{noResText}</div>
          )}

          {inProgress && (
            <div className={classNames(s.listHeader, s._no)}>Loading..</div>
          )}
        </div>
      </div>
    );
  }

  _input: any;

  _onType({ target: { value } }) {
    const { minLength } = this.props;

    this.setState({ value });

    if (value.length >= minLength) {
      this.props.onChange(value);
    }
  }

  _onPick(item) {
    const { onPick } = this.props;

    this.setState({ value: `${item.prefix} » ${item.text}` });
    this.props.onPick(item.id);
  }
}
