
import _ from 'lodash';
import React, { PropTypes } from 'react';
import TransitionGroup from 'react-addons-css-transition-group';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

class TotalCounter extends React.Component {
  static propTypes = {
    isFetching: PropTypes.oneOf([null, true, false]),
    entitiesCount: PropTypes.number.isRequired,
    fetch: PropTypes.func.isRequired,
  };

  static defaultProps = {
    isFetching: null,
  };

  constructor(props, ...args) {
    super(props, ...args);

    this.state = {
      showCounterForFetching: false,
    };
  }

  componentDidMount() {
    this.fetchTimer = setTimeout(this.fetchData, 100);
  }

  componentWillUnmount() {
    clearTimeout(this.fetchTimer);
  }

  @autobind
  fetchData() {
    const { isFetching, fetch } = this.props;
    if (isFetching === null) {
      fetch();
    }
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.isFetching === true) {
      this.setState({
        showCounterForFetching: this.props.isFetching === false,
      });
    }
  }

  render() {
    const { entitiesCount, isFetching } = this.props;
    let count = '';
    if (isFetching === false || (isFetching === true && this.state.showCounterForFetching)) {
      count = entitiesCount;
    }

    return (
      <TransitionGroup transitionName="fc-transition-counter"
                       transitionAppear={true}
                       transitionLeave={false}
                       transitionAppearTimeout={300}
                       transitionEnterTimeout={300}>
        <span id="total-counter-value" key={count}>{count}</span>
      </TransitionGroup>
    );
  }
}

function makePropsMapper(getLSState) {
  return state => {
    const {selectedSearch, savedSearches} = getLSState(state);

    return {
      entitiesCount: _.get(savedSearches, [selectedSearch, 'results', 'total']),
      isFetching: _.get(savedSearches, [selectedSearch, 'results', 'isFetching']),
    };
  };
}

export default function makeTotalCounter(getLSState, actions) {
  return connect(makePropsMapper(getLSState), actions)(TotalCounter);
}
